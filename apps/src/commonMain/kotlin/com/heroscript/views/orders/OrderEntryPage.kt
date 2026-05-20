package com.heroscript.views.orders

import com.heroscript.*
import com.heroscript.sdk.UserSession
import com.heroscript.sdk.currentSession
import com.heroscript.views.catalog.label
import com.heroscript.views.components.patientPicker
import com.heroscript.views.components.productPicker
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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Central clinical workflow. Two modes, same page:
 *  - New-Rx mode (prescriptionId == null): composer is fully editable, save writes both a
 *    Prescription and a PrescriptionOrder.
 *  - Refill mode (prescriptionId != null): composer is read-only summary; only pharmacy,
 *    quantity, ship-to, and willLastDays are editable.
 */
@Routable("orders/new")
class OrderEntryPage(
    val prescriptionId: Prescription.ID? = null,
    val patientId: Patient.ID? = null,
) : PageWithParent {
    override val title: Reactive<String> get() = Constant("New order")
    override var parentPage: Page = OrdersListPage()

    private val isRefill: Boolean get() = prescriptionId != null

    /* ---- Working draft state ---- */

    private val patient = Signal<Patient?>(null)
    private val product = Signal<Product?>(null)
    private val formType = Signal<Product.FormType?>(null)
    private val strength = Signal("")
    private val sig = Signal("")
    private val prescriberId = Signal<User.ID?>(null)
    private val endsAtText = Signal("")

    private val selectedMapping = Signal<ProductPharmacyMapping?>(null)
    private val customStrength = Signal("")
    private val customQuantity = Signal("")
    private val willLastDays = Signal("")

    private val shipToOption = Signal(ShipToOption.ClinicPrimary)
    private val selectedClinicAddressIdx = Signal(0)

    private val smsConsentAffirmed = Signal(false)
    private val emailConsentAffirmed = Signal(false)

    private val submitting = Signal(false)

    /* ---- Loaded reference data ---- */

    private val clinic = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        val cid = activeClinic() ?: return@rememberSuspending null
        session.clinics[cid].invoke()
    }

    private val me = rememberSuspending {
        currentSession()?.self?.invoke()
    }

    private val membershipsInClinic = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val cid = activeClinic() ?: return@rememberSuspending emptyList()
        session.clinicMemberships.query(
            Query(condition<ClinicMembership> {
                (it.clinic eq cid) and (it.acceptedAt neq null) and (it.deactivatedAt eq null)
            })
        )()
    }

    private val prescribers = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList<User>()
        membershipsInClinic()
            .filter { it.role == ClinicRole.Prescriber }
            .mapNotNull { m -> session.users[m.user].invoke() }
            .filter { it.prescriber != null }
    }

    private val selectedPrescriber = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        prescriberId()?.let { session.users[it].invoke() }
    }

    private val refillPrescription = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        prescriptionId?.let { session.prescriptions[it].invoke() }
    }

    private val refillProduct = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        refillPrescription()?.let { session.products[it.product].invoke() }
    }

    /** Active mappings for the selected (product, formType). */
    private val candidateMappings = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val pid = product()?._id ?: return@rememberSuspending emptyList()
        val ft = formType() ?: return@rememberSuspending emptyList()
        session.productPharmacyMappings.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<ProductPharmacyMapping> { it.product eq pid },
                        condition<ProductPharmacyMapping> { it.form eq ft },
                        condition<ProductPharmacyMapping> { it.active eq true },
                    )
                )
            )
        )()
    }

    private val destination = remember {
        val c = clinic()
        val p = patient()
        when (shipToOption()) {
            ShipToOption.ClinicPrimary -> c?.primaryAddress
            ShipToOption.ClinicAdditional -> {
                val idx = selectedClinicAddressIdx()
                c?.additionalShippingAddresses?.getOrNull(idx)
            }
            ShipToOption.Patient -> p?.shippingAddress
        }
    }

    /** Pharmacies eligible for the chosen ship-to state, deduped from candidate mappings. */
    private val eligiblePharmacies = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val dest = destination() ?: return@rememberSuspending emptyList()
        val state = dest.address.state.takeIf { it.length == 2 } ?: return@rememberSuspending emptyList()
        val mappings = candidateMappings()
        val byPharmacy = mappings.groupBy { it.pharmacy }

        byPharmacy.keys.mapNotNull { id -> session.pharmacies[id].invoke() }
            .filter { ph ->
                ph.isActive && ph.states.any { it.state == state }
            }
            .map { ph -> ph to (byPharmacy[ph._id] ?: emptyList()).filter { it.active } }
            .filter { (_, rows) -> rows.isNotEmpty() }
            .sortedBy { (_, rows) -> rows.minOf { it.total } }
    }

    private val recentSigs = rememberSuspending {
        if (isRefill) return@rememberSuspending emptyList()
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val pid = product()?._id ?: return@rememberSuspending emptyList()
        val by = prescriberId() ?: return@rememberSuspending emptyList()
        session.prescriptions.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<Prescription> { it.product eq pid },
                        condition<Prescription> { it.prescribedBy eq by },
                    )
                ),
                orderBy = sort<Prescription> { it.createdAt.descending() },
                limit = 5,
            )
        )()
    }

    /* ---- Effective values after edits / customizations ---- */

    private val effectiveStrength = remember {
        if (isRefill) refillPrescription()?.strength ?: 0.0
        else strength().toDoubleOrNull() ?: 0.0
    }

    private val effectiveFormType = remember {
        if (isRefill) refillPrescription()?.form
        else formType()
    }

    private val effectiveQuantity = remember {
        val m = selectedMapping() ?: return@remember 0.0
        m.quantity ?: customQuantity().toDoubleOrNull() ?: 0.0
    }

    private val effectiveProduct = remember {
        if (isRefill) refillProduct() else product()
    }

    /* ---- Validation ---- */

    private val blockers = remember {
        val list = buildList {
            if (patient() == null) add("Select a patient")
            if (effectiveProduct() == null) add("Select a product")
            if (effectiveFormType() == null) add("Select a form")
            if (effectiveStrength() <= 0) add("Enter a strength")
            val theSig = if (isRefill) refillPrescription()?.instructions ?: "" else sig()
            if (theSig.isBlank()) add("Enter a sig")
            if (prescriberId() == null) add("Pick a prescriber")
            if (destination() == null) add("Pick a ship-to address")
            if (destination()?.verifiedAt == null) add("Ship-to address must be verified")
            if (selectedMapping() == null) add("Pick a pharmacy")
            if (effectiveQuantity() <= 0) add("Set a quantity")
            if (willLastDays().toIntOrNull()?.let { it <= 0 } != false) add("Enter days the supply will last")
            if (!smsConsentAffirmed() && !emailConsentAffirmed()) add("Affirm at least one consent")
            val pre = selectedPrescriber()?.prescriber
            if (pre?.isDeaExpired == true) add("Prescriber DEA expired")
            if (effectiveProduct()?.controlled == true && pre?.canSubmitControlledSubstance != true) {
                add("Prescriber cannot submit controlled substances")
            }
        }
        list
    }

    private val canSubmitAsMe = remember {
        val u = me() ?: return@remember false
        val pid = prescriberId() ?: return@remember false
        u._id == pid
    }

    override fun ElementWriter.CanAddTheme.render() {
        seedFromRefill()
        seedFromPatientParam()
        seedDefaults()

        scrolling.col {
            shownWhen { activeClinic() == null }.card.col {
                h3("No active clinic")
                text("Orders are scoped to a clinic. Accept a clinic membership invite to create orders.")
            }

            shownWhen { activeClinic() != null }.col {
                refillBanner()
                patientSection()
                shipToSection()
                consentSection()
                prescriptionSection()
                pharmacySection()
                quantityDurationSection()
                summarySection()
                validationBanner()
                actionsSection()
            }
        }
    }

    /* ---- Seeding ---- */

    private fun ElementWriter.CanAddTheme.seedFromRefill() {
        if (!isRefill) return
        reactive {
            val rx = refillPrescription() ?: return@reactive
            val session = currentSession() ?: return@reactive
            if (patient.value == null) {
                patient.value = session.patients[rx.patient].invoke()
            }
            if (product.value == null) {
                product.value = session.products[rx.product].invoke()
            }
            if (formType.value == null) formType.value = rx.form
            if (strength.value.isBlank()) strength.value = rx.strength.toString()
            if (sig.value.isBlank()) sig.value = rx.instructions
            if (prescriberId.value == null) prescriberId.value = rx.prescribedBy
            if (endsAtText.value.isBlank()) endsAtText.value = rx.endsAt?.toString() ?: ""
        }
    }

    private fun ElementWriter.CanAddTheme.seedFromPatientParam() {
        if (patientId == null) return
        reactive {
            val session = currentSession() ?: return@reactive
            if (patient.value == null) {
                patient.value = session.patients[patientId].invoke()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.seedDefaults() {
        reactive {
            // Default prescriber to current user when they are a Prescriber in this clinic.
            val u = me() ?: return@reactive
            if (prescriberId.value == null && u.prescriber != null) {
                val cid = activeClinic() ?: return@reactive
                val memberships = membershipsInClinic()
                if (memberships.any { it.user == u._id && it.role == ClinicRole.Prescriber }) {
                    prescriberId.value = u._id
                }
            }
        }
        reactive {
            // Default ship-to to patient address if patient is loaded and clinic primary address is empty,
            // else clinic primary. Run only once: only when shipToOption is its initial default and a clinic exists.
            // Default initial is ClinicPrimary so this is a no-op except where we need to refresh selection.
            // (No imperative seeding needed beyond initial value.)
        }
        reactive {
            // willLastDays default: 28 for injectables, blank otherwise. Recompute when formType changes
            // unless the user has already typed something.
            val ft = effectiveFormType() ?: return@reactive
            if (willLastDays.value.isBlank()) {
                if (ft == Product.FormType.InjectableVial || ft == Product.FormType.InjectableSyringe) {
                    willLastDays.value = "28"
                }
            }
        }
        reactive {
            // Pre-check consent reaffirmation when the patient already has it on file.
            val p = patient() ?: return@reactive
            if (p.smsConsent != null && !smsConsentAffirmed.value) smsConsentAffirmed.value = true
            if (p.emailConsent != null && !emailConsentAffirmed.value) emailConsentAffirmed.value = true
        }
        reactive {
            // Clear the selected mapping when product/form changes — its (form, product) no longer applies.
            val pid = product()?._id
            val ft = formType()
            val m = selectedMapping.value ?: return@reactive
            if (m.product != pid || m.form != ft) selectedMapping.value = null
        }
    }

    /* ---- UI sections ---- */

    private fun ElementWriter.CanAddTheme.refillBanner() = col {
        shownWhen { isRefill }.card.col {
            h4("Refill against existing prescription")
            subtext("Patient, product, form, strength, sig, and prescriber are locked from the original Rx.")
        }
    }

    private fun ElementWriter.CanAddTheme.patientSection() = card.col {
        h3("Patient")
        if (isRefill) {
            reactive {
                clearChildren()
                val p = patient() ?: return@reactive
                col {
                    text(p.displayName)
                    subtext("DOB ${p.dateOfBirth} · ${p.gender}")
                    text(p.shippingAddress.address.recipient)
                    text(p.shippingAddress.address.line1)
                    text("${p.shippingAddress.address.city}, ${p.shippingAddress.address.state} ${p.shippingAddress.address.zip}")
                    subtext(if (p.shippingAddress.verifiedAt != null) "Verified" else "Unverified")
                }
            }
        } else {
            patientPicker(patient)
        }
    }

    private fun ElementWriter.CanAddTheme.shipToSection() = card.col {
        h3("Ship to")

        col {
            reactive {
                clearChildren()
                val c = clinic()
                val p = patient()

                row {
                    card.button {
                        ::enabled { true }
                        row {
                            centered.text {
                                ::content { if (shipToOption() == ShipToOption.ClinicPrimary) "● Clinic primary" else "○ Clinic primary" }
                            }
                        }
                        onClick { shipToOption.value = ShipToOption.ClinicPrimary }
                    }
                    if ((c?.additionalShippingAddresses?.size ?: 0) > 0) {
                        card.button {
                            row {
                                centered.text {
                                    ::content { if (shipToOption() == ShipToOption.ClinicAdditional) "● Other clinic address" else "○ Other clinic address" }
                                }
                            }
                            onClick { shipToOption.value = ShipToOption.ClinicAdditional }
                        }
                    }
                    if (p != null) {
                        card.button {
                            row {
                                centered.text {
                                    ::content { if (shipToOption() == ShipToOption.Patient) "● Patient address" else "○ Patient address" }
                                }
                            }
                            onClick { shipToOption.value = ShipToOption.Patient }
                        }
                    }
                }

                shownWhen {
                    shipToOption() == ShipToOption.ClinicAdditional &&
                        (clinic()?.additionalShippingAddresses?.size ?: 0) > 1
                }.col {
                    subtext("Select address")
                    reactive {
                        clearChildren()
                        val list = clinic()?.additionalShippingAddresses ?: return@reactive
                        list.forEachIndexed { idx, addr ->
                            card.button {
                                col {
                                    text {
                                        ::content {
                                            val prefix = if (selectedClinicAddressIdx() == idx) "● " else "○ "
                                            prefix + (addr.address.recipient.ifBlank { "Address ${idx + 1}" })
                                        }
                                    }
                                    subtext("${addr.address.line1}, ${addr.address.city}, ${addr.address.state}")
                                }
                                onClick { selectedClinicAddressIdx.value = idx }
                            }
                        }
                    }
                }
            }
        }

        col {
            reactive {
                clearChildren()
                val d = destination()
                if (d == null) {
                    subtext("No address available for this option.")
                } else {
                    col {
                        text(d.address.recipient.ifBlank { "(no recipient)" })
                        text(d.address.line1)
                        d.address.line2?.let { text(it) }
                        text("${d.address.city}, ${d.address.state} ${d.address.zip}")
                        subtext(if (d.verifiedAt != null) "Verified (${d.verificationProvider ?: "manual"})" else "Unverified")
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.consentSection() = card.col {
        h3("Consent reaffirmation")
        subtext("Reaffirm patient consent for this order. At least one channel required.")
        row {
            checkbox { checked bind smsConsentAffirmed }
            centered.text("SMS")
        }
        row {
            checkbox { checked bind emailConsentAffirmed }
            centered.text("Email")
        }
    }

    private fun ElementWriter.CanAddTheme.prescriptionSection() = card.col {
        h3("Prescription")

        if (isRefill) {
            reactive {
                clearChildren()
                val rx = refillPrescription() ?: return@reactive
                val prod = refillProduct()
                col {
                    row {
                        subtext("Product")
                        text(prod?.name ?: "—")
                    }
                    row {
                        subtext("Form")
                        text(rx.form.label())
                    }
                    row {
                        subtext("Strength")
                        text {
                            val unit = prod?.forms?.firstOrNull { it.form == rx.form }?.strengthUnit
                            content = if (unit != null) "${rx.strength} $unit" else rx.strength.toString()
                        }
                    }
                    row {
                        subtext("Sig")
                        text(rx.instructions)
                    }
                    rx.endsAt?.let {
                        row {
                            subtext("Expires")
                            text(it.toString())
                        }
                    }
                }
            }
        } else {
            productPicker(product)

            shownWhen { product() != null }.col {
                subtext("Form")
                reactive {
                    clearChildren()
                    val forms = product()?.forms?.toList()?.sortedBy { it.form.ordinal } ?: return@reactive
                    if (forms.isEmpty()) {
                        subtext("This product has no forms configured.")
                    }
                    forms.forEach { f ->
                        card.button {
                            row {
                                expanding.col {
                                    text {
                                        ::content {
                                            val prefix = if (formType() == f.form) "● " else "○ "
                                            prefix + f.form.label()
                                        }
                                    }
                                    subtext("${f.strengthUnit} · ${f.quantityUnit}")
                                }
                            }
                            onClick { formType.value = f.form }
                        }
                    }
                }
            }

            shownWhen { formType() != null }.field("Strength") {
                row {
                    expanding.textInput {
                        keyboardHints = KeyboardHints.decimal
                        content bind strength
                    }
                    centered.subtext {
                        ::content {
                            val ft = formType()
                            if (ft == null) "" else product()?.forms?.firstOrNull { it.form == ft }?.strengthUnit ?: ""
                        }
                    }
                }
            }

            shownWhen { product() != null && prescriberId() != null }.col {
                subtext("Recent sigs from this prescriber")
                col {
                    reactive {
                        clearChildren()
                        val list = recentSigs()
                        if (list.isEmpty()) {
                            subtext("No prior sigs for this product yet.")
                        } else {
                            list.forEach { rx ->
                                card.button {
                                    col {
                                        text(rx.instructions)
                                        subtext("Used ${rx.createdAt}")
                                    }
                                    onClick { sig.value = rx.instructions }
                                }
                            }
                        }
                    }
                }
            }

            field("Sig (instructions)") {
                textArea {
                    hint = "e.g. 0.5 mL IM weekly"
                    content bind sig
                }
            }

            subtext("Prescriber")
            col {
                reactive {
                    clearChildren()
                    val list = prescribers()
                    if (list.isEmpty()) {
                        subtext("No prescribers available in this clinic.")
                    }
                    list.forEach { p ->
                        card.button {
                            text {
                                ::content {
                                    val prefix = if (prescriberId() == p._id) "● " else "○ "
                                    prefix + p.displayName
                                }
                            }
                            onClick { prescriberId.value = p._id }
                        }
                    }
                }
            }

            field("Expiration (ISO instant — optional)") {
                textInput {
                    hint = "Leave blank for open-ended"
                    content bind endsAtText
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.pharmacySection() = card.col {
        h3("Pharmacy")
        subtext("Cards sorted by total price. Desktop side-by-side matrix is a follow-up.")
        // TODO: render a desktop matrix view (rows = pharmacies, cols = mappings) when on wide viewports.

        reactive {
            // Clear so a stale "Pick a product first" doesn't linger after the precondition is met.
            clearChildren()
            val pickedProduct = effectiveProduct()
            val pickedForm = effectiveFormType()
            val dest = destination()
            when {
                pickedProduct == null -> subtext("Pick a product first.")
                pickedForm == null -> subtext("Pick a form first.")
                dest == null -> subtext("Pick a ship-to address first.")
                eligiblePharmacies().isEmpty() ->
                    subtext("No pharmacies licensed to ship to ${dest.address.state} with this product/form.")
            }
        }

        col {
            reactive {
                clearChildren()
                val target = effectiveStrength()
                eligiblePharmacies().forEach { (pharmacy, rows) ->
                    card.col {
                        h4(pharmacy.name)
                        rows.sortedBy { it.total }.forEach { mapping ->
                            val strengthMatches =
                                mapping.strength == null || mapping.strength == target
                            card.button {
                                col {
                                    row {
                                        expanding.text {
                                            content = when {
                                                mapping.strength == null -> "Strength: Customizable"
                                                else -> "Strength: ${mapping.strength}"
                                            }
                                        }
                                        text {
                                            ::content {
                                                if (selectedMapping()?._id == mapping._id) "✓ Selected" else ""
                                            }
                                        }
                                    }
                                    text {
                                        content = when {
                                            mapping.quantity == null -> "Quantity: Customizable"
                                            else -> "Quantity: ${mapping.quantity}"
                                        }
                                    }
                                    row {
                                        subtext("Price ${formatCents(mapping.price)}")
                                        subtext("Ship ${formatCents(mapping.shippingFee)}")
                                        subtext("Tax ${formatCents(mapping.tax)}")
                                        // TODO: role-gate pricing display (ClinicAdmin / MA should not see).
                                    }
                                    text("Total ${formatCents(mapping.total)} · Lead time ${mapping.leadTimeDays} d")
                                    if (!strengthMatches) {
                                        subtext("Strength does not match prescription (${target}).")
                                    }
                                }
                                ::enabled { strengthMatches }
                                onClick {
                                    selectedMapping.value = mapping
                                    if (mapping.quantity != null) customQuantity.value = mapping.quantity.toString()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.quantityDurationSection() = card.col {
        h3("Quantity and duration")
        reactive {
            clearChildren()
            val mapping = selectedMapping()
            if (mapping == null) {
                subtext("Pick a pharmacy first.")
                return@reactive
            }

            if (mapping.quantity == null) {
                field("Quantity (customizable)") {
                    row {
                        expanding.textInput {
                            keyboardHints = KeyboardHints.decimal
                            content bind customQuantity
                        }
                        centered.subtext {
                            ::content {
                                val ft = effectiveFormType()
                                if (ft == null) "" else effectiveProduct()?.forms?.firstOrNull { it.form == ft }?.quantityUnit ?: ""
                            }
                        }
                    }
                }
            } else {
                row {
                    subtext("Quantity")
                    text {
                        val ft = effectiveFormType()
                        val unit = ft?.let { effectiveProduct()?.forms?.firstOrNull { f -> f.form == it }?.quantityUnit }
                        content = if (unit != null) "${mapping.quantity} $unit" else mapping.quantity.toString()
                    }
                }
            }

            field("Will last (days)") {
                textInput {
                    keyboardHints = KeyboardHints.integer
                    content bind willLastDays
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.summarySection() = card.col {
        h3("Order summary")
        reactive {
            clearChildren()
            val prod = effectiveProduct()
            val ft = effectiveFormType()
            val unit = ft?.let { prod?.forms?.firstOrNull { f -> f.form == it }?.strengthUnit }
            val theSig = if (isRefill) refillPrescription()?.instructions ?: "" else sig()
            val mapping = selectedMapping()
            val dest = destination()
            col {
                row {
                    subtext("Patient")
                    text(patient()?.displayName ?: "—")
                }
                row {
                    subtext("Prescription")
                    text(
                        listOfNotNull(
                            prod?.name,
                            ft?.label(),
                            effectiveStrength().takeIf { it > 0 }?.let { "${it}${unit?.let { u -> " $u" } ?: ""}" },
                            "qty ${effectiveQuantity()}",
                            theSig.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                    )
                }
                row {
                    subtext("Pharmacy")
                    text(
                        if (mapping == null) "—"
                        else {
                            val name = eligiblePharmacies().firstOrNull { it.first._id == mapping.pharmacy }?.first?.name ?: "—"
                            "$name · lead ${mapping.leadTimeDays} d · total ${formatCents(mapping.total)}"
                        }
                    )
                }
                row {
                    subtext("Ship to")
                    text(
                        if (dest == null) "—"
                        else "${dest.address.recipient} · ${dest.address.line1}, ${dest.address.city}, ${dest.address.state} ${dest.address.zip}"
                    )
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.validationBanner() = col {
        shownWhen { blockers().isNotEmpty() }.card.col {
            h4("Cannot submit yet")
            reactive {
                clearChildren()
                blockers().forEach { subtext("• $it") }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.actionsSection() = card.row {
        val ctx = context
        button {
            text { ::content { if (submitting()) "Saving..." else "Save draft" } }
            ::enabled { !submitting() && patient() != null }
            onClick {
                saveOrder(ctx, submit = false)
            }
        }
        expanding.space()
        shownWhen { canSubmitAsMe() }.important.button {
            text { ::content { if (submitting()) "Submitting..." else "Submit (ID.me)" } }
            ::enabled { !submitting() && blockers().isEmpty() }
            onClick {
                openIdMeStub(ctx) {
                    saveOrder(ctx, submit = true)
                }
            }
        }
    }

    /* ---- Save / submit ---- */

    private suspend fun saveOrder(ctx: com.lightningkite.kiteui.views.ElementContext, submit: Boolean) {
        val session = currentSession() ?: return
        val cid = activeClinic() ?: return
        val u = me() ?: return
        val pat = patient() ?: run { ctx.toast("Pick a patient"); return }
        val dest = destination() ?: run { ctx.toast("Pick a ship-to address"); return }
        val mapping = selectedMapping() ?: run { ctx.toast("Pick a pharmacy"); return }
        val prescBy = prescriberId() ?: run { ctx.toast("Pick a prescriber"); return }
        val days = willLastDays.value.toIntOrNull() ?: run { ctx.toast("Set will-last days"); return }
        val qty = effectiveQuantity()
        if (qty <= 0) { ctx.toast("Set quantity"); return }

        if (submit && blockers().isNotEmpty()) {
            ctx.toast("Resolve blockers first")
            return
        }

        submitting.value = true
        try {
            val rxId = if (isRefill) prescriptionId!! else Prescription.ID(Uuid.random())

            if (!isRefill) {
                val prod = product() ?: return
                val ft = formType() ?: return
                val s = strength().toDoubleOrNull() ?: return
                val endsAtParsed = endsAtText.value.trim().takeIf { it.isNotBlank() }
                    ?.let { runCatching { kotlin.time.Instant.parse(it) }.getOrNull() }
                val rx = Prescription(
                    _id = rxId,
                    clinic = cid,
                    patient = pat._id,
                    product = prod._id,
                    prescribedBy = prescBy,
                    form = ft,
                    strength = s,
                    instructions = sig.value,
                    endsAt = endsAtParsed,
                )
                session.prescriptions.add(rx)
            }

            val rx = session.prescriptions[rxId].invoke() ?: return
            val now = Clock.System.now()

            val clinReview = if (submit) ClinicianReview(
                user = u._id,
                idEvent = "stub-" + Uuid.random().toString(),
                approved = true,
                at = now,
            ) else null

            val order = PrescriptionOrder(
                _id = PrescriptionOrder.ID(Uuid.random()),
                prescription = rx._id,
                pharmacy = mapping.pharmacy,
                destination = dest,
                quantity = qty,
                willLastDays = days,
                clinic = cid,
                patient = rx.patient,
                product = rx.product,
                form = rx.form,
                strength = rx.strength,
                instructions = rx.instructions,
                prescribedBy = rx.prescribedBy,
                createdBy = u._id,
                assignedTo = rx.prescribedBy,
                consentAffirmedAt = if (submit) now else null,
                clinicianReview = clinReview,
            )
            session.prescriptionOrders.add(order)

            if (submit) {
                ctx.toast("Submitted")
                ctx.pageNavigator.navigate(OrderDetailPage(order._id))
            } else {
                ctx.toast("Draft saved")
                ctx.pageNavigator.navigate(OrdersListPage())
            }
        } finally {
            submitting.value = false
        }
    }

    enum class ShipToOption { ClinicPrimary, ClinicAdditional, Patient }
}

private fun formatCents(cents: Int): String {
    val dollars = cents / 100
    val rem = (cents % 100).toString().padStart(2, '0')
    return "$$dollars.$rem"
}
