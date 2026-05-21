package com.heroscript.views.orders

import com.heroscript.*
import com.heroscript.extensions.DerivedStatus
import com.heroscript.extensions.derivedStatus
import com.heroscript.sdk.currentSession
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
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalKiteUi::class)
@Routable("orders")
class OrdersListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Orders")
    override var parentPage: Page? = null

    @QueryParameter
    val statusFilter = Signal(OrderStatusFilter.All)

    @QueryParameter
    val fromDate = Signal<kotlinx.datetime.LocalDate?>(null)

    @QueryParameter
    val toDate = Signal<kotlinx.datetime.LocalDate?>(null)

    /**
     * Free-text search over patient / pharmacy / product / prescriber / MA names.
     * Applied client-side after the query since none of these fields are denormalized on the order.
     * TODO: replace with proper picker components once a reusable model picker exists.
     */
    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val controlledOnly = Signal(false)

    private val products = remember {
        val session = currentSession() ?: return@remember emptyList()
        session.products.query(Query<Product>())()
    }

    private val controlledProductIds = remember {
        products().filter { it.controlled }.map { it._id }.toSet()
    }

    val data = remember {
        val session = currentSession() ?: return@remember null
        val clinicId = activeClinic() ?: return@remember null

        val parts: List<Condition<PrescriptionOrder>?> = buildList {
            add(condition<PrescriptionOrder> { it.clinic eq clinicId })

            fromDate()?.let { d ->
                val instant = d.atStartOfDayIn(TimeZone.currentSystemDefault())
                add(condition<PrescriptionOrder> { it.createdAt gte instant })
            }
            toDate()?.let { d ->
                val instant = d.atStartOfDayIn(TimeZone.currentSystemDefault())
                add(condition<PrescriptionOrder> { it.createdAt lte instant })
            }
            if (controlledOnly()) {
                val ids = controlledProductIds()
                if (ids.isEmpty()) add(condition<PrescriptionOrder>(false))
                else add(condition<PrescriptionOrder> { it.product inside ids })
            }
            // Server-side narrowing for status filters that map directly to the order's own fields.
            // Derived statuses requiring PharmacyOrder/Shipment joins are filtered client-side below.
            // TODO: push derived-status filtering server-side once dataset growth makes client filtering impractical.
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
                condition = Condition.And(parts.filterNotNull()),
                orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { activeClinic() == null }.card.col {
                h3("No active clinic")
                text("Orders are scoped to a clinic. Accept a clinic membership invite to view orders.")
            }

            expanding.shownWhen { activeClinic() != null }.col {
                filterRow()

                val rawItems = remember { data()?.invoke() ?: emptyList() }

                // Client-side filter for derived statuses that need PharmacyOrder/Shipment joins,
                // and for the free-text search.
                val filtered = remember {
                    val session = currentSession() ?: return@remember emptyList()
                    val items = rawItems()
                    val q = search().trim().lowercase()
                    val status = statusFilter()

                    items.filter { o ->
                        val matchesStatus = when (status) {
                            OrderStatusFilter.All,
                            OrderStatusFilter.PendingSubmission,
                            OrderStatusFilter.Submitted,
                            OrderStatusFilter.Cancelled,
                                -> true
                            OrderStatusFilter.Accepted, OrderStatusFilter.InProcess -> {
                                val po = o.fulfilled?.by?.let { session.pharmacyOrders[it].invoke() }
                                val sh = o.shipment?.let { session.shipments[it].invoke() }
                                derivedStatus(o, po, sh) == DerivedStatus.InProcess
                            }
                            OrderStatusFilter.Shipped -> {
                                val sh = o.shipment?.let { session.shipments[it].invoke() }
                                sh?.shippedAt != null && o.cancellation == null && o.fulfilled?.reject == null
                            }
                            OrderStatusFilter.Rejected -> {
                                val po = o.fulfilled?.by?.let { session.pharmacyOrders[it].invoke() }
                                o.fulfilled?.reject != null || po?.totalRejection != null
                            }
                        }
                        if (!matchesStatus) return@filter false

                        if (q.isBlank()) return@filter true
                        val patient = session.patients[o.patient].invoke()
                        val product = session.products[o.product].invoke()
                        val pharmacy = session.pharmacies[o.pharmacy].invoke()
                        val prescriber = session.users[o.prescribedBy].invoke()
                        val creator = session.users[o.createdBy].invoke()
                        listOfNotNull(
                            patient?.displayName,
                            product?.name,
                            pharmacy?.name,
                            prescriber?.displayName,
                            creator?.displayName,
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
                        card.link {
                            ::to {
                                val id = order()._id
                                { OrderDetailPage(id) }
                            }
                            orderRowContent(order)
                        }
                    },
                )

                shownWhen { filtered().isEmpty() }.padded.col {
                    centered.text("No orders match the current filters.")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            expanding.fieldTheme.row {
                expanding.textInput {
                    hint = "Search patient, product, pharmacy, prescriber, MA"
                    content bind search
                }
                icon(Icon.search, "search")
            }
            card.row {
                centered.checkbox { checked bind controlledOnly }
                centered.text("Controlled")
            }
            card.button {
                icon(Icon.add, "New order")
                onClick { context.pageNavigator.navigate(OrderEntryPage()) }
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
                subtext("From")
                localDateField { content bind fromDate }
            }
            card.col {
                subtext("To")
                localDateField { content bind toDate }
            }
        }
    }

}

@kotlinx.serialization.Serializable
enum class OrderStatusFilter(val label: String) {
    All("All"),
    PendingSubmission("Pending submission"),
    Submitted("Submitted"),
    Accepted("Accepted"),
    InProcess("In Process"),
    Shipped("Shipped"),
    Cancelled("Cancelled"),
    Rejected("Rejected"),
}
