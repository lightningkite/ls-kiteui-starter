package com.heroscript.views.invoices

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*
import kotlin.time.Clock

@Routable("invoices/{id}")
class InvoiceDetailPage(val id: ClinicInvoice.ID) : PageWithParent {
    override val title: Reactive<String> get() = Constant("Invoice")
    override var parentPage: Page = InvoiceListPage()

    private val invoice = remember {
        currentSession()?.clinicInvoices?.get(id)?.invoke()
    }

    private val clinic = remember {
        val session = currentSession() ?: return@remember null
        invoice()?.let { session.clinics[it.clinic].invoke() }
    }

    private val lineItems = remember {
        val session = currentSession() ?: return@remember emptyList()
        session.pharmacyOrders.query(
            Query(
                condition = condition<PharmacyOrder> { it.invoice eq id },
                orderBy = sort<PharmacyOrder> { it.createdAt.ascending() },
            )
        )()
    }

    private val isOps = remember {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    override fun ElementWriter.CanAddTheme.render() {
        scrolling.col {
            shownWhen { invoice() == null }.padded.col { centered.text("Loading...") }

            shownWhen { invoice() != null }.col {
                headerSection()
                lineItemsSection()
                receiptSection()
                opsSection()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.headerSection() = card.col {
        row {
            expanding.h2 {
                ::content { "Invoice " + (invoice()?._id?.raw?.toString()?.take(8) ?: "") }
            }
            subtext {
                ::content { if (invoice()?.paidAt != null) "Paid" else "Unpaid" }
            }
        }
        col {
            row {
                subtext("Clinic")
                text { ::content { clinic()?.name ?: "—" } }
            }
            row {
                subtext("Period start")
                text { ::content { invoice()?.startPeriod?.toString() ?: "—" } }
            }
            row {
                subtext("Period end")
                text { ::content { invoice()?.endPeriod?.toString() ?: "—" } }
            }
            row {
                subtext("Total")
                text { ::content { invoice()?.total?.let { formatCents(it) } ?: "—" } }
            }
            row {
                subtext("Paid at")
                text { ::content { invoice()?.paidAt?.toString() ?: "—" } }
            }
            row {
                subtext("Stripe ID")
                fieldTheme.text { ::content { invoice()?.stripeId ?: "—" } }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.lineItemsSection() = card.col {
        h3("Line items")

        shownWhen { lineItems().isEmpty() }.subtext("No pharmacy orders in this billing window.")

        col {
            reactive {
                clearChildren()
                lineItems().forEach { po ->
                    card.col { lineItemRow(po) }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.lineItemRow(order: PharmacyOrder) = col {
        val pharmacy = remember {
            val session = currentSession() ?: return@remember null
            session.pharmacies[order.pharmacy].invoke()
        }
        row {
            expanding.col {
                row {
                    expanding.h4 { ::content { pharmacy()?.name ?: "—" } }
                    subtext(
                        when {
                            order.totalRejection != null -> "Rejected"
                            order.accepted != null -> "Accepted"
                            else -> "Pending"
                        }
                    )
                }
                subtext("Created ${order.createdAt}")
                if (order.destinationIsClinic) subtext("Ships to clinic")
                else subtext("Ships to patient")
            }
            col {
                centered.text(order.accepted?.total?.let { formatCents(it) } ?: "—")
            }
        }
    }

    private fun ElementWriter.CanAddTheme.receiptSection() = card.col {
        h3("Receipt")
        row {
            button {
                text("Download / email receipt")
                onClick { context.toast("Receipt download coming soon") }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.opsSection() = col {
        shownWhen { isOps() }.card.col {
            h3("Ops actions")

            row {
                button {
                    text("Force regenerate")
                    onClick { context.toast("Force regenerate coming soon") }
                }
            }

            shownWhen { invoice()?.paidAt == null }.row {
                important.button {
                    text("Mark paid")
                    onClick {
                        val session = currentSession() ?: return@onClick
                        val current = invoice() ?: return@onClick
                        val updated = current.copy(paidAt = Clock.System.now())
                        session.clinicInvoices[current._id].set(updated)
                        context.toast("Marked paid")
                    }
                }
            }

            shownWhen { invoice()?.paidAt != null }.subtext {
                ::content { "Marked paid at ${invoice()?.paidAt}" }
            }
        }
    }
}
