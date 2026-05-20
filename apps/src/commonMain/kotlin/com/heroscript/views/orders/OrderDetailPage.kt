package com.heroscript.views.orders

import com.heroscript.*
import com.heroscript.extensions.DerivedStatus
import com.heroscript.extensions.derivedStatus
import com.heroscript.sdk.currentSession
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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Routable("orders/{id}")
class OrderDetailPage(val id: PrescriptionOrder.ID) : PageWithParent {
    override val title: Reactive<String> get() = Constant("Order")
    override var parentPage: Page = OrdersListPage()

    private val order = rememberSuspending {
        currentSession()?.prescriptionOrders?.get(id)?.invoke()
    }

    private val pharmacyOrder = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        order()?.fulfilled?.by?.let { session.pharmacyOrders[it].invoke() }
    }

    private val shipment = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        order()?.shipment?.let { session.shipments[it].invoke() }
    }

    private val patient = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        order()?.let { session.patients[it.patient].invoke() }
    }

    private val pharmacy = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        order()?.let { session.pharmacies[it.pharmacy].invoke() }
    }

    private val prescriber = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        order()?.let { session.users[it.prescribedBy].invoke() }
    }

    private val product = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        order()?.let { session.products[it.product].invoke() }
    }

    /** All sibling PrescriptionOrders bound to the same PharmacyOrder, including this one. */
    private val siblings = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val poId = pharmacyOrder()?._id ?: return@rememberSuspending emptyList()
        session.prescriptionOrders.query(
            Query(condition<PrescriptionOrder> { it.fulfilled.notNull.by eq poId })
        )()
    }

    /** Shipments referenced by any sibling order. */
    private val bundleShipments = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val ids = siblings().mapNotNull { it.shipment }.distinct()
        ids.mapNotNull { session.shipments[it].invoke() }
    }

    /** Other PrescriptionOrders that point to the same Shipment as this order. */
    private val shipmentSharers = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val sid = order()?.shipment ?: return@rememberSuspending emptyList()
        session.prescriptionOrders.query(
            Query(condition<PrescriptionOrder> { (it.shipment eq sid) and (it._id neq id) })
        )()
    }

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val me = rememberSuspending { currentSession()?.self?.invoke() }

    /**
     * Draft state: prescriber hasn't submitted yet and the order hasn't been cancelled.
     * Only the assigned prescriber may submit (per ui.md Order Entry footer actions).
     */
    private val canSubmit = remember {
        val o = order() ?: return@remember false
        val u = me() ?: return@remember false
        o.clinicianReview == null && o.cancellation == null && u._id == o.prescribedBy
    }

    /**
     * Hard blockers mirroring OrderEntryPage's Submit (ID.me) preconditions. ui.md § Order Entry
     * footer-actions says: prescriber DEA, controlled-substance privilege, and verified ship-to
     * address must all hold before submit. Sig and consent live on the order at this point so
     * we don't re-validate them here — only what the *server state* could now violate.
     */
    private val submitBlockers = remember {
        val o = order() ?: return@remember emptyList<String>()
        buildList {
            if (o.destination.verifiedAt == null) add("Ship-to address must be verified")
            val pre = prescriber()?.prescriber
            if (pre?.isDeaExpired == true) add("Prescriber DEA expired")
            if (product()?.controlled == true && pre?.canSubmitControlledSubstance != true) {
                add("Prescriber cannot submit controlled substances")
            }
        }
    }

    override fun ElementWriter.CanAddTheme.render() {
        scrolling.col {
            shownWhen { order() == null }.padded.col { centered.text("Loading...") }

            shownWhen { order() != null }.col {
                headerSection()
                packageSection()
                timelineSection()
                shipmentSection()
                siblingsSection()
                notificationsSection()
                apiExchangeSection()
                actionsSection()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.headerSection() = card.col {
        row {
            expanding.h2 {
                ::content { "Order " + (order()?._id?.raw?.toString()?.take(8) ?: "") }
            }
            subtext {
                ::content {
                    val o = order()
                    if (o == null) "" else derivedStatus(o, pharmacyOrder(), shipment()).label
                }
            }
        }
        col {
            row {
                subtext("Patient")
                text { ::content { patient()?.displayName ?: "—" } }
            }
            row {
                subtext("Pharmacy")
                text { ::content { pharmacy()?.name ?: "—" } }
            }
            row {
                subtext("Prescriber")
                text { ::content { prescriber()?.displayName ?: "—" } }
            }
            row {
                subtext("Product")
                text {
                    ::content {
                        val o = order()
                        if (o == null) "—" else {
                            val form = product()?.forms?.firstOrNull { it.form == o.form }
                            val unit = form?.strengthUnit
                            val strengthStr = if (unit != null) "${o.strength} $unit" else o.strength.toString()
                            "${product()?.name ?: "—"} · $strengthStr"
                        }
                    }
                }
            }
            row {
                // Drafts haven't been submitted yet — relabel so prescribers don't mistake
                // the createdAt for a submission timestamp (Fix 5).
                subtext {
                    ::content {
                        if (order()?.clinicianReview == null) "Created" else "Submitted"
                    }
                }
                text { ::content { order()?.createdAt?.toString() ?: "—" } }
            }
            row {
                subtext("ID.me event")
                text { ::content { order()?.clinicianReview?.idEvent ?: "—" } }
            }
            row {
                subtext("Total")
                text {
                    ::content {
                        val cents = order()?.fulfilled?.accept?.total ?: pharmacyOrder()?.accepted?.total
                        cents?.let { formatCents(it) } ?: "—"
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.packageSection() = card.col {
        shownWhen { bundleShipments().size > 1 }.col {
            h3("Packages")
            text {
                ::content {
                    val all = bundleShipments()
                    val shipped = all.count { it.shippedAt != null }
                    "Shipped $shipped of ${all.size}"
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.timelineSection() = card.col {
        h3("Status timeline")
        // Step rules — see derivedStatus() doc and ui.md "Order Detail" → "Status timeline derivation".
        reactive {
            clearChildren()
            val o = order() ?: return@reactive
            val po = pharmacyOrder()
            val s = shipment()

            val cancelled = o.cancellation
            val rejected = o.fulfilled?.reject ?: po?.totalRejection
            val submitted = o.clinicianReview?.takeIf { it.approved }
            val accepted = po?.accepted?.takeIf { o.fulfilled?.reject == null }
            val inProcess = accepted != null && (s == null || s.shippedAt == null)
            val shipped = s?.shippedAt

            if (cancelled != null) {
                timelineStep("Cancelled", cancelled.at.toString(), active = true)
                subtext("Reason: ${cancelled.reason}")
                return@reactive
            }
            if (rejected != null) {
                timelineStep("Rejected", rejected.at.toString(), active = true)
                subtext("Reason: ${rejected.reason}")
                return@reactive
            }

            timelineStep("Submitted", submitted?.at?.toString() ?: "—", active = submitted != null)
            timelineStep("Accepted", accepted?.at?.toString() ?: "—", active = accepted != null)
            timelineStep("In Process", if (inProcess) "Current" else "—", active = inProcess)
            timelineStep("Shipped", shipped?.toString() ?: "—", active = shipped != null)
        }
    }

    private fun ElementWriter.CanAddTheme.timelineStep(label: String, timestamp: String, active: Boolean) {
        row {
            expanding.col {
                if (active) text(label) else subtext(label)
                subtext(timestamp)
            }
        }
    }

    private fun ElementWriter.CanAddTheme.shipmentSection() = card.col {
        shownWhen { shipment() != null }.col {
            h3("Shipment")
            row {
                subtext("Carrier")
                text { ::content { shipment()?.carrier ?: "—" } }
            }
            row {
                subtext("Tracking #")
                text { ::content { shipment()?.trackingNumber ?: "—" } }
            }
            shownWhen { shipment()?.shippingUrl != null }.col {
                externalLink {
                    ::to { shipment()?.shippingUrl }
                    text { ::content { shipment()?.shippingUrl ?: "" } }
                }
            }
            row {
                subtext("Shipped")
                text { ::content { shipment()?.shippedAt?.toString() ?: "—" } }
            }
            shownWhen { shipment()?.deliveredAt != null }.row {
                subtext("Delivered")
                text { ::content { shipment()?.deliveredAt?.toString() ?: "—" } }
            }

            shownWhen { shipmentSharers().isNotEmpty() }.col {
                h4("This package also contains")
                col {
                    reactive {
                        clearChildren()
                        shipmentSharers().forEach { sib ->
                            link {
                                ::to {
                                    val sibId = sib._id
                                    { OrderDetailPage(sibId) }
                                }
                                text("${sib.patient.raw.toString().take(8)} · ${sib.strength}")
                            }
                        }
                    }
                }
            }
        }
        shownWhen { shipment() == null }.col {
            h3("Shipment")
            subtext("No shipment yet.")
        }
    }

    private fun ElementWriter.CanAddTheme.siblingsSection() = card.col {
        shownWhen { siblings().size > 1 }.col {
            h3("Other orders in this pharmacy bundle")
            col {
                reactive {
                    clearChildren()
                    val session = currentSession() ?: return@reactive
                    siblings().filter { it._id != id }.forEach { sib ->
                        card.link {
                            ::to {
                                val sibId = sib._id
                                { OrderDetailPage(sibId) }
                            }
                            col {
                                row {
                                    expanding.text {
                                        val sibPatient = rememberSuspending {
                                            session.patients[sib.patient].invoke()?.displayName ?: "—"
                                        }
                                        ::content { sibPatient() }
                                    }
                                    val sibPo = pharmacyOrder()
                                    val sibShipment = rememberSuspending {
                                        sib.shipment?.let { session.shipments[it].invoke() }
                                    }
                                    subtext {
                                        ::content { derivedStatus(sib, sibPo, sibShipment()).label }
                                    }
                                }
                                subtext {
                                    val sibProduct = rememberSuspending {
                                        session.products[sib.product].invoke()?.name ?: "—"
                                    }
                                    ::content { sibProduct() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.notificationsSection() = card.col {
        h3("Patient notifications")
        subtext("Notifications will appear here once the notification mechanism lands.")
    }

    private fun ElementWriter.CanAddTheme.apiExchangeSection() = col {
        shownWhen { isOps() }.card.col {
            h3("API exchange")
            subtext("Last pharmacy API exchange (payload hash, timestamp) — wires when audit mechanism lands.")
        }
    }

    private fun ElementWriter.CanAddTheme.actionsSection() = col {
        // Inline validation banner that surfaces the same hard blockers OrderEntryPage enforces
        // (DEA, controlled-substance privilege, verified ship-to) so a prescriber pressing Submit
        // here gets the same feedback as on the new-order screen.
        shownWhen { canSubmit() && submitBlockers().isNotEmpty() }.card.col {
            h4("Cannot submit yet")
            reactive {
                clearChildren()
                submitBlockers().forEach { subtext("- $it") }
            }
        }

        card.row {
            val ctx = context

            val canCancel = rememberSuspending {
                val o = order() ?: return@rememberSuspending false
                val status = derivedStatus(o, pharmacyOrder(), shipment())
                (status == DerivedStatus.Submitted || status == DerivedStatus.InProcess) && o.shipment == null
            }

            shownWhen { canSubmit() }.important.button {
                text { ::content { if (submitting()) "Submitting..." else "Submit (ID.me)" } }
                ::enabled { !submitting() && submitBlockers().isEmpty() }
                onClick {
                    openIdMeStub(ctx) {
                        submitDraft(ctx)
                    }
                }
            }

            shownWhen { canCancel() }.button {
                text("Cancel")
                // TODO: wire to PrescriptionOrder.cancellation update + pharmacy adapter cancel call.
                onClick { context.toast("Cancel coming soon") }
            }
            shownWhen { isOps() }.button {
                text("Re-route")
                // TODO: open re-route flow that submits a fresh PrescriptionOrder against the same Prescription
                // and cancels the original (per ui.md Ops actions).
                onClick { context.toast("Re-route coming soon") }
            }
            shownWhen { shipment() != null }.button {
                text("Resend tracking SMS")
                // TODO: dispatch via notification mechanism once it lands.
                onClick { context.toast("Resend coming soon") }
            }
        }
    }

    private val submitting = Signal(false)

    /**
     * Promote a draft to submitted: write `clinicianReview` + stamp `consentAffirmedAt`, then
     * persist. Mirrors the submit half of OrderEntryPage.saveOrder. Stays on the detail page so
     * the timeline reactively re-renders.
     */
    private suspend fun submitDraft(ctx: com.lightningkite.kiteui.views.ElementContext) {
        val session = currentSession() ?: return
        val u = me() ?: return
        val o = order() ?: return
        if (o.clinicianReview != null || o.cancellation != null) return
        if (u._id != o.prescribedBy) {
            ctx.toast("Only the assigned prescriber may submit")
            return
        }
        if (submitBlockers().isNotEmpty()) {
            ctx.toast("Resolve blockers first")
            return
        }
        submitting.value = true
        try {
            val now = Clock.System.now()
            val updated = o.copy(
                clinicianReview = ClinicianReview(
                    user = u._id,
                    idEvent = "stub-" + Uuid.random().toString(),
                    approved = true,
                    at = now,
                ),
                consentAffirmedAt = now,
            )
            session.prescriptionOrders[o._id].set(updated)
            ctx.toast("Submitted")
        } finally {
            submitting.value = false
        }
    }
}

private fun formatCents(cents: Int): String {
    val dollars = cents / 100
    val rem = (cents % 100).toString().padStart(2, '0')
    return "$$dollars.$rem"
}
