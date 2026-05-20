package com.heroscript.views.catalog

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.views.components.pharmacyPicker
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
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlin.time.Clock

@Routable("catalog/{id}")
class CatalogDetailPage(
    val id: Product.ID,
    val startInEditMode: Boolean = false,
) : PageWithParent {
    override val title: Reactive<String> get() = Constant("Product")
    override var parentPage: Page = CatalogListPage()

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    private val loaded = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending null
        session.products[id].invoke()
    }

    private val draft = Signal<Product?>(null)
    private val editMode = Signal(startInEditMode)
    private val isNew = Signal(false)

    private val mappings = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        session.productPharmacyMappings.query(
            Query(condition<ProductPharmacyMapping> { it.product eq id })
        )()
    }

    private val addingMapping = Signal(false)

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            val current = loaded()
            if (current != null) {
                if (draft.value == null) draft.value = current
            } else if (draft.value == null && startInEditMode) {
                draft.value = Product(
                    _id = id,
                    name = "",
                    description = "",
                    forms = setOf(),
                    controlled = false,
                    active = true,
                )
                isNew.value = true
            }
        }

        scrolling.col {
            shownWhen { draft() == null }.padded.col { centered.text("Loading...") }

            shownWhen { draft() != null }.col {
                headerSection()
                descriptionSection()
                formsSection()
                shownWhen { !isNew() }.col { mappingsSection() }
                shownWhen { isNew() }.card.col {
                    subtext("Save the product to add pharmacy mappings.")
                }
                actionRow()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.headerSection() = card.col {
        row {
            expanding.h2 {
                ::content {
                    val d = draft()
                    when {
                        d == null -> "Product"
                        d.name.isBlank() -> "New product"
                        else -> d.name
                    }
                }
            }
            shownWhen { draft()?.controlled == true }.subtext("Controlled")
            shownWhen { draft()?.active == false }.subtext("Inactive")
            shownWhen { draft()?.active == true && draft()?.controlled != true }.subtext("Active")
            shownWhen { isOps() }.button {
                text { ::content { if (editMode()) "Cancel" else "Edit" } }
                onClick {
                    if (editMode.value) {
                        val current = loaded()
                        if (current != null) {
                            draft.value = current
                            editMode.value = false
                        } else {
                            context.toast("New product — save or leave the page")
                        }
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
                    content bind productField("", { it.name }, { d, v -> d.copy(name = v) })
                }
            }
            row {
                card.row {
                    centered.checkbox {
                        checked bind productField(false, { it.controlled }, { d, v -> d.copy(controlled = v) })
                    }
                    centered.text("Controlled")
                }
                card.row {
                    centered.checkbox {
                        checked bind productField(true, { it.active }, { d, v -> d.copy(active = v) })
                    }
                    centered.text("Active")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.descriptionSection() = card.col {
        h3("Description")
        shownWhen { editMode() }.textArea {
            hint = "Description"
            content bind productField("", { it.description }, { d, v -> d.copy(description = v) })
        }
        shownWhen { !editMode() }.text {
            ::content {
                draft()?.description?.takeIf { it.isNotBlank() } ?: "No description provided."
            }
        }
    }

    private fun ElementWriter.CanAddTheme.formsSection() = card.col {
        row {
            expanding.h3("Forms")
            shownWhen { editMode() }.button {
                icon(Icon.add, "Add form")
                onClick {
                    val d = draft.value ?: return@onClick
                    val used = d.forms.map { it.form }.toSet()
                    val candidate = Product.FormType.entries.firstOrNull { it !in used }
                    if (candidate == null) {
                        context.toast("All form types already added")
                        return@onClick
                    }
                    draft.value = d.copy(
                        forms = d.forms + Product.Form(
                            form = candidate,
                            strengthUnit = "",
                            quantityUnit = "",
                        )
                    )
                }
            }
        }

        shownWhen { editMode() }.subtext("Mappings reference this form by type — unit changes only affect display labels.")

        reactive {
            val forms = draft()?.forms?.toList().orEmpty()
            if (forms.isEmpty()) {
                if (editMode()) subtext("Add at least one form before saving.")
                else subtext("No forms configured.")
            }
        }

        col {
            reactive {
                clearChildren()
                val forms = draft()?.forms?.toList()?.sortedBy { it.form.ordinal } ?: return@reactive
                forms.forEachIndexed { index, form ->
                    card.col {
                        if (editMode()) formEditor(form)
                        else formReadOnly(form)
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.formReadOnly(form: Product.Form) = col {
        h4(form.form.label())
        row {
            subtext("Strength: ${form.strengthUnit}")
            subtext("Quantity: ${form.quantityUnit}")
        }
    }

    private fun ElementWriter.CanAddTheme.formEditor(form: Product.Form) = col {
        val strengthUnit = Signal(form.strengthUnit)
        val quantityUnit = Signal(form.quantityUnit)

        reactive {
            val current = draft.value ?: return@reactive
            val existing = current.forms.firstOrNull { it.form == form.form } ?: return@reactive
            val updated = existing.copy(
                strengthUnit = strengthUnit(),
                quantityUnit = quantityUnit(),
            )
            if (existing != updated) {
                draft.value = current.copy(
                    forms = current.forms.filterNot { it.form == form.form }.toSet() + updated
                )
            }
        }

        row {
            expanding.h4(form.form.label())
            button {
                text("Remove")
                onClick {
                    val d = draft.value ?: return@onClick
                    val mappingsForType = mappings().any { it.form == form.form }
                    if (mappingsForType) {
                        context.toast("Remove pharmacy mappings for this form first")
                        return@onClick
                    }
                    draft.value = d.copy(forms = d.forms.filterNot { it.form == form.form }.toSet())
                }
            }
        }
        subtext("Form type is locked — removing and re-adding would orphan existing mappings.")
        field("Strength unit") {
            textInput {
                hint = "e.g. mg/mL"
                content bind strengthUnit
            }
        }
        field("Quantity unit") {
            textInput {
                hint = "e.g. mL"
                content bind quantityUnit
            }
        }
    }

    private fun ElementWriter.CanAddTheme.mappingsSection() = card.col {
        row {
            expanding.h3("Pharmacy mappings")
            shownWhen { editMode() }.button {
                icon(Icon.add, "Add mapping")
                onClick { addingMapping.value = true }
            }
            shownWhen { editMode() }.button {
                text("Bulk import")
                onClick { context.toast("Bulk-import action coming soon") }
            }
        }

        shownWhen { addingMapping() }.card.col { newMappingEditor() }

        reactive {
            val list = mappings()
            if (list.isEmpty()) subtext("Not yet stocked by any pharmacy.")
        }

        col {
            reactive {
                clearChildren()
                val list = mappings()
                if (list.isEmpty()) return@reactive
                val grouped = list.groupBy { it.form }
                grouped.entries.sortedBy { it.key.ordinal }.forEach { (formType, rows) ->
                    card.col {
                        h4(formType.label())
                        rows.forEach { mapping ->
                            card.col {
                                if (editMode()) mappingEditor(mapping)
                                else mappingReadOnly(mapping)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.mappingReadOnly(mapping: ProductPharmacyMapping) {
        val pharmacyName = rememberSuspending {
            val session = currentSession() ?: return@rememberSuspending ""
            session.pharmacies[mapping.pharmacy].invoke()?.name ?: ""
        }
        row {
            expanding.text { ::content { pharmacyName() } }
            shownWhen { !mapping.active }.subtext("Inactive")
        }
        row {
            subtext("Strength: ${mapping.strength?.toString() ?: "Customizable"}")
            subtext("Quantity: ${mapping.quantity?.toString() ?: "Customizable"}")
        }
        subtext("SKU: ${mapping.pharmacySku}")
        subtext("Lead time: ${mapping.leadTimeDays} days")
        row {
            subtext("Price: ${centsToString(mapping.price)}")
            subtext("Tax: ${centsToString(mapping.tax)}")
            subtext("Ship: ${centsToString(mapping.shippingFee)}")
            subtext("Total: ${centsToString(mapping.total)}")
        }
    }

    private fun ElementWriter.CanAddTheme.mappingEditor(mapping: ProductPharmacyMapping) {
        val pharmacyName = rememberSuspending {
            val session = currentSession() ?: return@rememberSuspending ""
            session.pharmacies[mapping.pharmacy].invoke()?.name ?: ""
        }
        val strengthCustomizable = Signal(mapping.strength == null)
        val strengthValue = Signal(mapping.strength?.toString() ?: "")
        val quantityCustomizable = Signal(mapping.quantity == null)
        val quantityValue = Signal(mapping.quantity?.toString() ?: "")
        val sku = Signal(mapping.pharmacySku)
        val priceDollars = Signal(centsToDollarsString(mapping.price))
        val taxDollars = Signal(centsToDollarsString(mapping.tax))
        val shippingDollars = Signal(centsToDollarsString(mapping.shippingFee))
        val totalDollars = Signal(centsToDollarsString(mapping.total))
        val leadTime = Signal(mapping.leadTimeDays.toString())
        val active = Signal(mapping.active)

        reactive {
            val price = dollarsToCents(priceDollars()) ?: 0
            val tax = dollarsToCents(taxDollars()) ?: 0
            val ship = dollarsToCents(shippingDollars()) ?: 0
            val computed = price + tax + ship
            val existing = dollarsToCents(totalDollars()) ?: 0
            if (existing == 0 || existing != computed) {
                totalDollars.value = centsToDollarsString(computed)
            }
        }

        row {
            expanding.text { ::content { pharmacyName() } }
            card.row {
                centered.checkbox { checked bind active }
                centered.text("Active")
            }
        }
        subtext("Form: ${mapping.form.label()} (pharmacy and form locked — delete and re-add to change)")
        row {
            expanding.col {
                row {
                    card.row {
                        centered.checkbox { checked bind strengthCustomizable }
                        centered.text("Customizable")
                    }
                }
                shownWhen { !strengthCustomizable() }.field("Strength") {
                    textInput {
                        keyboardHints = KeyboardHints.decimal
                        content bind strengthValue
                    }
                }
            }
            expanding.col {
                row {
                    card.row {
                        centered.checkbox { checked bind quantityCustomizable }
                        centered.text("Customizable")
                    }
                }
                shownWhen { !quantityCustomizable() }.field("Quantity") {
                    textInput {
                        keyboardHints = KeyboardHints.decimal
                        content bind quantityValue
                    }
                }
            }
        }
        field("Pharmacy SKU") {
            textInput { content bind sku }
        }
        row {
            expanding.field("Price ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind priceDollars
                }
            }
            expanding.field("Tax ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind taxDollars
                }
            }
        }
        row {
            expanding.field("Shipping ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind shippingDollars
                }
            }
            expanding.field("Total ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind totalDollars
                }
            }
        }
        field("Lead time (days)") {
            textInput {
                keyboardHints = KeyboardHints.integer
                content bind leadTime
            }
        }
        row {
            atEnd.button {
                text("Remove")
                onClick {
                    val session = currentSession() ?: return@onClick
                    session.productPharmacyMappings[mapping._id].delete()
                    context.toast("Mapping removed")
                }
            }
            atEnd.important.button {
                text("Apply")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val strength = if (strengthCustomizable.value) null else strengthValue.value.toDoubleOrNull()
                    val quantity = if (quantityCustomizable.value) null else quantityValue.value.toDoubleOrNull()
                    val price = dollarsToCents(priceDollars.value)
                    val tax = dollarsToCents(taxDollars.value) ?: 0
                    val ship = dollarsToCents(shippingDollars.value) ?: 0
                    val total = dollarsToCents(totalDollars.value) ?: ((price ?: 0) + tax + ship)
                    val leadDays = leadTime.value.toIntOrNull()
                    val problems = buildList {
                        if (!strengthCustomizable.value && strength == null) add("strength")
                        if (!quantityCustomizable.value && quantity == null) add("quantity")
                        if (sku.value.isBlank()) add("SKU")
                        if (price == null) add("price")
                        if (leadDays == null) add("lead time")
                    }
                    if (problems.isNotEmpty()) {
                        context.toast("Please fix: ${problems.joinToString(", ")}")
                        return@onClick
                    }
                    session.productPharmacyMappings[mapping._id].set(
                        mapping.copy(
                            strength = strength,
                            quantity = quantity,
                            pharmacySku = sku.value.trim(),
                            price = price!!,
                            tax = tax,
                            shippingFee = ship,
                            total = total,
                            leadTimeDays = leadDays!!,
                            active = active.value,
                            updatedAt = Clock.System.now(),
                        )
                    )
                    context.toast("Mapping saved")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.newMappingEditor() = col {
        h4("Add mapping")

        val pharmacy = Signal<Pharmacy?>(null)
        val formType = Signal<Product.FormType?>(null)
        val strengthCustomizable = Signal(false)
        val strengthValue = Signal("")
        val quantityCustomizable = Signal(false)
        val quantityValue = Signal("")
        val sku = Signal("")
        val priceDollars = Signal("")
        val taxDollars = Signal("")
        val shippingDollars = Signal("")
        val totalDollars = Signal("")
        val leadTime = Signal("")

        pharmacyPicker(pharmacy)

        val availableTypes = remember {
            draft()?.forms?.map { it.form }?.sortedBy { it.ordinal } ?: emptyList()
        }
        field("Form type") {
            select {
                bind(
                    edits = formType,
                    data = remember { listOf<Product.FormType?>(null) + availableTypes() },
                    render = { it?.label() ?: "Select form…" },
                )
            }
        }
        subtext("Constrained to FormTypes this Product owns.")

        row {
            expanding.col {
                card.row {
                    centered.checkbox { checked bind strengthCustomizable }
                    centered.text("Strength customizable")
                }
                shownWhen { !strengthCustomizable() }.field("Strength") {
                    textInput {
                        keyboardHints = KeyboardHints.decimal
                        content bind strengthValue
                    }
                }
            }
            expanding.col {
                card.row {
                    centered.checkbox { checked bind quantityCustomizable }
                    centered.text("Quantity customizable")
                }
                shownWhen { !quantityCustomizable() }.field("Quantity") {
                    textInput {
                        keyboardHints = KeyboardHints.decimal
                        content bind quantityValue
                    }
                }
            }
        }
        field("Pharmacy SKU") {
            textInput { content bind sku }
        }
        row {
            expanding.field("Price ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind priceDollars
                }
            }
            expanding.field("Tax ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind taxDollars
                }
            }
        }
        row {
            expanding.field("Shipping ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind shippingDollars
                }
            }
            expanding.field("Total ($)") {
                textInput {
                    keyboardHints = KeyboardHints.decimal
                    content bind totalDollars
                }
            }
        }
        field("Lead time (days)") {
            textInput {
                keyboardHints = KeyboardHints.integer
                content bind leadTime
            }
        }

        reactive {
            val price = dollarsToCents(priceDollars()) ?: 0
            val tax = dollarsToCents(taxDollars()) ?: 0
            val ship = dollarsToCents(shippingDollars()) ?: 0
            val computed = price + tax + ship
            val existing = dollarsToCents(totalDollars()) ?: 0
            if (existing == 0 || existing != computed) {
                totalDollars.value = centsToDollarsString(computed)
            }
        }

        row {
            atEnd.button {
                text("Cancel")
                onClick { addingMapping.value = false }
            }
            atEnd.important.button {
                text("Save mapping")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val pid = pharmacy.value?._id
                    val ft = formType.value
                    val strength = if (strengthCustomizable.value) null else strengthValue.value.toDoubleOrNull()
                    val quantity = if (quantityCustomizable.value) null else quantityValue.value.toDoubleOrNull()
                    val price = dollarsToCents(priceDollars.value)
                    val tax = dollarsToCents(taxDollars.value) ?: 0
                    val ship = dollarsToCents(shippingDollars.value) ?: 0
                    val total = dollarsToCents(totalDollars.value) ?: ((price ?: 0) + tax + ship)
                    val leadDays = leadTime.value.toIntOrNull()
                    val problems = buildList {
                        if (pid == null) add("pharmacy")
                        if (ft == null) add("form type")
                        if (!strengthCustomizable.value && strength == null) add("strength")
                        if (!quantityCustomizable.value && quantity == null) add("quantity")
                        if (sku.value.isBlank()) add("SKU")
                        if (price == null) add("price")
                        if (leadDays == null) add("lead time")
                    }
                    if (problems.isNotEmpty()) {
                        context.toast("Please fix: ${problems.joinToString(", ")}")
                        return@onClick
                    }
                    session.productPharmacyMappings.add(
                        ProductPharmacyMapping(
                            pharmacy = pid!!,
                            product = id,
                            form = ft!!,
                            strength = strength,
                            quantity = quantity,
                            pharmacySku = sku.value.trim(),
                            price = price!!,
                            tax = tax,
                            shippingFee = ship,
                            total = total,
                            leadTimeDays = leadDays!!,
                        )
                    )
                    addingMapping.value = false
                    context.toast("Mapping added")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.actionRow() = row {
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
                if (isNew.value) {
                    session.products.add(d)
                    isNew.value = false
                } else {
                    session.products[id].set(d)
                }
                draft.value = d
                editMode.value = false
                context.toast("Saved")
            }
        }
    }

    private fun validate(p: Product): List<String> = buildList {
        if (p.name.isBlank()) add("name")
        if (p.forms.isEmpty()) add("at least one form")
        val typeCounts = p.forms.groupingBy { it.form }.eachCount()
        if (typeCounts.values.any { it > 1 }) add("duplicate form types")
        p.forms.forEachIndexed { i, f ->
            if (f.strengthUnit.isBlank()) add("form ${i + 1} strength unit")
            if (f.quantityUnit.isBlank()) add("form ${i + 1} quantity unit")
        }
    }

    private fun <V> productField(
        default: V,
        get: (Product) -> V,
        set: (Product, V) -> Product,
    ): MutableReactive<V> =
        draft.lens(
            get = { current -> current?.let(get) ?: default },
            modify = { current, v -> current?.let { set(it, v) } },
        )

}

internal fun Product.FormType.label(): String = when (this) {
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

private fun centsToString(c: Int): String {
    val dollars = c / 100
    val remainder = (if (c < 0) -c else c) % 100
    val sign = if (c < 0) "-" else ""
    return "$sign\$$dollars.${remainder.toString().padStart(2, '0')}"
}

private fun centsToDollarsString(c: Int): String {
    val sign = if (c < 0) "-" else ""
    val abs = if (c < 0) -c else c
    val dollars = abs / 100
    val remainder = abs % 100
    return "$sign$dollars.${remainder.toString().padStart(2, '0')}"
}

private fun dollarsToCents(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toDoubleOrNull() ?: return null
    return kotlin.math.round(parsed * 100.0).toInt()
}
