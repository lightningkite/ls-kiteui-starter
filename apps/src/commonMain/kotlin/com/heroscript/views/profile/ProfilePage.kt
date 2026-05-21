package com.heroscript.views.profile

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.sdk.sessionToken
import com.heroscript.views.LoginPage
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
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.data.toPhoneNumber
import kotlin.time.Clock
import kotlin.time.Instant

@Routable("profile")
class ProfilePage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Profile")
    override val parentPage: Page? = null

    private val editMode = Signal(false)

    private val loaded = remember {
        currentSession()?.self?.invoke()
    }

    private val draft = Signal<User?>(null)

    private val memberships = remember {
        currentSession()?.activeMemberships?.invoke() ?: emptyList()
    }

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            val current = loaded()
            if (current != null && draft.value == null) draft.value = current
        }

        scrolling.col {
            shownWhen { draft() == null }.padded.col { centered.text("Loading...") }

            shownWhen { draft() != null }.col {
                identitySection()
                mfaSection()
                membershipsSection()
                prescriberSection()
                notificationsSection()
                signOutSection()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.identitySection() = card.col {
        row {
            expanding.h2 {
                ::content {
                    val d = draft()
                    when {
                        d == null -> "Profile"
                        d.firstName.isBlank() && d.lastName.isBlank() -> "Profile"
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
            atEnd.important.button {
                text("Save")
                onClick {
                    val d = draft.value ?: return@onClick
                    val problems = validate(d)
                    if (problems.isNotEmpty()) {
                        context.toast("Please fix: ${problems.joinToString(", ")}")
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

        shownWhen { !editMode() }.col {
            text { ::content { draft()?.email?.raw ?: "" } }
            shownWhen { draft()?.phoneNumber != null }.text { ::content { draft()?.phoneNumber?.raw ?: "" } }
            subtext { ::content { "Role: ${draft()?.role?.name ?: "User"}" } }
        }
    }

    private fun ElementWriter.CanAddTheme.mfaSection() = card.col {
        h3("Multi-factor authentication")
        text {
            ::content {
                draft()?.mfaEnrolledAt?.let { "Enrolled $it" } ?: "Not enrolled"
            }
        }
        row {
            button {
                text("Re-enroll")
                // TODO: real MFA re-enrollment flow.
                onClick { context.toast("Coming soon") }
            }
            button {
                text("Recovery codes")
                // TODO: real recovery-code regeneration flow.
                onClick { context.toast("Coming soon") }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.membershipsSection() = card.col {
        h3("Clinic memberships")
        col {
            reactive {
                clearChildren()
                val list = memberships()
                val session = currentSession()
                if (list.isEmpty()) {
                    subtext("No active clinic memberships.")
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
                            subtext(m.acceptedAt?.let { "Accepted $it" } ?: "Pending")
                        }
                    }
                }
            }
        }
        subtext("Membership changes are managed by your Clinic Admin in Clinic Settings.")
    }

    private fun ElementWriter.CanAddTheme.prescriberSection() = col {
        shownWhen { draft()?.prescriber != null }.col {
            deaCard()
            stateLicensesCard()
            idMeCard()
            reminderLogCard()
        }
    }

    private fun ElementWriter.CanAddTheme.deaCard() = card.col {
        h3("DEA")
        shownWhen { editMode() }.field("DEA number") {
            textInput {
                content bind prescriberField(
                    "",
                    { it.deaNumber },
                    { p, v -> p.copy(deaNumber = v) },
                )
            }
        }
        shownWhen { !editMode() }.text {
            ::content { draft()?.prescriber?.deaNumber ?: "" }
        }
        text {
            ::content {
                draft()?.prescriber?.let { "Expires ${it.deaExpiration}" } ?: ""
            }
        }
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
        // TODO: inline preview of deaLicenseImage; for now just show the reference.
        subtext {
            ::content {
                draft()?.prescriber?.deaLicenseImage?.let { "License image: ${it.location}" } ?: ""
            }
        }
        shownWhen { editMode() }.button {
            text("Replace image")
            // TODO: wire to file picker; replacement goes back into the Ops verification queue.
            onClick { context.toast("Coming soon") }
        }
    }

    private fun ElementWriter.CanAddTheme.stateLicensesCard() = card.col {
        row {
            expanding.h3("State medical licenses")
            shownWhen { editMode() }.button {
                icon(Icon.add, "Add state license")
                onClick {
                    val d = draft.value ?: return@onClick
                    val p = d.prescriber ?: return@onClick
                    draft.value = d.copy(
                        prescriber = p.copy(
                            stateLicenses = p.stateLicenses + PrescriberLicensing.StateMedicalLicense(
                                state = "",
                                licenseNumber = "",
                                expiration = now(),
                            ),
                        ),
                    )
                }
            }
        }
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
                            if (editMode()) stateLicenseEditor(license) { updated ->
                                val current = draft.value ?: return@stateLicenseEditor
                                val prescriber = current.prescriber ?: return@stateLicenseEditor
                                val currentList = prescriber.stateLicenses.toMutableList()
                                if (updated == null) currentList.removeAt(index)
                                else currentList[index] = updated
                                draft.value = current.copy(
                                    prescriber = prescriber.copy(stateLicenses = currentList.toSet()),
                                )
                            }
                            else stateLicenseReadOnly(license)
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
            license.review?.let { if (it.approved) "Verified" else "Rejected" } ?: "Pending verification"
        )
    }

    private fun ElementWriter.CanAddTheme.stateLicenseEditor(
        license: PrescriberLicensing.StateMedicalLicense,
        onChange: (PrescriberLicensing.StateMedicalLicense?) -> Unit,
    ) = col {
        val state = Signal(license.state)
        val number = Signal(license.licenseNumber)
        val expiration = Signal(license.expiration.toString())

        reactive {
            val parsed = runCatching { Instant.parse(expiration()) }.getOrNull() ?: license.expiration
            onChange(
                license.copy(
                    state = state().uppercase().take(2),
                    licenseNumber = number(),
                    expiration = parsed,
                )
            )
        }

        field("State") { textInput { hint = "e.g. CA"; content bind state } }
        field("License number") { textInput { content bind number } }
        field("Expiration (ISO instant)") { textInput { content bind expiration } }
        atEnd.button {
            text("Remove")
            onClick { onChange(null) }
        }
    }

    private fun ElementWriter.CanAddTheme.idMeCard() = card.col {
        h3("ID.me")
        text {
            ::content {
                draft()?.prescriber?.let { p ->
                    p.idMeSubjectId?.let { id ->
                        "Linked: $id" + (p.idMeLinkedAt?.let { " at $it" } ?: "")
                    } ?: "Not linked"
                } ?: ""
            }
        }
        button {
            text { ::content { if (draft()?.prescriber?.idMeSubjectId != null) "Re-link ID.me" else "Link ID.me" } }
            // TODO: real ID.me OAuth flow.
            onClick { context.toast("Coming soon") }
        }
    }

    private fun ElementWriter.CanAddTheme.reminderLogCard() = card.col {
        h3("Renewal reminders")
        subtext("Renewal reminders sent at 60/30/7 days will appear here once the notification mechanism lands.")
    }

    private fun ElementWriter.CanAddTheme.notificationsSection() = card.col {
        h3("Notification preferences")
        // TODO: wire when a user-preferences model exists.
        notificationToggle("Renewal reminders (DEA, state licenses)")
        notificationToggle("Expiring-license warnings")
        notificationToggle("Draft awaits you")
        notificationToggle("Settlement receipts")
        subtext("Preference storage coming soon.")
    }

    private fun ElementWriter.CanAddTheme.notificationToggle(label: String) = row {
        expanding.text(label)
        checkbox {
            enabled = false
            checked bind Signal(false)
        }
    }

    private fun ElementWriter.CanAddTheme.signOutSection() = row {
        atEnd.important.button {
            text("Sign out")
            onClick {
                try {
                    currentSession()?.api?.userAuth?.terminateSession()
                } catch (_: Exception) {
                } finally {
                    sessionToken set null
                    activeClinic.value = null
                    context.pageNavigator.reset(LoginPage())
                }
            }
        }
    }

    private fun validate(u: User): List<String> = buildList {
        if (u.firstName.isBlank()) add("first name")
        if (u.lastName.isBlank()) add("last name")
        if (u.email.raw.isBlank()) add("email")
    }

    private fun <V> userField(
        default: V,
        get: (User) -> V,
        set: (User, V) -> User,
    ): com.lightningkite.reactive.core.MutableReactive<V> =
        draft.lens(
            get = { current -> current?.let(get) ?: default },
            modify = { current, v -> current?.let { set(it, v) } },
        )

    private fun <V> prescriberField(
        default: V,
        get: (PrescriberLicensing) -> V,
        set: (PrescriberLicensing, V) -> PrescriberLicensing,
    ): com.lightningkite.reactive.core.MutableReactive<V> =
        draft.lens(
            get = { current -> current?.prescriber?.let(get) ?: default },
            modify = { current, v ->
                current?.let { u ->
                    val p = u.prescriber ?: return@let u
                    u.copy(prescriber = set(p, v))
                }
            },
        )
}
