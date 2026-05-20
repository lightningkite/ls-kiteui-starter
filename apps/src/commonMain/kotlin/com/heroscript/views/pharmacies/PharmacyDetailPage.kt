package com.heroscript.views.pharmacies

import com.heroscript.*
import com.heroscript.sdk.currentSession
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
import com.lightningkite.services.data.toPhoneNumber
import com.lightningkite.services.database.*
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

@Routable("ops/pharmacies/{id}")
class PharmacyDetailPage(
    val id: Pharmacy.ID,
    val startInEditMode: Boolean = false,
) : PageWithParent {
    override val title: Reactive<String> get() = Constant("Pharmacy")
    override var parentPage: Page = PharmacyListPage()

    private val editMode = Signal(startInEditMode)
    private val draft = Signal<Pharmacy?>(null)

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val loaded = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        session.pharmacies[id].invoke()
    }

    private val mappings = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        session.productPharmacyMappings.query(
            Query(condition<ProductPharmacyMapping> { it.pharmacy eq id })
        )()
    }

    private val orders = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        session.pharmacyOrders.query(
            Query(
                condition = condition<PharmacyOrder> { it.pharmacy eq id },
                orderBy = sort<PharmacyOrder> { it.createdAt.descending() },
                limit = 20,
            )
        )()
    }

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            val current = loaded()
            if (current != null && draft.value == null) {
                draft.value = current
            } else if (draft.value == null && startInEditMode) {
                draft.value = Pharmacy(
                    _id = id,
                    name = "",
                    adapterType = PharmacyAdapterType.LifeFile,
                    credentialsSecretRef = "",
                    contactEmail = "placeholder@example.com".toEmailAddress(),
                )
            }
        }

        scrolling.col {
            shownWhen { !isOps() }.padded.col {
                centered.text("Ops access required.")
            }

            shownWhen { isOps() && draft() == null }.padded.col { centered.text("Loading...") }

            shownWhen { isOps() && draft() != null }.col {
                generalSection()
                statesSection()
                mappingsSection()
                ordersSection()
                actionsSection()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.generalSection() = card.col {
        row {
            expanding.h2 {
                ::content {
                    val d = draft()
                    when {
                        d == null -> "Pharmacy"
                        d.name.isBlank() -> "New pharmacy"
                        else -> d.name
                    }
                }
            }
            shownWhen { draft()?.isActive == false }.subtext("Inactive")
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
            field("Name") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind pharmacyField("", { it.name }, { d, v -> d.copy(name = v) })
                }
            }
            field("Adapter type") {
                select {
                    bind(
                        edits = pharmacyField(
                            PharmacyAdapterType.LifeFile,
                            { it.adapterType },
                            { d, v -> d.copy(adapterType = v) },
                        ),
                        data = Constant(PharmacyAdapterType.entries.toList()),
                        render = { it.name },
                    )
                }
            }
            field("Credentials secret ref") {
                textInput {
                    content bind pharmacyField(
                        "",
                        { it.credentialsSecretRef },
                        { d, v -> d.copy(credentialsSecretRef = v) },
                    )
                }
            }
            subtext("Pointer to AWS Secrets Manager. Update only with a new Secrets Manager ARN.")
            field("Contact email") {
                textInput {
                    keyboardHints = KeyboardHints.email
                    content bind pharmacyField(
                        "",
                        { it.contactEmail.raw },
                        { d, v ->
                            val parsed = runCatching { v.toEmailAddress() }.getOrNull()
                            if (parsed == null) d else d.copy(contactEmail = parsed)
                        },
                    )
                }
            }
            field("Contact phone") {
                textInput {
                    keyboardHints = KeyboardHints.phone
                    content bind pharmacyField(
                        "",
                        { it.contactPhone?.raw ?: "" },
                        { d, v -> d.copy(contactPhone = v.takeIf { it.isNotBlank() }?.toPhoneNumber()) },
                    )
                }
            }
        }

        shownWhen { !editMode() }.col {
            row {
                subtext("Adapter")
                text { ::content { draft()?.adapterType?.name ?: "—" } }
            }
            row {
                subtext("Contact")
                text { ::content { draft()?.contactEmail?.raw ?: "—" } }
            }
            shownWhen { draft()?.contactPhone != null }.row {
                subtext("Phone")
                text { ::content { draft()?.contactPhone?.raw ?: "" } }
            }
            row {
                subtext("Created")
                text { ::content { draft()?.createdAt?.toString() ?: "—" } }
            }
            shownWhen { draft()?.deactivatedAt != null }.row {
                subtext("Deactivated")
                text { ::content { draft()?.deactivatedAt?.toString() ?: "" } }
            }
            col {
                subtext("Credentials secret ref (pointer to AWS Secrets Manager)")
                fieldTheme.text { ::content { draft()?.credentialsSecretRef?.ifBlank { "—" } ?: "—" } }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.statesSection() = card.col {
        row {
            expanding.h3("State licensing")
            shownWhen { editMode() }.button {
                icon(Icon.add, "Add state")
                onClick {
                    val d = draft.value ?: return@onClick
                    draft.value = d.copy(states = d.states + Pharmacy.StateInfo(state = ""))
                }
            }
        }

        reactive {
            val list = draft()?.states?.toList().orEmpty()
            if (list.isEmpty()) {
                subtext("No state licensing configured.")
            }
        }

        col {
            reactive {
                clearChildren()
                val list = draft()?.states?.toList().orEmpty()
                list.forEachIndexed { index, entry ->
                    card.col {
                        if (editMode()) stateEditor(index, entry)
                        else stateReadOnly(entry)
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.stateReadOnly(entry: Pharmacy.StateInfo) = col {
        row {
            expanding.h4 { ::content { entry.state.uppercase().ifBlank { "—" } } }
            entry.effectiveDate?.let { subtext("Effective $it") }
            entry.expirationDate?.let { subtext("Expires $it") }
        }
        entry.notes?.takeIf { it.isNotBlank() }?.let { subtext(it) }
    }

    private fun ElementWriter.CanAddTheme.stateEditor(index: Int, entry: Pharmacy.StateInfo) = col {
        val state = Signal(entry.state)
        val effective = Signal<LocalDate?>(entry.effectiveDate)
        val expiration = Signal<LocalDate?>(entry.expirationDate)
        val notes = Signal(entry.notes ?: "")

        reactive {
            val current = draft.value ?: return@reactive
            val updated = Pharmacy.StateInfo(
                state = state().trim().uppercase(),
                effectiveDate = effective(),
                expirationDate = expiration(),
                notes = notes().takeIf { it.isNotBlank() },
            )
            val list = current.states.toMutableList()
            if (index in list.indices && list[index] != updated) {
                list[index] = updated
                draft.value = current.copy(states = list.toSet())
            }
        }

        field("State (2-letter)") {
            textInput {
                keyboardHints = KeyboardHints.title
                content bind state
            }
        }
        field("Effective date") {
            localDateField { content bind effective }
        }
        field("Expiration date") {
            localDateField { content bind expiration }
        }
        field("Notes") {
            textInput { content bind notes }
        }
        atEnd.button {
            text("Remove")
            onClick {
                val current = draft.value ?: return@onClick
                val list = current.states.toMutableList()
                if (index in list.indices) list.removeAt(index)
                draft.value = current.copy(states = list.toSet())
            }
        }
    }

    private fun ElementWriter.CanAddTheme.mappingsSection() = card.col {
        h3("Catalog mappings")
        subtext("Pharmacy mappings are edited from the Catalog Ops screens.")

        reactive {
            val list = mappings()
            if (list.isEmpty()) subtext("This pharmacy carries no products yet.")
        }

        col {
            reactive {
                clearChildren()
                val list = mappings()
                if (list.isEmpty()) return@reactive
                list.forEach { mapping ->
                    card.col {
                        val productName = rememberSuspending {
                            val session = currentSession() ?: return@rememberSuspending ""
                            session.products[mapping.product].invoke()?.name ?: ""
                        }
                        row {
                            expanding.h4 { ::content { productName().ifBlank { mapping.product.toString().take(8) } } }
                            shownWhen { !mapping.active }.subtext("Inactive")
                        }
                        row {
                            subtext("Form: ${mapping.form.label()}")
                            subtext("Strength: ${mapping.strength?.toString() ?: "Customizable"}")
                            subtext("Quantity: ${mapping.quantity?.toString() ?: "Customizable"}")
                        }
                        subtext("SKU: ${mapping.pharmacySku}")
                        row {
                            subtext("Price: ${centsToString(mapping.price)}")
                            subtext("Shipping: ${centsToString(mapping.shippingFee)}")
                            subtext("Total: ${centsToString(mapping.total)}")
                        }
                        subtext("Lead time: ${mapping.leadTimeDays} days")
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.ordersSection() = card.col {
        h3("Recent orders")

        reactive {
            val list = orders()
            if (list.isEmpty()) subtext("No pharmacy orders yet.")
        }

        col {
            reactive {
                clearChildren()
                val list = orders()
                if (list.isEmpty()) return@reactive
                list.forEach { po ->
                    card.col {
                        val clinicName = rememberSuspending {
                            val session = currentSession() ?: return@rememberSuspending ""
                            session.clinics[po.clinic].invoke()?.name ?: ""
                        }
                        row {
                            expanding.h4 { ::content { clinicName().ifBlank { po.clinic.toString().take(8) } } }
                            subtext { ::content { pharmacyOrderStatus(po) } }
                        }
                        subtext("Created ${po.createdAt}")
                        po.accepted?.let { subtext("Accepted · external ${it.externalId}") }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.actionsSection() = card.col {
        h3("Actions")
        row {
            button {
                text("Test connection")
                onClick {
                    context.toast("Test connection coming soon")
                }
            }
            button {
                text {
                    ::content { if (draft()?.isActive == true) "Deactivate" else "Activate" }
                }
                onClick {
                    val session = currentSession() ?: return@onClick
                    val d = draft.value ?: return@onClick
                    val updated = if (d.isActive) d.copy(deactivatedAt = Clock.System.now())
                    else d.copy(deactivatedAt = null)
                    session.pharmacies[id].set(updated)
                    draft.value = updated
                    context.toast(if (updated.isActive) "Activated" else "Deactivated")
                }
            }
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
                    session.pharmacies[id].set(d)
                    draft.value = d
                    editMode.value = false
                    context.toast("Saved")
                }
            }
        }
    }

    private fun validate(p: Pharmacy): List<String> = buildList {
        if (p.name.isBlank()) add("name")
        if (p.credentialsSecretRef.isBlank()) add("credentials secret ref")
        if (p.contactEmail.raw.isBlank()) add("contact email")
        if (runCatching { p.contactEmail.raw.toEmailAddress() }.getOrNull() == null) add("valid contact email")
        p.states.forEachIndexed { i, s ->
            if (s.state.length != 2 || s.state.any { !it.isLetter() }) add("state row ${i + 1} (2-letter code)")
        }
    }

    private fun <V> pharmacyField(
        default: V,
        get: (Pharmacy) -> V,
        set: (Pharmacy, V) -> Pharmacy,
    ): MutableReactive<V> =
        draft.lens(
            get = { current -> current?.let(get) ?: default },
            modify = { current, v -> current?.let { set(it, v) } },
        )
}

private fun centsToString(c: Int): String {
    val dollars = c / 100
    val remainder = (if (c < 0) -c else c) % 100
    val sign = if (c < 0) "-" else ""
    return "$sign\$$dollars.${remainder.toString().padStart(2, '0')}"
}

private fun pharmacyOrderStatus(po: PharmacyOrder): String = when {
    po.totalRejection != null -> "Rejected"
    po.accepted != null -> "Accepted"
    else -> "Pending"
}

private fun Product.FormType.label(): String = when (this) {
    Product.FormType.InjectableVial -> "Injectable vial"
    Product.FormType.InjectableSyringe -> "Injectable syringe"
    Product.FormType.OralTablet -> "Oral tablet"
    Product.FormType.OralCapsule -> "Oral capsule"
    Product.FormType.OralSolution -> "Oral solution"
    Product.FormType.TopicalCream -> "Topical cream"
    Product.FormType.TopicalGel -> "Topical gel"
    Product.FormType.Troche -> "Troche"
    Product.FormType.Other -> "Other"
}
