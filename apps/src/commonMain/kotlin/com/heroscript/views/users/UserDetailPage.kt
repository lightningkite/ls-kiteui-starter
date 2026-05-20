package com.heroscript.views.users

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.views.orders.OrdersListPage
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.data.EmailAddress
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.data.toPhoneNumber
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.and
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.neq
import kotlin.time.Clock
import kotlin.time.Instant

private enum class ReviewTarget { Dea, State }

@Routable("ops/users/{id}")
class UserDetailPage(
    val id: User.ID,
    val startInEditMode: Boolean = false,
) : PageWithParent {
    override val title: Reactive<String> get() = Constant("User")
    override var parentPage: Page = UserListPage()

    private val editMode = Signal(startInEditMode)

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val me = rememberSuspending { currentSession()?.self?.invoke() }

    private val canPromoteToAdmin = rememberSuspending {
        (me()?.role ?: UserRole.User) >= UserRole.Developer
    }

    private val loaded = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        session.users[id].invoke()
    }

    private val draft = Signal<User?>(null)

    private val reviewTarget = Signal<ReviewTarget?>(null)
    private val reviewStateIndex = Signal<Int?>(null)
    private val reviewApproved = Signal(true)
    private val reviewNotes = Signal("")

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            val current = loaded()
            if (current != null) {
                if (draft.value == null) draft.value = current
            } else if (draft.value == null && startInEditMode) {
                @Suppress("DEPRECATION")
                draft.value = User(
                    _id = id,
                    email = EmailAddress(""),
                    firstName = "",
                    lastName = "",
                    role = UserRole.User,
                )
            }
        }

        scrolling.col {
            shownWhen { !isOps() }.padded.col {
                centered.text("This view is restricted to HeroScript Ops.")
            }

            expanding.shownWhen { isOps() }.col {
                shownWhen { draft() == null }.padded.col { centered.text("Loading...") }

                shownWhen { draft() != null }.col {
                    identitySection()
                    sessionSection()
                    prescriberSection()
                    membershipsSection()
                    activitySection()
                    dangerSection()
                    actionRow()
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.identitySection() = card.col {
        row {
            expanding.h2 {
                ::content {
                    val d = draft()
                    when {
                        d == null -> "User"
                        d.firstName.isBlank() && d.lastName.isBlank() && d.email.raw.isBlank() -> "New user"
                        d.firstName.isBlank() && d.lastName.isBlank() -> d.email.raw
                        else -> d.displayName
                    }
                }
            }
            button {
                text { ::content { if (editMode()) "Cancel" else "Edit" } }
                onClick {
                    if (editMode.value) {
                        loaded()?.let { draft.value = it }
                        editMode.value = false
                    } else {
                        editMode.value = true
                    }
                }
            }
        }

        shownWhen { editMode() }.col {
            field("First name") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind userField("", { it.firstName }, { d, v -> d.copy(firstName = v) })
                }
            }
            field("Last name") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind userField("", { it.lastName }, { d, v -> d.copy(lastName = v) })
                }
            }
            field("Email") {
                textInput {
                    keyboardHints = KeyboardHints.email
                    content bind userField(
                        "",
                        { it.email.raw },
                        { d, v -> d.copy(email = v.toEmailAddress()) },
                    )
                }
            }
            field("Phone") {
                textInput {
                    keyboardHints = KeyboardHints.phone
                    content bind userField(
                        "",
                        { it.phoneNumber?.raw ?: "" },
                        { d, v -> d.copy(phoneNumber = v.takeIf { it.isNotBlank() }?.toPhoneNumber()) },
                    )
                }
            }
            field("System role") {
                select {
                    bind(
                        edits = userField(UserRole.User, { it.role }, { d, v -> d.copy(role = v) }),
                        data = Constant(UserRole.entries.toList()),
                        render = { it.name },
                    )
                }
            }
            subtext {
                ::content {
                    val r = draft()?.role ?: UserRole.User
                    if (r >= UserRole.Admin && !canPromoteToAdmin()) {
                        "Only Developer or Root can promote to Admin or above."
                    } else ""
                }
            }
        }

        shownWhen { !editMode() }.col {
            text { ::content { draft()?.email?.raw ?: "" } }
            shownWhen { draft()?.phoneNumber != null }.text { ::content { draft()?.phoneNumber?.raw ?: "" } }
            subtext { ::content { "Role: ${draft()?.role?.name ?: "User"}" } }
        }
    }

    private fun ElementWriter.CanAddTheme.sessionSection() = card.col {
        h3("Session & MFA")
        text {
            ::content {
                draft()?.mfaEnrolledAt?.let { "MFA enrolled $it" } ?: "MFA not enrolled"
            }
        }
        subtext {
            ::content {
                draft()?.lastLoginAt?.let { "Last login $it" } ?: "Never logged in"
            }
        }
    }

    private fun ElementWriter.CanAddTheme.prescriberSection() = col {
        shownWhen { draft()?.prescriber != null }.col {
            deaCard()
            stateLicensesCard()
            idMeCard()
        }
    }

    private fun ElementWriter.CanAddTheme.deaCard() = card.col {
        row {
            expanding.h3("DEA")
            shownWhen {
                val p = draft()?.prescriber
                p != null && reviewTarget() != ReviewTarget.Dea
            }.button {
                text("Verify DEA")
                onClick { openReview(ReviewTarget.Dea, null) }
            }
        }
        text { ::content { draft()?.prescriber?.deaNumber ?: "" } }
        text { ::content { draft()?.prescriber?.let { "Expires ${it.deaExpiration}" } ?: "" } }
        text {
            ::content {
                draft()?.prescriber?.let { p ->
                    when {
                        p.isDeaExpired -> "Expired"
                        p.isDeaVerified -> "Verified"
                        else -> "Pending verification"
                    }
                } ?: ""
            }
        }
        subtext {
            ::content {
                draft()?.prescriber?.deaLicenseImage?.let { "License image: ${it.location}" } ?: ""
            }
        }
        subtext {
            ::content {
                val r = draft()?.prescriber?.deaReview
                r?.let {
                    "Reviewed ${it.at}: ${if (it.approved) "approved" else "rejected"}" +
                        if (it.notes.isNotBlank()) " — ${it.notes}" else ""
                } ?: ""
            }
        }

        shownWhen { reviewTarget() == ReviewTarget.Dea }.col {
            reviewModal()
        }
    }

    private fun ElementWriter.CanAddTheme.stateLicensesCard() = card.col {
        h3("State medical licenses")
        col {
            reactive {
                clearChildren()
                val p = draft()?.prescriber ?: return@reactive
                val licenses = p.stateLicenses.toList()
                if (licenses.isEmpty()) {
                    subtext("No state licenses on file.")
                } else {
                    licenses.forEachIndexed { index, license ->
                        card.col {
                            row {
                                expanding.col { stateLicenseReadOnly(license) }
                                shownWhen {
                                    !(reviewTarget() == ReviewTarget.State && reviewStateIndex() == index)
                                }.button {
                                    text("Verify license")
                                    onClick { openReview(ReviewTarget.State, index) }
                                }
                            }
                            shownWhen {
                                reviewTarget() == ReviewTarget.State && reviewStateIndex() == index
                            }.col {
                                reviewModal()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.stateLicenseReadOnly(license: PrescriberLicensing.StateMedicalLicense) = col {
        text("${license.state} · ${license.licenseNumber}")
        subtext("Expires ${license.expiration}")
        subtext(
            license.review?.let {
                "Reviewed ${it.at}: ${if (it.approved) "approved" else "rejected"}" +
                    if (it.notes.isNotBlank()) " — ${it.notes}" else ""
            } ?: "Pending verification"
        )
    }

    private fun ElementWriter.CanAddTheme.idMeCard() = card.col {
        h3("ID.me")
        text {
            ::content {
                draft()?.prescriber?.let { p ->
                    p.idMeSubjectId?.let { "Linked: $it" + (p.idMeLinkedAt?.let { at -> " at $at" } ?: "") }
                        ?: "Not linked"
                } ?: ""
            }
        }
        subtext("ID.me linkage is established by the user from their Profile.")
    }

    private fun ElementWriter.CanAddTheme.reviewModal() = card.col {
        h4("Record verification")
        field("Decision") {
            select {
                bind(
                    edits = reviewApproved,
                    data = Constant(listOf(true, false)),
                    render = { if (it) "Approve" else "Reject" },
                )
            }
        }
        field("Notes") { textInput { content bind reviewNotes } }
        row {
            button {
                text("Cancel")
                onClick { closeReview() }
            }
            atEnd.important.button {
                text("Save verification")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val d = draft.value ?: return@onClick
                    val p = d.prescriber ?: return@onClick
                    val self = me() ?: return@onClick
                    val review = PrescriberLicensing.Review(
                        byUser = self._id,
                        approved = reviewApproved.value,
                        notes = reviewNotes.value,
                        at = Clock.System.now(),
                    )
                    val updatedPrescriber = when (reviewTarget.value) {
                        ReviewTarget.Dea -> p.copy(deaReview = review)
                        ReviewTarget.State -> {
                            val index = reviewStateIndex.value ?: return@onClick
                            val licenses = p.stateLicenses.toList()
                            if (index !in licenses.indices) return@onClick
                            val updated = licenses.toMutableList().also {
                                it[index] = it[index].copy(review = review)
                            }
                            p.copy(stateLicenses = updated.toSet())
                        }
                        null -> return@onClick
                    }
                    val updatedUser = d.copy(prescriber = updatedPrescriber, updatedAt = Clock.System.now())
                    session.users[d._id].set(updatedUser)
                    draft.value = updatedUser
                    closeReview()
                    context.toast("Verification recorded")
                }
            }
        }
    }

    private fun openReview(target: ReviewTarget, stateIndex: Int?) {
        reviewTarget.value = target
        reviewStateIndex.value = stateIndex
        reviewApproved.value = true
        reviewNotes.value = ""
    }

    private fun closeReview() {
        reviewTarget.value = null
        reviewStateIndex.value = null
        reviewNotes.value = ""
    }

    private fun ElementWriter.CanAddTheme.membershipsSection() = card.col {
        h3("Clinic memberships")
        val memberships = rememberSuspending {
            val session = currentSession() ?: return@rememberSuspending emptyList()
            session.clinicMemberships.query(
                Query(condition<ClinicMembership> { it.user eq id })
            )()
        }
        col {
            reactive {
                clearChildren()
                val list = memberships()
                val session = currentSession()
                if (list.isEmpty()) {
                    subtext("No clinic memberships on file.")
                } else {
                    list.forEach { m ->
                        card.col {
                            text {
                                ::content {
                                    if (session == null) m.clinic.toString()
                                    else session.clinics[m.clinic]()?.name ?: m.clinic.toString()
                                }
                            }
                            subtext("Role: ${m.role.name}")
                            row {
                                subtext("Invited ${m.invitedAt}")
                                subtext(m.acceptedAt?.let { "Accepted $it" } ?: "Not yet accepted")
                            }
                            subtext(
                                m.deactivatedAt?.let { "Deactivated $it" }
                                    ?: if (m.isActive) "Active" else "Inactive"
                            )
                        }
                    }
                }
            }
        }
        subtext("Clinic membership changes are managed from Clinic Settings or Ops Clinic Detail.")
    }

    private fun ElementWriter.CanAddTheme.activitySection() = col {
        shownWhen { draft()?.prescriber != null }.card.col {
            h3("Orders prescribed")
            val count = rememberSuspending {
                val session = currentSession() ?: return@rememberSuspending 0
                session.prescriptionOrders.skipCache.count(
                    condition<PrescriptionOrder> { it.prescribedBy eq id }
                )
            }
            text { ::content { "${count()} orders prescribed by this user" } }
            row {
                button {
                    text("View in Orders")
                    // TODO: navigate with a `prescribedBy` filter once OrdersListPage exposes one.
                    onClick { context.pageNavigator.navigate(OrdersListPage()) }
                }
            }
        }

        card.col {
            h3("Orders drafted")
            val count = rememberSuspending {
                val session = currentSession() ?: return@rememberSuspending 0
                session.prescriptionOrders.skipCache.count(
                    condition<PrescriptionOrder> { it.createdBy eq id }
                )
            }
            text { ::content { "${count()} orders created by this user" } }
            row {
                button {
                    text("View in Orders")
                    // TODO: navigate with a `createdBy` filter once OrdersListPage exposes one.
                    onClick { context.pageNavigator.navigate(OrdersListPage()) }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.dangerSection() = card.col {
        h3("Deactivation")
        text {
            ::content {
                draft()?.deactivatedAt?.let { "Deactivated $it" } ?: "Currently active"
            }
        }

        val confirming = Signal(false)
        row {
            shownWhen { draft()?.deactivatedAt == null && !confirming() }.button {
                text("Deactivate user")
                onClick { confirming.value = true }
            }
            shownWhen { confirming() }.col {
                text("Confirm deactivation?")
                row {
                    button {
                        text("Cancel")
                        onClick { confirming.value = false }
                    }
                    important.button {
                        text("Deactivate")
                        onClick {
                            val session = currentSession() ?: return@onClick
                            val d = draft.value ?: return@onClick
                            val updated = d.copy(
                                deactivatedAt = Clock.System.now(),
                                updatedAt = Clock.System.now(),
                            )
                            session.users[d._id].set(updated)
                            draft.value = updated
                            confirming.value = false
                            context.toast("User deactivated")
                        }
                    }
                }
            }
            shownWhen { draft()?.deactivatedAt != null }.button {
                text("Reactivate")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val d = draft.value ?: return@onClick
                    val updated = d.copy(deactivatedAt = null, updatedAt = Clock.System.now())
                    session.users[d._id].set(updated)
                    draft.value = updated
                    context.toast("User reactivated")
                }
            }
        }
    }

    private fun ViewWriter.actionRow() = row {
        atEnd.shownWhen { editMode() }.important.button {
            text("Save")
            onClick {
                val d = draft.value ?: return@onClick
                val problems = validate(d)
                if (problems.isNotEmpty()) {
                    context.toast("Please fix: ${problems.joinToString(", ")}")
                    return@onClick
                }
                if (d.role >= UserRole.Admin && !canPromoteToAdmin()) {
                    context.toast("Only Developer or Root can promote to Admin or above.")
                    return@onClick
                }
                val session = currentSession() ?: return@onClick
                val updated = d.copy(updatedAt = Clock.System.now())
                session.users[d._id].set(updated)
                draft.value = updated
                editMode.value = false
                context.toast("Saved")
            }
        }
    }

    private fun validate(u: User): List<String> = buildList {
        if (u.firstName.isBlank()) add("first name")
        if (u.lastName.isBlank()) add("last name")
        if (u.email.raw.isBlank()) add("email")
        else if (!isLikelyEmail(u.email.raw)) add("valid email")
        // TODO: server-side email-uniqueness check on edit; today the unique index will reject the save.
    }

    private fun isLikelyEmail(raw: String): Boolean {
        val at = raw.indexOf('@')
        if (at <= 0 || at >= raw.length - 1) return false
        val dot = raw.indexOf('.', at)
        return dot in (at + 2)..(raw.length - 2)
    }

    private fun <V> userField(
        default: V,
        get: (User) -> V,
        set: (User, V) -> User,
    ): MutableReactive<V> =
        draft.lens(
            get = { current -> current?.let(get) ?: default },
            modify = { current, v -> current?.let { set(it, v) } },
        )
}
