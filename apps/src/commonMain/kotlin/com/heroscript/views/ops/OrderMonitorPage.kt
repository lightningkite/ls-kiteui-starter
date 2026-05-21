package com.heroscript.views.ops

import com.heroscript.*
import com.heroscript.extensions.DerivedStatus
import com.heroscript.extensions.derivedStatus
import com.heroscript.sdk.currentSession
import com.heroscript.views.orders.OrderAlertReason
import com.heroscript.views.orders.OrderDetailPage
import com.heroscript.views.orders.OrderStatusFilter
import com.heroscript.views.orders.orderAlertReason
import com.heroscript.views.orders.orderRowContent
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalKiteUi::class)
@Routable("ops/orders")
class OrderMonitorPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Order Monitor")
    override var parentPage: Page? = null

    @QueryParameter
    val statusFilter = Signal(OrderStatusFilter.All)

    @QueryParameter
    val alertFilter = Signal(AlertFilter.All)

    @QueryParameter
    val fromDate = Signal<kotlinx.datetime.LocalDate?>(null)

    @QueryParameter
    val toDate = Signal<kotlinx.datetime.LocalDate?>(null)

    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val clinicFilter = Signal<Clinic.ID?>(null)

    @QueryParameter
    val pharmacyFilter = Signal<Pharmacy.ID?>(null)

    private val isOps = remember {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val clinics = remember {
        val session = currentSession() ?: return@remember emptyList()
        if (!isOps()) return@remember emptyList()
        session.clinics.query(Query<Clinic>())()
    }

    private val clinicNameById = remember { clinics().associate { it._id to it.name } }

    private val clinicOptions = remember { listOf<Clinic.ID?>(null) + clinics().map { it._id } }

    private val pharmacies = remember {
        val session = currentSession() ?: return@remember emptyList()
        if (!isOps()) return@remember emptyList()
        session.pharmacies.query(Query<Pharmacy>())()
    }

    private val pharmacyNameById = remember { pharmacies().associate { it._id to it.name } }

    private val pharmacyOptions = remember { listOf<Pharmacy.ID?>(null) + pharmacies().map { it._id } }

    /* ---- KPI tile data (server-side counts) ---- */

    private val activeOrdersCount = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending 0
        if (!isOps()) return@rememberSuspending 0
        session.prescriptionOrders.skipCache.count(
            condition<PrescriptionOrder> {
                (it.clinicianReview neq null) and
                    (it.cancellation eq null) and
                    (it.fulfilled.notNull.reject eq null)
            }
        )
    }

    private val awaitingAcceptOver1hCount = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending 0
        if (!isOps()) return@rememberSuspending 0
        val cutoff = Clock.System.now() - 1.hours
        session.prescriptionOrders.skipCache.count(
            condition<PrescriptionOrder> {
                (it.clinicianReview neq null) and
                    (it.cancellation eq null) and
                    (it.fulfilled eq null) and
                    (it.createdAt lt cutoff)
            }
        )
    }

    private val rejectedTodayCount = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending 0
        if (!isOps()) return@rememberSuspending 0
        val since = Clock.System.now() - 1.days
        // Order-level rejections only — bundle-level PharmacyOrder.totalRejection isn't
        // efficiently joinable without a PharmacyOrder list scan. TODO: include bundle rejections.
        session.prescriptionOrders.skipCache.count(
            condition<PrescriptionOrder> {
                (it.fulfilled.notNull.reject.notNull.at gte since)
            }
        )
    }

    // TODO: average-ship-time requires aggregating shippedAt - clinicianReview.at across
    // completed orders; not trivial client-side. Surfacing as "—" placeholder.

    /* ---- List data ---- */

    /**
     * Built as a remember because it depends on `isOps()` (itself a
     * remember). A non-suspending `remember` here causes the list pipeline
     * downstream of `data` to wedge while `isOps` is still loading — the page renders
     * the KPIs (which are remember) but the list area never populates.
     */
    val data = remember {
        val session = currentSession() ?: return@remember null
        if (!isOps()) return@remember null

        val parts: List<Condition<PrescriptionOrder>?> = buildList {
            clinicFilter()?.let { cid ->
                add(condition<PrescriptionOrder> { it.clinic eq cid })
            }
            pharmacyFilter()?.let { pid ->
                add(condition<PrescriptionOrder> { it.pharmacy eq pid })
            }
            fromDate()?.let { d ->
                val instant = d.atStartOfDayIn(TimeZone.currentSystemDefault())
                add(condition<PrescriptionOrder> { it.createdAt gte instant })
            }
            toDate()?.let { d ->
                val instant = d.atStartOfDayIn(TimeZone.currentSystemDefault())
                add(condition<PrescriptionOrder> { it.createdAt lte instant })
            }
            when (statusFilter()) {
                OrderStatusFilter.PendingSubmission -> add(condition<PrescriptionOrder> {
                    (it.clinicianReview eq null) and (it.cancellation eq null)
                })
                OrderStatusFilter.Cancelled -> add(condition<PrescriptionOrder> { it.cancellation neq null })
                OrderStatusFilter.Submitted -> add(condition<PrescriptionOrder> {
                    (it.clinicianReview neq null) and (it.fulfilled eq null) and (it.cancellation eq null)
                })
                else -> {}
            }
        }

        session.prescriptionOrders.query(
            Query(
                condition = if (parts.filterNotNull().isEmpty()) Condition.Always
                else Condition.And(parts.filterNotNull()),
                orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { !isOps() }.padded.col {
                centered.text("This view is restricted to HeroScript Ops.")
            }

            expanding.shownWhen { isOps() }.col {
                kpiRow()
                filterRow()

                val rawItems = remember { data()?.invoke() ?: emptyList() }

                val filtered = remember {
                    val session = currentSession() ?: return@remember emptyList()
                    val items = rawItems()
                    val q = search().trim().lowercase()
                    val status = statusFilter()
                    val alert = alertFilter()

                    items.filter { o ->
                        val po = o.fulfilled?.by?.let { session.pharmacyOrders[it].invoke() }
                        val sh = o.shipment?.let { session.shipments[it].invoke() }

                        val matchesStatus = when (status) {
                            OrderStatusFilter.All,
                            OrderStatusFilter.PendingSubmission,
                            OrderStatusFilter.Submitted,
                            OrderStatusFilter.Cancelled,
                                -> true
                            OrderStatusFilter.Accepted, OrderStatusFilter.InProcess ->
                                derivedStatus(o, po, sh) == DerivedStatus.InProcess
                            OrderStatusFilter.Shipped ->
                                sh?.shippedAt != null && o.cancellation == null && o.fulfilled?.reject == null
                            OrderStatusFilter.Rejected ->
                                o.fulfilled?.reject != null || po?.totalRejection != null
                        }
                        if (!matchesStatus) return@filter false

                        val reason = orderAlertReason(o, po)
                        val matchesAlert = when (alert) {
                            AlertFilter.All -> true
                            AlertFilter.Stuck -> reason == OrderAlertReason.Stuck
                            AlertFilter.Rejected -> reason == OrderAlertReason.Rejected
                            AlertFilter.AddressIssue -> reason == OrderAlertReason.AddressIssue
                        }
                        if (!matchesAlert) return@filter false

                        if (q.isBlank()) return@filter true
                        val patient = session.patients[o.patient].invoke()
                        val product = session.products[o.product].invoke()
                        val pharmacy = session.pharmacies[o.pharmacy].invoke()
                        val clinic = session.clinics[o.clinic].invoke()
                        val prescriber = session.users[o.prescribedBy].invoke()
                        listOfNotNull(
                            patient?.displayName,
                            product?.name,
                            pharmacy?.name,
                            clinic?.name,
                            prescriber?.displayName,
                        ).any { it.lowercase().contains(q) }
                    }
                }

                expanding.lazyColumn(
                    items = filtered,
                    id = { it._id },
                    loadMore = {
                        val d = data() ?: return@lazyColumn
                        d.limit = d().size + 20
                        delay(3.seconds)
                    },
                    render = { order ->
                        card.col {
                            link {
                                ::to {
                                    val id = order()._id
                                    { OrderDetailPage(id) }
                                }
                                orderRowContent(order, showClinic = true, showAlert = true)
                            }
                            opsActionRow(order)
                        }
                    },
                )

                shownWhen { filtered().isEmpty() }.padded.col {
                    centered.text("No orders match the current filters.")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.kpiRow() = row {
        kpiTile("Active", activeOrdersCount) {
            statusFilter.value = OrderStatusFilter.All
            alertFilter.value = AlertFilter.All
        }
        kpiTile("Stuck > 1h", awaitingAcceptOver1hCount) {
            statusFilter.value = OrderStatusFilter.Submitted
            alertFilter.value = AlertFilter.Stuck
        }
        kpiTile("Rejected 24h", rejectedTodayCount) {
            statusFilter.value = OrderStatusFilter.Rejected
            alertFilter.value = AlertFilter.All
        }
        card.col {
            subtext("Avg ship")
            // TODO: compute average shippedAt - clinicianReview.at over last 30 days.
            h3("—")
        }
    }

    private fun ElementWriter.CanAddTheme.kpiTile(
        label: String,
        count: Reactive<Int>,
        onTap: () -> Unit,
    ) = card.button {
        col {
            subtext(label)
            h3 { ::content { count().toString() } }
        }
        onClick { onTap() }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            expanding.fieldTheme.row {
                expanding.textInput {
                    hint = "Search patient, product, pharmacy, clinic, prescriber"
                    content bind search
                }
                icon(Icon.search, "search")
            }
        }
        row {
            card.col {
                subtext("Status")
                select {
                    bind(
                        edits = statusFilter,
                        data = Constant(OrderStatusFilter.entries.toList()),
                        render = { it.label },
                    )
                }
            }
            card.col {
                subtext("Alert")
                select {
                    bind(
                        edits = alertFilter,
                        data = Constant(AlertFilter.entries.toList()),
                        render = { it.label },
                    )
                }
            }
            card.col {
                subtext("From")
                localDateField { content bind fromDate }
            }
            card.col {
                subtext("To")
                localDateField { content bind toDate }
            }
        }
        row {
            card.col {
                subtext("Clinic")
                select {
                    bind(
                        edits = clinicFilter,
                        data = clinicOptions,
                        render = { id ->
                            if (id == null) "All clinics"
                            else clinicNameById.state.getOrNull()?.get(id) ?: "—"
                        },
                    )
                }
            }
            card.col {
                subtext("Pharmacy")
                select {
                    bind(
                        edits = pharmacyFilter,
                        data = pharmacyOptions,
                        render = { id ->
                            if (id == null) "All pharmacies"
                            else pharmacyNameById.state.getOrNull()?.get(id) ?: "—"
                        },
                    )
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.opsActionRow(order: Reactive<PrescriptionOrder>) = col {
        val showReroute = Signal(false)
        val showContact = Signal(false)
        val showCancel = Signal(false)
        val cancelReason = Signal("")

        row {
            expanding.space()
            button {
                text("Re-route")
                onClick {
                    showReroute.value = !showReroute.value
                    showContact.value = false
                    showCancel.value = false
                }
            }
            button {
                text("Contact pharmacy")
                onClick {
                    showContact.value = !showContact.value
                    showReroute.value = false
                    showCancel.value = false
                }
            }
            shownWhen {
                val o = order()
                o.cancellation == null && o.shipment == null
            }.button {
                text("Cancel")
                onClick {
                    showCancel.value = !showCancel.value
                    showReroute.value = false
                    showContact.value = false
                }
            }
        }

        shownWhen { showReroute() }.card.col {
            h4("Re-route to alternate pharmacy")
            val eligible = remember {
                val session = currentSession() ?: return@remember emptyList()
                val o = order()
                val state = o.destination.address.state.takeIf { it.length == 2 }
                    ?: return@remember emptyList()
                val mappings = session.productPharmacyMappings.query(
                    Query(
                        condition = Condition.And(
                            listOf(
                                condition<ProductPharmacyMapping> { it.product eq o.product },
                                condition<ProductPharmacyMapping> { it.form eq o.form },
                                condition<ProductPharmacyMapping> { it.active eq true },
                            )
                        )
                    )
                )()
                val byPharmacy = mappings.groupBy { it.pharmacy }
                byPharmacy.keys
                    .filter { it != o.pharmacy }
                    .mapNotNull { id -> session.pharmacies[id].invoke() }
                    .filter { ph -> ph.isActive && ph.states.any { it.state == state } }
            }
            reactive {
                clearChildren()
                val list = eligible()
                if (list.isEmpty()) {
                    subtext("No alternate pharmacies licensed in ${order().destination.address.state}.")
                } else {
                    list.forEach { ph ->
                        row {
                            expanding.text(ph.name)
                            button {
                                text("Confirm reroute")
                                // TODO: cancel original order with reason "Re-routed to ${ph.name}"
                                // and create a fresh PrescriptionOrder against the same Prescription.
                                // Also audit-log per ui.md Network Order Monitor § audit.
                                onClick { context.toast("Re-route coming soon") }
                            }
                        }
                    }
                }
            }
        }

        shownWhen { showContact() }.card.col {
            h4("Contact pharmacy")
            val pharmacy = remember {
                val session = currentSession() ?: return@remember null
                session.pharmacies[order().pharmacy].invoke()
            }
            row {
                subtext("Email")
                text { ::content { pharmacy()?.contactEmail?.raw ?: "—" } }
            }
            row {
                subtext("Phone")
                text { ::content { pharmacy()?.contactPhone?.raw ?: "—" } }
            }
            button {
                text("Send templated outreach")
                // TODO: dispatch via notification mechanism once it lands; audit-log per ui.md.
                onClick { context.toast("Outreach coming soon") }
            }
        }

        shownWhen { showCancel() }.card.col {
            h4("Cancel order")
            fieldTheme.textArea {
                hint = "Reason"
                content bind cancelReason
            }
            row {
                expanding.space()
                button {
                    text("Confirm cancel")
                    onClick {
                        val reason = cancelReason().trim()
                        if (reason.isBlank()) {
                            context.toast("Reason required")
                            return@onClick
                        }
                        val session = currentSession() ?: return@onClick
                        val self = session.self()
                        val o = order()
                        val updated = o.copy(
                            cancellation = PrescriptionOrder.Cancellation(
                                at = now(),
                                by = self._id,
                                reason = reason,
                            )
                        )
                        // TODO: audit-log per ui.md Network Order Monitor § audit (mechanism TBD).
                        session.prescriptionOrders[o._id].set(updated)
                        showCancel.value = false
                        cancelReason.value = ""
                        context.toast("Cancelled")
                    }
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable
enum class AlertFilter(val label: String) {
    All("All alerts"),
    Stuck("Stuck > 1h"),
    Rejected("Rejected"),
    AddressIssue("Address issue"),
}
