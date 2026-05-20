package com.heroscript.views.clinics

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.views.components.addressEditor
import com.heroscript.views.components.isFilledOut
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.and
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.neq
import kotlin.time.Clock

@Routable("clinic-settings")
class ClinicSettingsPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Clinic Settings")
    override val parentPage: Page? = null

    private val editMode = Signal(false)
    private val draft = Signal<Clinic?>(null)

    private val loadedClinic = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        val cid = activeClinic() ?: return@rememberSuspending null
        session.clinics[cid].invoke()
    }

    private val me = rememberSuspending {
        currentSession()?.self?.invoke()
    }

    private val memberships = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        session.clinicMemberships.query(
            Query(condition<ClinicMembership> {
                (it.clinic eq cid) and (it.acceptedAt neq null) and (it.deactivatedAt eq null)
            })
        )()
    }

    private val myMembership: Reactive<ClinicMembership?> = rememberSuspending {
        val u = me() ?: return@rememberSuspending null
        memberships().firstOrNull { it.user == u._id }
    }

    private val isClinicAdmin: Reactive<Boolean> = rememberSuspending {
        myMembership()?.role == ClinicRole.ClinicAdmin
    }

    private val invitingOpen = Signal(false)

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            val current = loadedClinic()
            if (current != null && draft.value == null) draft.value = current
        }

        scrolling.col {
            shownWhen { activeClinic() == null }.padded.col {
                centered.text("No active clinic. Accept a clinic membership invite to manage clinic settings.")
            }

            shownWhen { activeClinic() != null && !isClinicAdmin() }.padded.col {
                centered.text("You need ClinicAdmin access to manage clinic settings.")
            }

            shownWhen { activeClinic() != null && isClinicAdmin() && draft() == null }.padded.col {
                centered.text("Loading...")
            }

            shownWhen { activeClinic() != null && isClinicAdmin() && draft() != null }.col {
                generalSection()
                billingSection()
                membersSection()
                actionRow()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.generalSection() = card.col {
        row {
            expanding.h2 { ::content { draft()?.name ?: "Clinic" } }
            button {
                text { ::content { if (editMode()) "Cancel" else "Edit" } }
                onClick {
                    if (editMode.value) {
                        loadedClinic()?.let { draft.value = it }
                        editMode.value = false
                        invitingOpen.value = false
                    } else {
                        editMode.value = true
                    }
                }
            }
        }
        subtext("Clinic name is managed by HeroScript Ops.")

        col {
            h4("Logo")
            subtext {
                ::content {
                    draft()?.logo?.let { "Logo on file: ${it.original}" } ?: "No logo on file."
                }
            }
            // TODO: file picker for clinic logo upload.
        }

        col {
            h3("Primary address")
            addressEditor(
                value = clinicField(
                    VerifiedAddress(address = Address.EMPTY),
                    { it.primaryAddress },
                    { d, v -> d.copy(primaryAddress = v) },
                ),
                editing = editMode,
            )
        }

        col {
            row {
                expanding.h3("Additional shipping addresses")
                shownWhen { editMode() }.button {
                    icon(Icon.add, "Add shipping address")
                    onClick {
                        val d = draft.value ?: return@onClick
                        draft.value = d.copy(
                            additionalShippingAddresses = d.additionalShippingAddresses +
                                VerifiedAddress(address = Address.EMPTY),
                        )
                    }
                }
            }
            col {
                reactive {
                    clearChildren()
                    val d = draft() ?: return@reactive
                    if (d.additionalShippingAddresses.isEmpty()) {
                        subtext("No additional shipping addresses on file.")
                    } else {
                        d.additionalShippingAddresses.forEachIndexed { index, _ ->
                            card.col {
                                addressEditor(
                                    value = shippingAddressField(index),
                                    editing = editMode,
                                )
                                atEnd.shownWhen { editMode() }.button {
                                    text("Remove")
                                    onClick {
                                        val current = draft.value ?: return@onClick
                                        draft.value = current.copy(
                                            additionalShippingAddresses = current.additionalShippingAddresses
                                                .toMutableList().also { it.removeAt(index) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.billingSection() = card.col {
        h3("Billing")

        shownWhen { editMode() }.col {
            field("Billing contact name") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind clinicField("", { it.billingContactName }, { d, v -> d.copy(billingContactName = v) })
                }
            }
            field("Billing contact email") {
                textInput {
                    keyboardHints = KeyboardHints.email
                    content bind clinicField(
                        "",
                        { it.billingContactEmail.raw },
                        { d, v -> d.copy(billingContactEmail = v.toEmailAddress()) },
                    )
                }
            }
            field("Payment method type") {
                select {
                    bind(
                        edits = clinicField(
                            PaymentType.Card,
                            { it.stripePaymentType },
                            { d, v -> d.copy(stripePaymentType = v) },
                        ),
                        data = Constant(PaymentType.entries.toList()),
                        render = { it.name },
                    )
                }
            }
            field("Payment ID") {
                textInput {
                    content bind clinicField("", { it.stripePaymentId }, { d, v -> d.copy(stripePaymentId = v) })
                }
            }
            // TODO: Real Stripe/Priority payment-method redirect goes here.
        }

        shownWhen { !editMode() }.col {
            text { ::content { draft()?.billingContactName ?: "" } }
            text { ::content { draft()?.billingContactEmail?.raw ?: "" } }
            subtext {
                ::content {
                    draft()?.let { d ->
                        "${d.stripePaymentType.name} · ${d.stripePaymentId.ifBlank { "no payment method on file" }}"
                    } ?: ""
                }
            }
        }

        card.col {
            h4("Last settlement")
            // TODO: Wire to ClinicInvoice once invoice surfacing lands; placeholder for now.
            subtext("Settlement history will appear here once invoicing is connected.")
        }
    }

    private fun ElementWriter.CanAddTheme.membersSection() = card.col {
        row {
            expanding.h3("Members")
            shownWhen { !invitingOpen() }.button {
                icon(Icon.add, "Invite member")
                onClick { invitingOpen.value = true }
            }
        }

        shownWhen { invitingOpen() }.col { inviteForm() }

        col {
            reactive {
                clearChildren()
                val list = memberships()
                if (list.isEmpty()) {
                    subtext("No active members.")
                } else {
                    list.forEach { membership ->
                        card.col { memberRow(membership) }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.memberRow(membership: ClinicMembership) {
        val pendingRole = Signal(membership.role)
        row {
            expanding.col {
                text {
                    ::content {
                        currentSession()?.users?.get(membership.user)?.invoke()?.displayName
                            ?: membership.user.toString()
                    }
                }
                subtext {
                    ::content {
                        currentSession()?.users?.get(membership.user)?.invoke()?.email?.raw ?: ""
                    }
                }
                subtext { ::content { "Current role: ${membership.role.name}" } }
            }
            col {
                field("Change role to") {
                    select {
                        bind(
                            edits = pendingRole,
                            data = Constant(ClinicRole.entries.toList()),
                            render = { it.name },
                        )
                    }
                }
                row {
                    shownWhen { pendingRole() != membership.role }.button {
                        text("Apply")
                        onClick {
                            val session = currentSession() ?: return@onClick
                            val newRole = pendingRole.value
                            if (membership.role == ClinicRole.ClinicAdmin
                                && newRole != ClinicRole.ClinicAdmin
                                && !hasOtherActiveAdmin(membership)
                            ) {
                                context.toast("Cannot demote the only active ClinicAdmin.")
                                pendingRole.value = membership.role
                                return@onClick
                            }
                            session.clinicMemberships[membership._id].set(
                                membership.copy(role = newRole),
                            )
                            context.toast("Role updated")
                        }
                    }
                    button {
                        text("Deactivate")
                        onClick {
                            val session = currentSession() ?: return@onClick
                            if (membership.role == ClinicRole.ClinicAdmin
                                && !hasOtherActiveAdmin(membership)
                            ) {
                                context.toast("Cannot remove the only active ClinicAdmin.")
                                return@onClick
                            }
                            session.clinicMemberships[membership._id].set(
                                membership.copy(deactivatedAt = Clock.System.now()),
                            )
                            context.toast("Member deactivated")
                        }
                    }
                }
            }
        }
    }

    private suspend fun hasOtherActiveAdmin(target: ClinicMembership): Boolean =
        memberships().any { it._id != target._id && it.role == ClinicRole.ClinicAdmin }

    private fun ElementWriter.CanAddTheme.inviteForm() = card.col {
        h4("Invite member")
        val email = Signal("")
        val role = Signal<ClinicRole>(ClinicRole.MedicalAssistant)

        field("Email") {
            textInput {
                keyboardHints = KeyboardHints.email
                content bind email
            }
        }
        field("Role") {
            select {
                bind(
                    edits = role,
                    data = Constant(listOf(ClinicRole.Prescriber, ClinicRole.MedicalAssistant, ClinicRole.ClinicAdmin)),
                    render = { it.name },
                )
            }
        }
        row {
            button {
                text("Cancel")
                onClick {
                    invitingOpen.value = false
                    email.value = ""
                }
            }
            atEnd.important.button {
                text("Send invite")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val cid = activeClinic() ?: return@onClick
                    val u = me() ?: return@onClick
                    val emailRaw = email.value.trim()
                    if (emailRaw.isBlank()) {
                        context.toast("Email is required.")
                        return@onClick
                    }
                    val parsedEmail = runCatching { emailRaw.toEmailAddress() }.getOrNull()
                    if (parsedEmail == null) {
                        context.toast("Invalid email.")
                        return@onClick
                    }
                    val existing = session.users.query(
                        Query(condition<User> { it.email eq parsedEmail })
                    )().firstOrNull()
                    val targetUser = existing ?: session.users.add(
                        User(
                            email = parsedEmail,
                            firstName = "",
                            lastName = "",
                        )
                    )
                    session.clinicMemberships.add(
                        ClinicMembership(
                            clinic = cid,
                            user = targetUser._id,
                            role = role.value,
                            invitedBy = u._id,
                        )
                    )
                    // TODO: Real activation email dispatch (handled server-side on insert in a later phase).
                    invitingOpen.value = false
                    email.value = ""
                    role.value = ClinicRole.MedicalAssistant
                    context.toast("Invite sent")
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
                val session = currentSession() ?: return@onClick
                session.clinics[d._id].set(d)
                editMode.value = false
                context.toast("Saved")
            }
        }
    }

    private fun validate(c: Clinic): List<String> = buildList {
        if (c.billingContactEmail.raw.isBlank()) add("billing contact email")
        if (c.billingContactName.isBlank()) add("billing contact name")
        if (!c.primaryAddress.address.isFilledOut()) add("primary address")
        c.additionalShippingAddresses.forEachIndexed { i, a ->
            if (!a.address.isFilledOut()) add("shipping address ${i + 1}")
        }
    }

    private fun <V> clinicField(
        default: V,
        get: (Clinic) -> V,
        set: (Clinic, V) -> Clinic,
    ): MutableReactive<V> =
        draft.lens(
            get = { current -> current?.let(get) ?: default },
            modify = { current, v -> current?.let { set(it, v) } },
        )

    private fun shippingAddressField(index: Int): MutableReactive<VerifiedAddress> =
        draft.lens(
            get = { current ->
                current?.additionalShippingAddresses?.getOrNull(index)
                    ?: VerifiedAddress(address = Address.EMPTY)
            },
            modify = { current, v ->
                current?.let { c ->
                    val list = c.additionalShippingAddresses.toMutableList()
                    if (index in list.indices) list[index] = v
                    c.copy(additionalShippingAddresses = list)
                }
            },
        )
}
