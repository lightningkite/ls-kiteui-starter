package com.heroscript.views.components

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*

/**
 * Free-text search picker over the active clinic's patients. Selecting a row writes the
 * Patient to [value]; clearing writes null.
 */
fun ElementWriter.CanAddTheme.patientPicker(value: MutableReactive<Patient?>) = col {
    val search = Signal("")

    val results = remember {
        val session = currentSession() ?: return@remember emptyList()
        val clinicId = activeClinic() ?: return@remember emptyList()
        val q = search().trim().takeIf { it.isNotBlank() } ?: return@remember emptyList()
        val nameSearch =
            condition<Patient> { it.firstName.contains(q, ignoreCase = true) } or
                condition<Patient> { it.lastName.contains(q, ignoreCase = true) }
        session.patients.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<Patient> { it.clinic eq clinicId },
                        nameSearch,
                    )
                ),
                limit = 10,
            )
        )()
    }

    shownWhen { value() == null }.col {
        field("Patient") {
            textInput {
                hint = "Search by name"
                content bind search
            }
        }
        col {
            reactive {
                clearChildren()
                results().forEach { p ->
                    card.button {
                        col {
                            text(p.displayName)
                            subtext("DOB ${p.dateOfBirth}")
                        }
                        onClick {
                            value.set(p)
                            search.value = ""
                        }
                    }
                }
            }
        }
    }

    shownWhen { value() != null }.card.row {
        expanding.col {
            text { ::content { value()?.displayName ?: "" } }
            subtext { ::content { value()?.let { "DOB ${it.dateOfBirth} · ${it.gender}" } ?: "" } }
        }
        button {
            text("Change")
            onClick { value.set(null) }
        }
    }
}
