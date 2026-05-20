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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*

/** Free-text search picker over active Products. */
fun ElementWriter.CanAddTheme.productPicker(value: MutableReactive<Product?>) = col {
    val search = Signal("")

    val results = rememberSuspending {
        val session = currentSession() ?: return@rememberSuspending emptyList()
        val q = search().trim().takeIf { it.isNotBlank() } ?: return@rememberSuspending emptyList()
        session.products.query(
            Query(
                condition = Condition.And(
                    listOf(
                        condition<Product> { it.active eq true },
                        condition<Product> { it.name.contains(q, ignoreCase = true) },
                    )
                ),
                limit = 10,
            )
        )()
    }

    shownWhen { value() == null }.col {
        field("Product") {
            textInput {
                hint = "Search product name"
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
                            if (p.controlled) subtext("Controlled")
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
            subtext { ::content { if (value()?.controlled == true) "Controlled" else "" } }
        }
        button {
            text("Change")
            onClick { value.set(null) }
        }
    }
}
