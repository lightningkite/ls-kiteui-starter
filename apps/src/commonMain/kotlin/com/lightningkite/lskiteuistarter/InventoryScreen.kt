// by Claude — Read-only inventory list screen
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.lskiteuistarter.sdk.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*

@Routable("/inventory")
class InventoryPage : Page {
    override val title: Reactive<String> get() = Constant("Inventory")

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val session = currentSessionNotNull

        val items = remember {
            val s = session()
            s.inventoryItems.list(Query(Condition.Always))()
        }

        scrolling.col {
            centered.h2 {
                debugName = "inventoryTitle" // by Claude — testId
                content = "Inventory"
            }

            // by Claude — Add Item button
            important.buttonTheme.button {
                debugName = "addItemButton" // by Claude — testId
                centered.text("Add Item")
                onClick { pageNavigator.navigate(InventoryNewPage()) }
            }

            col {
                debugName = "inventoryList" // by Claude — testId
                forEachById(items, id = { it._id }) { itemReactive ->
                    card.row {
                        expanding.col {
                            text { ::content { itemReactive().name } }
                            subtext { ::content {
                                val item = itemReactive()
                                buildString {
                                    append("Category: ${item.category.name}")
                                    if (item.notes != null) append(" — ${item.notes}")
                                }
                            } }
                        }
                        col {
                            centered.h3 { ::content { "×${itemReactive().quantity}" } }
                        }
                        // by Claude — Edit button for each item
                        button {
                            centered.text("Edit")
                            onClick {
                                val item: InventoryItem = itemReactive.awaitOnce()
                                pageNavigator.navigate(InventoryEditPage(item._id.toString()))
                            }
                        }
                    }
                }
            }
        }
    }
}
