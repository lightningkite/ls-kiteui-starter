package com.heroscript.views

import com.heroscript.*
import com.heroscript.extensions.derivedStatus
import com.heroscript.sdk.currentSession
import com.heroscript.views.clinics.ClinicSettingsPage
import com.heroscript.views.orders.OrderDetailPage
import com.heroscript.views.orders.OrderEntryPage
import com.heroscript.views.patients.PatientDetailPage
import com.heroscript.views.patients.newPatientId
import com.heroscript.views.profile.ProfilePage
import com.heroscript.views.refills.RefillQueuePage
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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Routable("/dashboard")
class DashboardPage : Page {
    override val title: Reactive<String> get() = Constant("Dashboard")

    private val me = rememberSuspending { currentSession()?.self?.invoke() }

    private val memberships = rememberSuspending {
        currentSession()?.activeMemberships?.invoke() ?: emptyList()
    }

    private val activeClinicRecord = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        val cid = activeClinic() ?: return@rememberSuspending null
        session.clinics[cid].invoke()
    }

    private val activeMembership = rememberSuspending {
        val cid = activeClinic() ?: return@rememberSuspending null
        memberships().firstOrNull { it.clinic == cid }
    }

    private val isPrescriberHere = rememberSuspending {
        val u = me() ?: return@rememberSuspending false
        val m = activeMembership() ?: return@rememberSuspending false
        u.prescriber != null && m.role == ClinicRole.Prescriber
    }

    private val isMedicalAssistantHere = rememberSuspending {
        activeMembership()?.role == ClinicRole.MedicalAssistant
    }

    private val isClinicAdminHere = rememberSuspending {
        activeMembership()?.role == ClinicRole.ClinicAdmin
    }

    private val recentTouched = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        val uid = me()?._id ?: return@rememberSuspending emptyList()
        session.prescriptionOrders.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<PrescriptionOrder> { it.clinic eq cid },
                        condition<PrescriptionOrder> { it.createdBy eq uid } or
                            condition<PrescriptionOrder> { it.prescribedBy eq uid },
                    )
                ),
                orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
                limit = 5,
            )
        )()
    }

    private val prescriberDrafts = rememberSuspending {
        if (!isPrescriberHere()) return@rememberSuspending emptyList()
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        val uid = me()?._id ?: return@rememberSuspending emptyList()
        session.prescriptionOrders.query(
            Query(
                condition = condition<PrescriptionOrder> {
                    (it.clinic eq cid) and
                        (it.assignedTo eq uid) and
                        (it.clinicianReview eq null) and
                        (it.cancellation eq null)
                },
                orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
                limit = 5,
            )
        )()
    }

    private val maDrafts = rememberSuspending {
        if (!isMedicalAssistantHere()) return@rememberSuspending emptyList()
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        val uid = me()?._id ?: return@rememberSuspending emptyList()
        session.prescriptionOrders.query(
            Query(
                condition = condition<PrescriptionOrder> {
                    (it.clinic eq cid) and
                        (it.createdBy eq uid) and
                        (it.clinicianReview eq null) and
                        (it.cancellation eq null)
                },
                orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
                limit = 5,
            )
        )()
    }

    private val pendingInvites = rememberSuspending {
        if (!isClinicAdminHere()) return@rememberSuspending emptyList()
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        session.clinicMemberships.query(
            Query(condition<ClinicMembership> {
                (it.clinic eq cid) and (it.acceptedAt eq null) and (it.deactivatedAt eq null)
            })
        )()
    }

    private val prescribersInClinic = rememberSuspending {
        if (!isClinicAdminHere()) return@rememberSuspending emptyList<User>()
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        session.clinicMemberships.query(
            Query(condition<ClinicMembership> {
                (it.clinic eq cid) and (it.role eq ClinicRole.Prescriber) and
                    (it.acceptedAt neq null) and (it.deactivatedAt eq null)
            })
        )()
            .mapNotNull { session.users[it.user].invoke() }
            .filter { it.prescriber != null }
    }

    /** Open Prescriptions in this clinic — used by the MA refill summary. */
    private val openPrescriptions = rememberSuspending {
        if (!isMedicalAssistantHere()) return@rememberSuspending emptyList()
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        val nowInstant = Clock.System.now()
        session.prescriptions.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<Prescription> { it.clinic eq cid },
                        condition<Prescription> { it.endsAt eq null } or
                            condition<Prescription> { it.endsAt.notNull gt nowInstant },
                    )
                )
            )
        )()
    }

    private val refillSummary = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending RefillSummary(0, 0)
        val prescriptions = openPrescriptions()
        if (prescriptions.isEmpty()) return@rememberSuspending RefillSummary(0, 0)
        val nowInstant = Clock.System.now()
        val horizon = nowInstant + (7L * 86_400L).seconds
        var overdue = 0
        var dueSoon = 0
        prescriptions.forEach { rx ->
            val last = session.prescriptionOrders.query(
                Query(
                    condition = condition<PrescriptionOrder> { it.prescription eq rx._id },
                    orderBy = sort<PrescriptionOrder> { it.createdAt.descending() },
                    limit = 1,
                )
            )().firstOrNull() ?: return@forEach
            val refillDue = last.createdAt + (last.willLastDays.toLong() * 86_400L).seconds
            when {
                refillDue < nowInstant -> overdue++
                refillDue <= horizon -> dueSoon++
            }
        }
        RefillSummary(overdue, dueSoon)
    }

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            if (currentSession() == null) context.pageNavigator.reset(LoginPage())
        }

        scrolling.col {
            shownWhen { currentSession() == null }.padded.col { centered.text("Loading...") }

            shownWhen { currentSession() != null && activeClinic() == null }.card.col {
                h3("No active clinic")
                text("Accept a clinic membership invite to get started.")
            }

            shownWhen { currentSession() != null && activeClinic() != null }.col {
                activeClinicCard()
                recentActivityCard()
                announcementsCard()
                prescriberSections()
                medicalAssistantSections()
                clinicAdminSections()
                quickActionsCard()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.activeClinicCard() = card.col {
        row {
            expanding.col {
                subtext("Active clinic")
                h3 { ::content { activeClinicRecord()?.name ?: "—" } }
            }
        }
        shownWhen { memberships().size > 1 }.col {
            subtext("Switch clinic")
            row {
                reactive {
                    clearChildren()
                    val session = currentSession() ?: return@reactive
                    val current = activeClinic()
                    memberships().forEach { m ->
                        val isCurrent = m.clinic == current
                        card.button {
                            text {
                                ::content {
                                    session.clinics[m.clinic].invoke()?.name ?: "—"
                                }
                            }
                            ::enabled { !isCurrent }
                            onClick { activeClinic.value = m.clinic }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.recentActivityCard() = card.col {
        h3("Recent activity")
        shownWhen { recentTouched().isEmpty() }.col {
            text("No recent orders you've touched.")
        }
        shownWhen { recentTouched().isNotEmpty() }.col {
            reactive {
                clearChildren()
                val session = currentSession() ?: return@reactive
                recentTouched().forEach { order ->
                    card.link {
                        ::to {
                            val id = order._id
                            { OrderDetailPage(id) }
                        }
                        col {
                            row {
                                expanding.h4 {
                                    val patient = rememberSuspending {
                                        session.patients[order.patient].invoke()
                                    }
                                    val product = rememberSuspending {
                                        session.products[order.product].invoke()
                                    }
                                    ::content {
                                        val p = patient()?.displayName ?: "—"
                                        val pr = product()?.name ?: "—"
                                        "$p · $pr"
                                    }
                                }
                                subtext {
                                    val pharmacyOrder = rememberSuspending {
                                        order.fulfilled?.by?.let { session.pharmacyOrders[it].invoke() }
                                    }
                                    val shipment = rememberSuspending {
                                        order.shipment?.let { session.shipments[it].invoke() }
                                    }
                                    ::content {
                                        derivedStatus(order, pharmacyOrder(), shipment()).label
                                    }
                                }
                            }
                            subtext(order.createdAt.toString())
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.announcementsCard() = card.col {
        h3("Announcements")
        // TODO: Rendered when SystemAnnouncement model exists.
        subtext("No announcements.")
    }

    private fun ElementWriter.CanAddTheme.prescriberSections() = col {
        shownWhen { isPrescriberHere() }.col {
            prescriberDraftsCard()
            licenseWarningsCard()
            idMeLinkageCard()
        }
    }

    private fun ElementWriter.CanAddTheme.prescriberDraftsCard() = card.col {
        h3("Drafts awaiting your submission")
        shownWhen { prescriberDrafts().isEmpty() }.text("No drafts awaiting your submission.")
        shownWhen { prescriberDrafts().isNotEmpty() }.col {
            reactive {
                clearChildren()
                val session = currentSession() ?: return@reactive
                prescriberDrafts().forEach { order ->
                    card.link {
                        // TODO: prefer OrderEntryPage in submit mode once that mode exists;
                        // OrderDetailPage is the routable landing for now.
                        ::to {
                            val id = order._id
                            { OrderDetailPage(id) }
                        }
                        col {
                            row {
                                expanding.h4 {
                                    val patient = rememberSuspending {
                                        session.patients[order.patient].invoke()
                                    }
                                    val product = rememberSuspending {
                                        session.products[order.product].invoke()
                                    }
                                    ::content {
                                        val p = patient()?.displayName ?: "—"
                                        val pr = product()?.name ?: "—"
                                        "$p · $pr"
                                    }
                                }
                            }
                            subtext(order.createdAt.toString())
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.licenseWarningsCard() = card.col {
        h3("License expiration")
        col {
            reactive {
                clearChildren()
                val u = me() ?: return@reactive
                val lic = u.prescriber ?: return@reactive
                val nowInstant = Clock.System.now()
                val warnings = buildList {
                    daysUntil(lic.deaExpiration, nowInstant)?.let { add("DEA" to it) }
                    lic.stateLicenses.forEach { sl ->
                        daysUntil(sl.expiration, nowInstant)?.let { add(sl.state to it) }
                    }
                }
                if (warnings.isEmpty()) {
                    text("No expirations within 60 days.")
                } else {
                    warnings.forEach { (label, days) ->
                        link {
                            ::to { { ProfilePage() } }
                            row {
                                expanding.text("$label expires in $days d")
                                subtext(if (days <= 7) "Urgent" else if (days <= 30) "Soon" else "Heads up")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.idMeLinkageCard() = col {
        shownWhen { (me()?.prescriber?.idMeSubjectId) == null }.card.col {
            h3("Link ID.me")
            text("Your account is not linked to ID.me. Linkage is required to submit orders.")
            row {
                button {
                    text("Link ID.me")
                    onClick { context.toast("Coming soon") }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.medicalAssistantSections() = col {
        shownWhen { isMedicalAssistantHere() }.col {
            maDraftsCard()
            refillSummaryCard()
        }
    }

    private fun ElementWriter.CanAddTheme.maDraftsCard() = card.col {
        h3("My drafts in progress")
        shownWhen { maDrafts().isEmpty() }.text("No drafts in progress.")
        shownWhen { maDrafts().isNotEmpty() }.col {
            reactive {
                clearChildren()
                val session = currentSession() ?: return@reactive
                maDrafts().forEach { order ->
                    card.link {
                        ::to {
                            val id = order._id
                            { OrderDetailPage(id) }
                        }
                        col {
                            row {
                                expanding.h4 {
                                    val patient = rememberSuspending {
                                        session.patients[order.patient].invoke()
                                    }
                                    val product = rememberSuspending {
                                        session.products[order.product].invoke()
                                    }
                                    ::content {
                                        val p = patient()?.displayName ?: "—"
                                        val pr = product()?.name ?: "—"
                                        "$p · $pr"
                                    }
                                }
                            }
                            subtext(order.createdAt.toString())
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.refillSummaryCard() = card.link {
        ::to { { RefillQueuePage() } }
        col {
            h3("Refill queue")
            row {
                col {
                    subtext("Overdue")
                    h2 { ::content { refillSummary().overdue.toString() } }
                }
                col {
                    subtext("Due in 7 days")
                    h2 { ::content { refillSummary().dueSoon.toString() } }
                }
            }
            shownWhen { refillSummary().overdue == 0 && refillSummary().dueSoon == 0 }.col {
                text("No refills due in the next 7 days.")
            }
        }
    }

    private fun ElementWriter.CanAddTheme.clinicAdminSections() = col {
        shownWhen { isClinicAdminHere() }.col {
            pendingInvitesCard()
            clinicLicenseWarningsCard()
            invoicesPlaceholderCard()
        }
    }

    private fun ElementWriter.CanAddTheme.pendingInvitesCard() = card.link {
        ::to { { ClinicSettingsPage() } }
        col {
            h3("Pending invites")
            row {
                col {
                    subtext("Outstanding")
                    h2 { ::content { pendingInvites().size.toString() } }
                }
            }
            shownWhen { pendingInvites().isEmpty() }.text("No pending invites.")
        }
    }

    private fun ElementWriter.CanAddTheme.clinicLicenseWarningsCard() = card.col {
        h3("Prescriber licenses expiring")
        col {
            reactive {
                clearChildren()
                val nowInstant = Clock.System.now()
                val rows = buildList {
                    prescribersInClinic().forEach { u ->
                        val lic = u.prescriber ?: return@forEach
                        daysUntil(lic.deaExpiration, nowInstant)?.let { add(Triple(u, "DEA", it)) }
                        lic.stateLicenses.forEach { sl ->
                            daysUntil(sl.expiration, nowInstant)?.let { add(Triple(u, sl.state, it)) }
                        }
                    }
                }.sortedBy { it.third }
                if (rows.isEmpty()) {
                    text("No prescriber licenses expiring in the next 60 days.")
                } else {
                    rows.forEach { (u, label, days) ->
                        row {
                            expanding.text("${u.displayName} · $label")
                            subtext("in $days d")
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.invoicesPlaceholderCard() = card.col {
        h3("Open invoices")
        // TODO: Wires when invoice query is built or the Invoices screen lands.
        subtext("Coming soon.")
    }

    private fun ElementWriter.CanAddTheme.quickActionsCard() = card.col {
        h3("Quick actions")
        row {
            important.button {
                row {
                    icon(Icon.add, "")
                    text("Start new order")
                }
                onClick { context.pageNavigator.navigate(OrderEntryPage()) }
            }
            button {
                row {
                    icon(Icon.person, "")
                    text("Add patient")
                }
                onClick {
                    context.pageNavigator.navigate(
                        PatientDetailPage(newPatientId(), startInEditMode = true)
                    )
                }
            }
        }
    }
}

private data class RefillSummary(val overdue: Int, val dueSoon: Int)

/**
 * Returns the integer days from [now] until [expiration] if it falls within the
 * 60-day warning window (including already-expired). Null otherwise.
 */
private fun daysUntil(expiration: kotlin.time.Instant, now: kotlin.time.Instant): Long? {
    val days = (expiration - now).inWholeSeconds / 86_400L
    return if (days <= 60L) days else null
}
