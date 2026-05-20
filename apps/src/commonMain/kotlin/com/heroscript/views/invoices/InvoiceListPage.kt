package com.heroscript.views.invoices

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalKiteUi::class)
@Routable("invoices")
class InvoiceListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Invoices")
    override var parentPage: Page? = null

    @QueryParameter
    val paidFilter = Signal(PaidFilter.All)

    @QueryParameter
    val fromDate = Signal<kotlinx.datetime.LocalDate?>(null)

    @QueryParameter
    val toDate = Signal<kotlinx.datetime.LocalDate?>(null)

    @QueryParameter
    val clinicFilter = Signal<Clinic.ID?>(null)

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val isActiveClinicAdmin = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending false
        val cid = activeClinic() ?: return@rememberSuspending false
        session.activeMemberships().any { it.clinic == cid && it.role == ClinicRole.ClinicAdmin }
    }

    private val allClinics = rememberSuspending {
        if (!isOps()) return@rememberSuspending emptyList()
        currentSession()?.clinics?.query(Query<Clinic>())?.invoke() ?: emptyList()
    }

    private val clinicNameById = rememberSuspending {
        allClinics().associate { it._id to it.name }
    }

    private val clinicOptions = rememberSuspending {
        listOf<Clinic.ID?>(null) + allClinics().map { it._id }
    }

    /**
     * Built as a rememberSuspending because it depends on other rememberSuspending values
     * (`isOps`, `isActiveClinicAdmin`). Using a non-suspending `remember` here caused the
     * outer reactive graph to re-throw ReactiveLoading repeatedly during initial load,
     * which surfaced as a JS stack overflow inside lazyColumn's Remember chain.
     */
    val data = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        val opsView = isOps()

        val clinicCondition = if (opsView) {
            clinicFilter()?.let { id -> condition<ClinicInvoice> { it.clinic eq id } }
        } else {
            val cid = activeClinic() ?: return@rememberSuspending null
            if (!isActiveClinicAdmin()) return@rememberSuspending null
            condition<ClinicInvoice> { it.clinic eq cid }
        }

        val paidCondition = when (paidFilter()) {
            PaidFilter.All -> null
            PaidFilter.Paid -> condition<ClinicInvoice> { it.paidAt neq null }
            PaidFilter.Unpaid -> condition<ClinicInvoice> { it.paidAt eq null }
        }

        val fromCondition = fromDate()?.let { d ->
            val instant = d.atStartOfDayIn(TimeZone.currentSystemDefault())
            condition<ClinicInvoice> { it.startPeriod gte instant }
        }
        val toCondition = toDate()?.let { d ->
            val instant = d.atStartOfDayIn(TimeZone.currentSystemDefault())
            condition<ClinicInvoice> { it.endPeriod lte instant }
        }

        session.clinicInvoices.query(
            Query(
                condition = Condition.And(listOfNotNull(clinicCondition, paidCondition, fromCondition, toCondition)),
                orderBy = sort<ClinicInvoice> { it.startPeriod.descending() },
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { !isOps() && (activeClinic() == null || !isActiveClinicAdmin()) }.card.col {
                h3("Access required")
                text("ClinicAdmin or Ops access required to view invoices.")
            }

            expanding.shownWhen { isOps() || (activeClinic() != null && isActiveClinicAdmin()) }.col {
                filterRow()

                val items = rememberSuspending { data()?.invoke() ?: emptyList() }

                expanding.lazyColumn(
                    items = items,
                    id = { it._id },
                    loadMore = {
                        val d = data() ?: return@lazyColumn
                        d.limit = d().size + 20
                        delay(3.seconds)
                    },
                    render = { invoice ->
                        card.link {
                            ::to {
                                val id = invoice()._id
                                { InvoiceDetailPage(id) }
                            }
                            invoiceRow(invoice)
                        }
                    },
                )

                shownWhen { items().isEmpty() }.padded.col {
                    centered.text("No invoices match the current filters.")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            card.col {
                subtext("Paid")
                select {
                    bind(
                        edits = paidFilter,
                        data = Constant(PaidFilter.entries.toList()),
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
        shownWhen { isOps() }.card.col {
            subtext("Clinic")
            select {
                bind(
                    edits = clinicFilter,
                    data = clinicOptions,
                    render = { id ->
                        if (id == null) "All clinics"
                        else clinicNameById.state.getOrNull()?.get(id) ?: id.raw.toString().take(8)
                    },
                )
            }
        }
    }

    private fun ElementWriter.CanAddTheme.invoiceRow(invoice: Reactive<ClinicInvoice>) = col {
        val clinic = rememberSuspending {
            val session = currentSession() ?: return@rememberSuspending null
            session.clinics[invoice().clinic].invoke()
        }
        row {
            expanding.col {
                h4 {
                    ::content {
                        val inv = invoice()
                        "${inv.startPeriod} – ${inv.endPeriod}"
                    }
                }
                shownWhen { isOps() }.subtext { ::content { clinic()?.name ?: "—" } }
                subtext {
                    ::content {
                        val inv = invoice()
                        if (inv.paidAt != null) "Paid ${inv.paidAt}" else "Unpaid"
                    }
                }
                subtext { ::content { "Stripe ${invoice().stripeId.take(12)}" } }
            }
            col {
                centered.h4 { ::content { formatCents(invoice().total) } }
            }
        }
    }
}

@kotlinx.serialization.Serializable
enum class PaidFilter(val label: String) {
    All("All"),
    Paid("Paid"),
    Unpaid("Unpaid"),
}

internal fun formatCents(cents: Int): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val dollars = abs / 100
    val rem = (abs % 100).toString().padStart(2, '0')
    return (if (negative) "-$" else "$") + "$dollars.$rem"
}
