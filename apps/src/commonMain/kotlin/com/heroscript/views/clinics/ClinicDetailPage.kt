package com.heroscript.views.clinics

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.views.components.addressEditor
import com.heroscript.views.components.isFilledOut
import com.heroscript.views.invoices.InvoiceListPage
import com.heroscript.views.orders.OrdersListPage
import com.heroscript.views.patients.PatientListPage
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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.and
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.neq
import kotlin.time.Clock

@Routable("ops/clinics/{id}")
class ClinicDetailPage(
    val id: Clinic.ID,
    val startInEditMode: Boolean = false,
) : PageWithParent {
    override val title: Reactive<String> get() = Constant("Clinic")
    override var parentPage: Page = ClinicListPage()

    private val editMode = Signal(startInEditMode)
    private val draft = Signal<Clinic?>(null)

    /** True while this page is composing a brand-new clinic that hasn't been saved yet. */
    private val newClinicMode = Signal(startInEditMode)

    /** Inline first-admin invite form, shown after the first save in new-clinic mode. */
    private val firstAdminOpen = Signal(false)
    private val firstAdminEmail = Signal("")
    private val firstAdminFirstName = Signal("")
    private val firstAdminLastName = Signal("")

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val loaded = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        if (newClinicMode()) return@rememberSuspending null
        session.clinics[id].invoke()
    }

    private val memberships = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        if (newClinicMode()) return@rememberSuspending emptyList()
        session.clinicMemberships.query(
            Query(condition<ClinicMembership> { it.clinic eq id })
        )()
    }

    private val activeMemberUserIds = rememberSuspending {
        memberships().filter { it.isActive }.map { it.user }.toSet()
    }

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            if (newClinicMode() && draft.value == null) {
                @Suppress("DEPRECATION")
                draft.value = Clinic(
                    _id = id,
                    name = "",
                    primaryAddress = VerifiedAddress(address = Address.EMPTY),
                    billingContactEmail = "placeholder@example.com".toEmailAddress(),
                    billingContactName = "",
                    stripePaymentId = "",
                    stripePaymentType = PaymentType.Card,
                )
            } else if (!newClinicMode()) {
                val current = loaded()
                if (current != null && draft.value == null) draft.value = current
            }
        }

        scrolling.col {
            shownWhen { !isOps() }.padded.col {
                centered.text("This view is restricted to HeroScript Ops.")
            }

            shownWhen { isOps() && draft() == null }.padded.col { centered.text("Loading...") }

            shownWhen { isOps() && draft() != null }.col {
                generalSection()
                billingSection()
                shownWhen { firstAdminOpen() }.col { firstAdminSection() }
                shownWhen { !newClinicMode() }.col {
                    membershipsSection()
                    sublistsSection()
                    actionsSection()
                }
                actionRow()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.generalSection() = card.col {
        row {
            expanding.h2 {
                ::content {
                    val d = draft()
                    when {
                        d == null -> "Clinic"
                        newClinicMode() && d.name.isBlank() -> "New clinic"
                        d.name.isBlank() -> "(unnamed clinic)"
                        else -> d.name
                    }
                }
            }
            shownWhen { !newClinicMode() }.button {
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
            field("Clinic name") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind clinicField("", { it.name }, { d, v -> d.copy(name = v) })
                }
            }
        }
        shownWhen { !editMode() }.col {
            subtext { ::content { if (draft()?.isActive == false) "Deactivated" else "Active" } }
        }

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

        shownWhen { !editMode() && !newClinicMode() }.col {
            subtext { ::content { draft()?.let { "Created ${it.createdAt}" } ?: "" } }
            shownWhen { draft()?.deactivatedAt != null }.subtext {
                ::content { "Deactivated ${draft()?.deactivatedAt}" }
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
    }

    private fun ElementWriter.CanAddTheme.firstAdminSection() = card.col {
        h3("Provision first ClinicAdmin")
        subtext("Step 2: invite the clinic's first administrator. They will receive an activation email.")

        field("Email") {
            textInput {
                keyboardHints = KeyboardHints.email
                content bind firstAdminEmail
            }
        }
        field("First name (optional)") {
            textInput {
                keyboardHints = KeyboardHints.title
                content bind firstAdminFirstName
            }
        }
        field("Last name (optional)") {
            textInput {
                keyboardHints = KeyboardHints.title
                content bind firstAdminLastName
            }
        }
        row {
            button {
                text("Skip")
                onClick {
                    firstAdminOpen.value = false
                    newClinicMode.value = false
                }
            }
            atEnd.important.button {
                text("Send invite")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val me = session.self()
                    val emailRaw = firstAdminEmail.value.trim()
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
                            firstName = firstAdminFirstName.value.trim(),
                            lastName = firstAdminLastName.value.trim(),
                        )
                    )
                    session.clinicMemberships.add(
                        ClinicMembership(
                            clinic = id,
                            user = targetUser._id,
                            role = ClinicRole.ClinicAdmin,
                            invitedBy = me._id,
                        )
                    )
                    firstAdminOpen.value = false
                    newClinicMode.value = false
                    firstAdminEmail.value = ""
                    firstAdminFirstName.value = ""
                    firstAdminLastName.value = ""
                    context.toast("Invite sent")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.membershipsSection() = card.col {
        h3("Memberships")
        subtext("Ops sees every membership — active and inactive.")

        col {
            reactive {
                clearChildren()
                val list = memberships()
                if (list.isEmpty()) {
                    subtext("No memberships yet.")
                } else {
                    list.forEach { membership ->
                        card.col { memberRow(membership) }
                    }
                }
            }
        }

        col { inviteForm() }
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
                row {
                    subtext { ::content { "Invited ${membership.invitedAt}" } }
                    subtext {
                        ::content {
                            membership.acceptedAt?.let { "Accepted $it" } ?: "Pending acceptance"
                        }
                    }
                }
                shownWhen { membership.deactivatedAt != null }.subtext {
                    ::content { "Deactivated ${membership.deactivatedAt}" }
                }
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
                    shownWhen { pendingRole() != membership.role && membership.deactivatedAt == null }.button {
                        text("Apply")
                        onClick {
                            val session = currentSession() ?: return@onClick
                            session.clinicMemberships[membership._id].set(
                                membership.copy(role = pendingRole.value),
                            )
                            context.toast("Role updated")
                        }
                    }
                    shownWhen { membership.deactivatedAt == null }.button {
                        text("Deactivate")
                        onClick {
                            val session = currentSession() ?: return@onClick
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

    private fun ElementWriter.CanAddTheme.inviteForm() = card.col {
        h4("Invite member")
        val email = Signal("")
        val firstName = Signal("")
        val lastName = Signal("")
        val role = Signal<ClinicRole>(ClinicRole.MedicalAssistant)

        field("Email") {
            textInput {
                keyboardHints = KeyboardHints.email
                content bind email
            }
        }
        row {
            expanding.field("First name (optional)") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind firstName
                }
            }
            expanding.field("Last name (optional)") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind lastName
                }
            }
        }
        field("Role") {
            select {
                bind(
                    edits = role,
                    data = Constant(ClinicRole.entries.toList()),
                    render = { it.name },
                )
            }
        }
        atEnd.important.button {
            text("Send invite")
            onClick {
                val session = currentSession() ?: return@onClick
                val me = session.self()
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
                        firstName = firstName.value.trim(),
                        lastName = lastName.value.trim(),
                    )
                )
                session.clinicMemberships.add(
                    ClinicMembership(
                        clinic = id,
                        user = targetUser._id,
                        role = role.value,
                        invitedBy = me._id,
                    )
                )
                email.value = ""
                firstName.value = ""
                lastName.value = ""
                role.value = ClinicRole.MedicalAssistant
                context.toast("Invite sent")
            }
        }
    }

    private fun ElementWriter.CanAddTheme.sublistsSection() = col {
        card.col {
            h3("Users")
            col {
                reactive {
                    clearChildren()
                    val session = currentSession() ?: return@reactive
                    val ids = activeMemberUserIds()
                    if (ids.isEmpty()) {
                        subtext("No active users in this clinic.")
                    } else {
                        ids.forEach { uid ->
                            val membership = memberships().firstOrNull { it.user == uid && it.isActive }
                            row {
                                expanding.col {
                                    text {
                                        ::content {
                                            session.users[uid].invoke()?.displayName ?: uid.toString()
                                        }
                                    }
                                    subtext {
                                        ::content {
                                            session.users[uid].invoke()?.email?.raw ?: ""
                                        }
                                    }
                                }
                                subtext(membership?.role?.name ?: "")
                            }
                        }
                    }
                }
            }
        }

        card.col {
            val patientCount = rememberSuspending {
                val session = currentSession() ?: return@rememberSuspending 0
                session.patients.skipCache.count(
                    condition<Patient> { it.clinic eq id }
                )
            }
            row {
                expanding.col {
                    h3("Patients")
                    subtext { ::content { "${patientCount()} patients" } }
                }
                button {
                    text("Open")
                    // TODO: PatientListPage does not yet accept a clinic-id filter; the destination
                    // will show whatever is in the operator's active clinic.
                    onClick { context.pageNavigator.navigate(PatientListPage()) }
                }
            }
        }

        card.col {
            val orderCount = rememberSuspending {
                val session = currentSession() ?: return@rememberSuspending 0
                session.prescriptionOrders.skipCache.count(
                    condition<PrescriptionOrder> { it.clinic eq id }
                )
            }
            row {
                expanding.col {
                    h3("Orders")
                    subtext { ::content { "${orderCount()} orders" } }
                }
                button {
                    text("Open")
                    // TODO: OrdersListPage scopes to the operator's active clinic; no clinic-id arg yet.
                    onClick { context.pageNavigator.navigate(OrdersListPage()) }
                }
            }
        }

        card.col {
            val invoiceCount = rememberSuspending {
                val session = currentSession() ?: return@rememberSuspending 0
                session.clinicInvoices.skipCache.count(
                    condition<ClinicInvoice> { it.clinic eq id }
                )
            }
            row {
                expanding.col {
                    h3("Invoices")
                    subtext { ::content { "${invoiceCount()} invoices" } }
                }
                button {
                    text("Open")
                    // TODO: InvoiceListPage is a stub; clinic filter will be added when the page is built.
                    onClick { context.pageNavigator.navigate(InvoiceListPage()) }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.actionsSection() = card.row {
        val confirmingDeactivate = Signal(false)
        val confirmingReactivate = Signal(false)

        shownWhen { draft()?.isActive == true && !confirmingDeactivate() }.button {
            text("Deactivate clinic")
            onClick { confirmingDeactivate.value = true }
        }
        shownWhen { confirmingDeactivate() }.row {
            subtext("Confirm deactivation?")
            button {
                text("Cancel")
                onClick { confirmingDeactivate.value = false }
            }
            important.button {
                text("Deactivate")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val d = draft.value ?: return@onClick
                    val updated = d.copy(deactivatedAt = Clock.System.now())
                    session.clinics[d._id].set(updated)
                    draft.value = updated
                    confirmingDeactivate.value = false
                    context.toast("Clinic deactivated")
                }
            }
        }

        shownWhen { draft()?.isActive == false && !confirmingReactivate() }.button {
            text("Reactivate clinic")
            onClick { confirmingReactivate.value = true }
        }
        shownWhen { confirmingReactivate() }.row {
            subtext("Confirm reactivation?")
            button {
                text("Cancel")
                onClick { confirmingReactivate.value = false }
            }
            important.button {
                text("Reactivate")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val d = draft.value ?: return@onClick
                    val updated = d.copy(deactivatedAt = null)
                    session.clinics[d._id].set(updated)
                    draft.value = updated
                    confirmingReactivate.value = false
                    context.toast("Clinic reactivated")
                }
            }
        }
    }

    private fun ViewWriter.actionRow() = row {
        atEnd.shownWhen { editMode() }.important.button {
            text { ::content { if (newClinicMode()) "Create clinic" else "Save" } }
            onClick {
                val d = draft.value ?: return@onClick
                val problems = validate(d)
                if (problems.isNotEmpty()) {
                    context.toast("Please fix: ${problems.joinToString(", ")}")
                    return@onClick
                }
                val session = currentSession() ?: return@onClick
                if (newClinicMode.value) {
                    session.clinics.add(d)
                    draft.value = d
                    editMode.value = false
                    firstAdminOpen.value = true
                    context.toast("Clinic created — invite the first admin")
                } else {
                    session.clinics[d._id].set(d)
                    draft.value = d
                    editMode.value = false
                    context.toast("Saved")
                }
            }
        }
    }

    private fun validate(c: Clinic): List<String> = buildList {
        if (c.name.isBlank()) add("clinic name")
        if (c.billingContactName.isBlank()) add("billing contact name")
        val email = c.billingContactEmail.raw
        if (email.isBlank() || email == "placeholder@example.com" || !email.contains('@')) {
            add("billing contact email")
        }
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
