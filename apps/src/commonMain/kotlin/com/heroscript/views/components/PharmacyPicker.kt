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

/** Free-text search picker over active Pharmacies. */
fun ElementWriter.CanAddTheme.pharmacyPicker(value: MutableReactive<Pharmacy?>) = col {
    val search = Signal("")

    val results = remember {
        val session = currentSession() ?: return@remember emptyList()
        val q = search().trim().takeIf { it.isNotBlank() } ?: return@remember emptyList()
        session.pharmacies.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<Pharmacy> { it.deactivatedAt eq null },
                        condition<Pharmacy> { it.name.contains(q, ignoreCase = true) },
                    )
                ),
                limit = 10,
            )
        )()
    }

    shownWhen { value() == null }.col {
        field("Pharmacy") {
            textInput {
                hint = "Search pharmacy name"
                content bind search
            }
        }
        col {
            reactive {
                clearChildren()
                results().forEach { p ->
                    card.button {
                        row {
                            expanding.text(p.name)
                            subtext(p.adapterType.name)
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
            text { ::content { value()?.name ?: "" } }
            subtext { ::content { value()?.adapterType?.name ?: "" } }
        }
        button {
            text("Change")
            onClick { value.set(null) }
        }
    }
}
