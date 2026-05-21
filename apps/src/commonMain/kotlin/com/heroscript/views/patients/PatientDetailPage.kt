package com.heroscript.views.patients

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.heroscript.views.components.addressEditor
import com.heroscript.views.components.isFilledOut
import com.heroscript.views.orders.OrderEntryPage
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

/**
 * Patient detail with inline edit toggle. Same screen for add-patient (open in edit mode
 * against a blank Patient) and existing-patient edit.
 *
 * Edit mode and the working draft are local Signals here rather than a Draft<Patient> wrapper
 * over the cache — the cache item's MutableReactive<Patient?> is null-typed, and the USBE
 * NonNullModelCacheItem helper required to make Draft compose cleanly would have pulled in
 * more port surface than this single screen justifies. Once a second edit screen lands, lift
 * the pattern into a reusable helper.
 */
@Routable("patients/{id}")
class PatientDetailPage(
    val id: Patient.ID,
    val startInEditMode: Boolean = false,
) : PageWithParent {
    override val title: Reactive<String> get() = Constant("Patient")
    override var parentPage: Page = PatientListPage()

    private val editMode = Signal(startInEditMode)

    private val loaded = remember {
        val session = currentSession() ?: return@remember null
        session.patients[id].invoke()
    }

    private val draft = Signal<Patient?>(null)

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            val current = loaded()
            val session = currentSession()
            if (current != null) {
                if (draft.value == null) draft.value = current
            } else if (draft.value == null && startInEditMode && session != null) {
                val clinicId = activeClinic() ?: return@reactive
                val me = session.self().let { it._id }
                draft.value = Patient(
                    _id = id,
                    clinic = clinicId,
                    firstName = "",
                    lastName = "",
                    gender = Gender.U,
                    dateOfBirth = today(),
                    shippingAddress = VerifiedAddress(address = Address.EMPTY),
                    createdBy = me,
                )
            }
        }

        scrolling.col {
            shownWhen { draft() == null }.padded.col { centered.text("Loading...") }

            shownWhen { draft() != null }.col {
                identitySection()
                shippingSection()
                consentSection()
                clinicalSection()
                placeholderSection()
                actionRow()
            }
        }
    }

    private fun ElementWriter.CanAddTheme.identitySection() = card.col {
        row {
            expanding.h2 {
                ::content {
                    val d = draft()
                    when {
                        d == null -> "Patient"
                        d.firstName.isBlank() && d.lastName.isBlank() -> "New patient"
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
                    content bind patientField("", { it.firstName }, { d, v -> d.copy(firstName = v) })
                }
            }
            field("Last name") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind patientField("", { it.lastName }, { d, v -> d.copy(lastName = v) })
                }
            }
            field("Gender") {
                select {
                    bind(
                        edits = patientField(Gender.U, { it.gender }, { d, v -> d.copy(gender = v) }),
                        data = Constant(Gender.entries.toList()),
                        render = { it.label() },
                    )
                }
            }
            field("Date of birth") {
                localDateField {
                    content bind patientField<kotlinx.datetime.LocalDate?>(
                        null,
                        { it.dateOfBirth },
                        { d, v -> d.copy(dateOfBirth = v ?: today()) },
                    )
                }
            }
            field("Phone") {
                textInput {
                    keyboardHints = KeyboardHints.phone
                    content bind patientField(
                        "",
                        { it.phoneNumber?.raw ?: "" },
                        { d, v -> d.copy(phoneNumber = v.takeIf { it.isNotBlank() }?.toPhoneNumber()) },
                    )
                }
            }
            field("Email") {
                textInput {
                    keyboardHints = KeyboardHints.email
                    content bind patientField(
                        "",
                        { it.email?.raw ?: "" },
                        { d, v -> d.copy(email = v.takeIf { it.isNotBlank() }?.toEmailAddress()) },
                    )
                }
            }
        }

        shownWhen { !editMode() }.col {
            row {
                subtext { ::content { (draft()?.gender ?: Gender.U).label() } }
                subtext { ::content { "DOB ${draft()?.dateOfBirth ?: today()}" } }
            }
            shownWhen { draft()?.phoneNumber != null }.text { ::content { draft()?.phoneNumber?.raw ?: "" } }
            shownWhen { draft()?.email != null }.text { ::content { draft()?.email?.raw ?: "" } }
        }
    }

    private fun ElementWriter.CanAddTheme.shippingSection() = card.col {
        h3("Shipping address")
        addressEditor(
            value = patientField(
                VerifiedAddress(address = Address.EMPTY),
                { it.shippingAddress },
                { d, v -> d.copy(shippingAddress = v) },
            ),
            editing = editMode,
        )
    }

    private fun ElementWriter.CanAddTheme.consentSection() = card.col {
        h3("Consent")
        row {
            expanding.col {
                text("SMS")
                subtext {
                    ::content { draft()?.smsConsent?.let { "Affirmed $it" } ?: "Not affirmed" }
                }
            }
            button {
                text("Re-affirm")
                onClick {
                    draft.value = draft.value?.copy(smsConsent = Clock.System.now())
                }
            }
        }
        row {
            expanding.col {
                text("Email")
                subtext {
                    ::content { draft()?.emailConsent?.let { "Affirmed $it" } ?: "Not affirmed" }
                }
            }
            button {
                text("Re-affirm")
                onClick {
                    draft.value = draft.value?.copy(emailConsent = Clock.System.now())
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.clinicalSection() = card.col {
        h3("Clinical")
        clinicalEntryList(
            label = "Allergies",
            withReaction = true,
            get = { it.allergies },
            set = { d, v -> d.copy(allergies = v) },
        )
        clinicalEntryList(
            label = "Diseases",
            withReaction = false,
            get = { it.diseases },
            set = { d, v -> d.copy(diseases = v) },
        )
        clinicalEntryList(
            label = "Other medications",
            withReaction = false,
            get = { it.otherMedications },
            set = { d, v -> d.copy(otherMedications = v) },
        )
    }

    private fun ElementWriter.CanAddTheme.clinicalEntryList(
        label: String,
        withReaction: Boolean,
        get: (Patient) -> List<ClinicalEntry>?,
        set: (Patient, List<ClinicalEntry>?) -> Patient,
    ) = col {
        row {
            expanding.h4(label)
            shownWhen { get(draft() ?: return@shownWhen false) == null && editMode() }.button {
                text("Ask now")
                onClick { draft.value = draft.value?.let { set(it, emptyList()) } }
            }
            shownWhen {
                val list = get(draft() ?: return@shownWhen false)
                editMode() && list != null && list.isEmpty()
            }.button {
                text("Mark unasked")
                onClick { draft.value = draft.value?.let { set(it, null) } }
            }
            shownWhen {
                val list = get(draft() ?: return@shownWhen false)
                editMode() && list != null
            }.button {
                icon(Icon.add, "Add entry")
                onClick {
                    val cur = draft.value ?: return@onClick
                    val existing = get(cur) ?: emptyList()
                    draft.value = set(cur, existing + ClinicalEntry(description = ""))
                }
            }
        }

        reactive {
            val d = draft() ?: return@reactive
            val list = get(d)
            when {
                list == null -> subtext("Not asked at intake")
                list.isEmpty() -> subtext("Patient reported none")
                else -> {} // entries render below
            }
        }

        col {
            reactive {
                clearChildren()
                val d = draft() ?: return@reactive
                val list = get(d) ?: return@reactive
                list.forEachIndexed { index, entry ->
                    card.col {
                        if (editMode()) clinicalEntryEditor(entry, withReaction) { updated ->
                            val current = draft.value ?: return@clinicalEntryEditor
                            val currentList = get(current) ?: return@clinicalEntryEditor
                            draft.value = if (updated == null) {
                                set(current, currentList.toMutableList().also { it.removeAt(index) })
                            } else {
                                set(current, currentList.toMutableList().also { it[index] = updated })
                            }
                        }
                        else clinicalEntryReadOnly(entry, withReaction)
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.clinicalEntryReadOnly(entry: ClinicalEntry, withReaction: Boolean) = col {
        text(entry.description.ifBlank { "(no description)" })
        entry.code?.let { subtext("Code: $it") }
        entry.source?.let { subtext("Source: ${it.name}") }
        if (withReaction) entry.reaction?.let { subtext("Reaction: $it") }
        if (entry.startDate != null || entry.endDate != null) {
            subtext("From ${entry.startDate ?: "?"} to ${entry.endDate ?: "present"}")
        }
    }

    private fun ElementWriter.CanAddTheme.clinicalEntryEditor(
        entry: ClinicalEntry,
        withReaction: Boolean,
        onChange: (ClinicalEntry?) -> Unit,
    ) = col {
        val description = Signal(entry.description)
        val code = Signal(entry.code ?: "")
        val source = Signal<ClinicalSource?>(entry.source)
        val reaction = Signal(entry.reaction ?: "")

        reactive {
            onChange(
                entry.copy(
                    description = description(),
                    code = code().takeIf { it.isNotBlank() },
                    source = source(),
                    reaction = if (withReaction) reaction().takeIf { it.isNotBlank() } else null,
                )
            )
        }

        field("Description") { textInput { content bind description } }
        field("Code (optional)") { textInput { content bind code } }
        field("Source") {
            select {
                bind(
                    edits = source,
                    data = Constant(listOf<ClinicalSource?>(null) + ClinicalSource.entries.toList()),
                    render = { it?.name ?: "—" },
                )
            }
        }
        if (withReaction) {
            field("Reaction (optional)") { textInput { content bind reaction } }
        }
        atEnd.button {
            text("Remove")
            onClick { onChange(null) }
        }
    }

    private fun ElementWriter.CanAddTheme.placeholderSection() = card.col {
        h3("History")
        subtext("Prescriptions, orders, and notification history will appear here in a later phase.")

        shownWhen { !editMode() && loaded() != null }.row {
            button {
                text("Start order for this patient")
                onClick { context.pageNavigator.navigate(OrderEntryPage(patientId = id)) }
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
                val updated = d.copy(updatedAt = Clock.System.now())
                session.patients[id].set(updated)
                draft.value = updated
                editMode.value = false
                context.toast("Saved")
            }
        }
    }

    private fun validate(p: Patient): List<String> = buildList {
        if (p.firstName.isBlank()) add("first name")
        if (p.lastName.isBlank()) add("last name")
        if (!p.shippingAddress.address.isFilledOut()) add("shipping address")
    }

    private fun <V> patientField(
        default: V,
        get: (Patient) -> V,
        set: (Patient, V) -> Patient,
    ): com.lightningkite.reactive.core.MutableReactive<V> =
        draft.lens(
            get = { current -> current?.let(get) ?: default },
            modify = { current, v -> current?.let { set(it, v) } },
        )
}
