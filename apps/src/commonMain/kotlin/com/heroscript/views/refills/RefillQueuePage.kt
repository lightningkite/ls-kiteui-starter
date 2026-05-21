package com.heroscript.views.refills

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.views.components.productPicker
import com.heroscript.views.orders.OrderEntryPage
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
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Refill Queue — open Prescriptions with at least one prior PrescriptionOrder, ordered by
 * how overdue/imminent the next refill is. Refill-due date = lastOrder.createdAt +
 * (willLastDays * 1 day), per the prior order's prescriber-set willLastDays. No re-derivation.
 *
 * Dismissal is client-only (per ui.md): snoozed prescription IDs are held in an in-memory
 * Signal for the session.
 */
@OptIn(InternalKiteUi::class)
@Routable("refills")
class RefillQueuePage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Refill Queue")
    override var parentPage: Page? = null

    @QueryParameter
    val dueInDays = Signal(7)

    @QueryParameter
    val overdueOnly = Signal(false)

    @QueryParameter
    val expiredOnly = Signal(false)

    private val productFilter = Signal<Product?>(null)
    private val prescriberFilter = Signal<User?>(null)

    private val dismissed = Signal<Set<Prescription.ID>>(emptySet())

    private val openPrescriptions = remember {
        val session = currentSession() ?: return@remember null
        val clinicId = activeClinic() ?: return@remember null
        val nowInstant = Clock.System.now()
        val parts = buildList {
            add(condition<Prescription> { it.clinic eq clinicId })
            if (!expiredOnly()) {
                add(
                    condition<Prescription> { it.endsAt eq null } or
                        condition<Prescription> { it.endsAt.notNull gt nowInstant }
                )
            } else {
                add(condition<Prescription> { it.endsAt neq null })
                add(condition<Prescription> { it.endsAt.notNull lte nowInstant })
            }
            prescriberFilter()?.let { u -> add(condition<Prescription> { it.prescribedBy eq u._id }) }
            productFilter()?.let { p -> add(condition<Prescription> { it.product eq p._id }) }
        }
        session.prescriptions.query(
            Query(
                condition = Condition.And(parts),
                orderBy = sort<Prescription> { it.createdAt.descending() },
            )
        )
    }

    /**
     * For each prescription, look up its most recent PrescriptionOrder (N+1 per row).
     * Acceptable at V1 pilot scale; flag for batching once the queue exceeds a few hundred rows.
     * TODO: batch via `prescription inside ids` and group client-side once data volume warrants.
     */
    private val rows = remember {
        val session = currentSession() ?: return@remember emptyList()
        val prescriptions = openPrescriptions()?.invoke() ?: return@remember emptyList()
        val nowInstant = Clock.System.now()
        val horizon = nowInstant + (dueInDays() * 86_400).seconds

        prescriptions.mapNotNull { rx ->
            val lastOrder = session.prescriptionOrders.query(
                Query(
                    condition = condition<PrescriptionOrder> { it.prescription eq rx._id },
                    orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
                    limit = 1,
                )
            )().firstOrNull() ?: return@mapNotNull null

            val refillDue = lastOrder.createdAt + (lastOrder.willLastDays * 86_400).seconds
            RefillRow(rx, lastOrder, refillDue, nowInstant)
        }
            .filter { it.prescription._id !in dismissed() }
            .filter { it.refillDue <= horizon }
            .filter { !overdueOnly() || it.isOverdue }
            .sortedBy { it.refillDue }
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { activeClinic() == null }.card.col {
                h3("No active clinic")
                text("The refill queue is scoped to a clinic. Accept a clinic membership invite to view refills.")
            }

            expanding.shownWhen { activeClinic() != null }.col {
                filterRow()

                expanding.scrolling.col {
                    reactive {
                        clearChildren()
                        val list = rows()
                        if (list.isEmpty()) {
                            padded.col {
                                centered.text("No refills due in this window.")
                            }
                        } else {
                            list.forEach { entry ->
                                refillRowCard(entry)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            card.col {
                subtext("Due in")
                select {
                    bind(
                        edits = dueInDays,
                        data = Constant(listOf(0, 3, 7, 14, 30)),
                        render = { if (it == 0) "Overdue only" else "$it days" },
                    )
                }
            }
            card.row {
                centered.checkbox { checked bind overdueOnly }
                centered.text("Overdue only")
            }
            card.row {
                centered.checkbox { checked bind expiredOnly }
                centered.text("Rx expired")
            }
        }
        row {
            expanding.col {
                productPicker(productFilter)
            }
            expanding.col {
                prescriberPicker(prescriberFilter)
            }
        }
    }

    private fun ElementWriter.CanAddTheme.refillRowCard(entry: RefillRow) = card.col {
        val patient = remember {
            currentSession()?.patients?.get(entry.prescription.patient)?.invoke()
        }
        val product = remember {
            currentSession()?.products?.get(entry.prescription.product)?.invoke()
        }
        val pharmacy = remember {
            currentSession()?.pharmacies?.get(entry.lastOrder.pharmacy)?.invoke()
        }

        row {
            expanding.col {
                h4 { ::content { patient()?.displayName ?: "—" } }
                text {
                    ::content {
                        val name = product()?.name ?: "—"
                        val unit = product()?.forms?.firstOrNull { it.form == entry.prescription.form }?.strengthUnit
                        val strength = entry.prescription.strength
                        if (unit != null) "$name · $strength $unit" else "$name · $strength"
                    }
                }
                subtext { ::content { "Last fill ${entry.lastOrder.createdAt} · ${pharmacy()?.name ?: "—"}" } }
                row {
                    if (entry.isOverdue) {
                        danger.text("Overdue by ${entry.daysOverdue} d")
                    } else {
                        text("Due in ${entry.daysUntilDue} d")
                    }
                    entry.prescription.endsAt?.let { end ->
                        subtext(if (end < entry.now) "Rx expired $end" else "Rx ends $end")
                    }
                }
            }
            col {
                important.button {
                    text("Reorder")
                    onClick {
                        context.pageNavigator.navigate(OrderEntryPage(prescriptionId = entry.prescription._id))
                    }
                }
                button {
                    icon(Icon.close, "Snooze")
                    onClick {
                        dismissed.value = dismissed.value + entry.prescription._id
                        context.toast("Snoozed for 7 days")
                    }
                }
            }
        }
    }
}

private data class RefillRow(
    val prescription: Prescription,
    val lastOrder: PrescriptionOrder,
    val refillDue: Instant,
    val now: Instant,
) {
    val isOverdue: Boolean get() = refillDue < now
    val daysOverdue: Long get() = (now - refillDue).inWholeSeconds / 86_400L
    val daysUntilDue: Long get() = (refillDue - now).inWholeSeconds / 86_400L
}

/**
 * Search picker over the active clinic's Prescriber members. Local to the refill screen for now;
 * extract to views/components/ on the third reuse (per build-plan ground rules).
 */
private fun ElementWriter.CanAddTheme.prescriberPicker(value: MutableReactive<User?>) = col {
    val search = Signal("")

    val results = remember {
        val session = currentSession() ?: return@remember emptyList<User>()
        val clinicId = activeClinic() ?: return@remember emptyList<User>()
        val q = search().trim().takeIf { it.isNotBlank() } ?: return@remember emptyList<User>()
        val memberships = session.clinicMemberships.query(
            Query(condition<ClinicMembership> {
                (it.clinic eq clinicId) and (it.role eq ClinicRole.Prescriber) and (it.acceptedAt neq null) and (it.deactivatedAt eq null)
            })
        )()
        memberships
            .mapNotNull { session.users[it.user].invoke() }
            .filter {
                it.firstName.contains(q, ignoreCase = true) ||
                    it.lastName.contains(q, ignoreCase = true)
            }
            .take(10)
    }

    shownWhen { value() == null }.col {
        textInput {
            hint = "Search prescriber"
            content bind search
        }
        col {
            reactive {
                clearChildren()
                results().forEach { u ->
                    card.button {
                        text(u.displayName)
                        onClick {
                            value.set(u)
                            search.value = ""
                        }
                    }
                }
            }
        }
    }

    shownWhen { value() != null }.card.row {
        expanding.text { ::content { value()?.displayName ?: "" } }
        button {
            text("Clear")
            onClick { value.set(null) }
        }
    }
}
