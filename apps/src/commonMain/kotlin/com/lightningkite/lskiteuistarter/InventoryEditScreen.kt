// by Claude — Create/edit form for inventory items
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.requestFile
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.lightningserver.files.toServerFile
import com.lightningkite.lightningserver.media.ServerFileWithMetadata
import com.lightningkite.lskiteuistarter.sdk.*
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*
import com.lightningkite.services.database.modification
import kotlin.uuid.Uuid

@Routable("inventory/new")
class InventoryNewPage : Page {
    override val title: Reactive<String> get() = Constant("New Item")

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val nameSignal = Signal("")
        val categorySignal = Signal(ItemCategory.entries.first())
        val quantitySignal = Signal<Double?>(null)
        val notesSignal = Signal("")
        val photoSignal = Signal<ServerFileWithMetadata?>(null)

        scrolling.col {
            centered.h2 {
                debugName = "editTitle" // by Claude — testId
                content = "New Item"
            }

            inventoryForm(
                nameSignal = nameSignal,
                categorySignal = categorySignal,
                quantitySignal = quantitySignal,
                notesSignal = notesSignal,
                photoSignal = photoSignal,
            )

            row {
                expanding.button {
                    debugName = "cancelButton" // by Claude — testId
                    centered.text("Cancel")
                    onClick { pageNavigator.goBack() }
                }

                expanding.important.buttonTheme.button {
                    debugName = "saveButton" // by Claude — testId
                    centered.text("Create")
                    ::enabled { nameSignal().isNotBlank() }
                    onClick {
                        val s: UserSession = currentSession.awaitOnce() ?: return@onClick
                        // Get organization from first membership
                        val memberships = s.api.membership.query(Query(Condition.Always))
                        val orgId = memberships.firstOrNull()?.organization
                            ?: throw IllegalStateException("No organization found")
                        s.api.inventoryItem.insert(
                            InventoryItem(
                                organization = orgId,
                                name = nameSignal.value,
                                category = categorySignal.value,
                                quantity = quantitySignal.value?.toInt() ?: 0,
                                notes = notesSignal.value.takeIf { it.isNotBlank() },
                                photo = photoSignal.value,
                            )
                        )
                        pageNavigator.navigate(InventoryPage())
                    }
                }
            }
        }
    }
}

@Routable("inventory/{itemId}/edit")
class InventoryEditPage(val itemId: String) : Page {
    override val title: Reactive<String> get() = Constant("Edit Item")

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val parsedId = Uuid.parse(itemId)

        val nameSignal = Signal("")
        val categorySignal = Signal(ItemCategory.entries.first())
        val quantitySignal = Signal<Double?>(null)
        val notesSignal = Signal("")
        val photoSignal = Signal<ServerFileWithMetadata?>(null)

        // Load existing item data into form signals
        val item: Reactive<InventoryItem> = rememberSuspending {
            val s: UserSession = currentSession() ?: throw kotlinx.coroutines.CancellationException("No session")
            val loaded: InventoryItem = s.api.inventoryItem.detail(parsedId)
            nameSignal.value = loaded.name
            categorySignal.value = loaded.category
            quantitySignal.value = loaded.quantity.toDouble()
            notesSignal.value = loaded.notes ?: ""
            photoSignal.value = loaded.photo
            loaded
        }

        scrolling.col {
            centered.h2 {
                debugName = "editTitle" // by Claude — testId
                ::content { "Edit: ${item().name}" }
            }

            inventoryForm(
                nameSignal = nameSignal,
                categorySignal = categorySignal,
                quantitySignal = quantitySignal,
                notesSignal = notesSignal,
                photoSignal = photoSignal,
            )

            row {
                expanding.button {
                    debugName = "cancelButton" // by Claude — testId
                    centered.text("Cancel")
                    onClick { pageNavigator.goBack() }
                }

                expanding.important.buttonTheme.button {
                    debugName = "saveButton" // by Claude — testId
                    centered.text("Save")
                    ::enabled { nameSignal().isNotBlank() }
                    onClick {
                        val s: UserSession = currentSession.awaitOnce() ?: return@onClick
                        // by Claude — use modify (PATCH) instead of replace (PUT) because
                        // organization and createdAt have cannotBeModified() restrictions
                        s.api.inventoryItem.modify(
                            parsedId,
                            modification {
                                it.name assign nameSignal.value
                                it.category assign categorySignal.value
                                it.quantity assign (quantitySignal.value?.toInt() ?: 0)
                                it.notes assign notesSignal.value.takeIf { it.isNotBlank() }
                                it.photo assign photoSignal.value
                            }
                        )
                        pageNavigator.navigate(InventoryPage())
                    }
                }
            }

            danger.buttonTheme.button {
                debugName = "deleteButton" // by Claude — testId
                centered.text("Delete")
                onClick {
                    val s: UserSession = currentSession.awaitOnce() ?: return@onClick
                    s.api.inventoryItem.delete(parsedId)
                    pageNavigator.navigate(InventoryPage())
                }
            }
        }
    }
}

// by Claude — Shared form fields used by both create and edit pages
fun ViewWriter.inventoryForm(
    nameSignal: Signal<String>,
    categorySignal: Signal<ItemCategory>,
    quantitySignal: Signal<Double?>,
    notesSignal: Signal<String>,
    photoSignal: Signal<ServerFileWithMetadata?>,
) {
    field("Name") {
        textInput {
            debugName = "nameInput" // by Claude — testId
            content bind nameSignal
        }
    }

    field("Category") {
        select {
            debugName = "categorySelect" // by Claude — testId
            bind(categorySignal, Constant(ItemCategory.entries.toList())) { it.name }
        }
    }

    field("Quantity") {
        numberInput {
            debugName = "quantityInput" // by Claude — testId
            content bind quantitySignal
        }
    }

    field("Notes") {
        textArea {
            debugName = "notesInput" // by Claude — testId
            content bind notesSignal
        }
    }

    field("Photo") {
        button {
            debugName = "uploadPhotoButton" // by Claude — testId
            // Capture RContext before entering the onClick lambda (not a ViewWriter scope)
            val rContext = context
            centered.text {
                ::content { if (photoSignal() != null) "Change Photo" else "Upload Photo" }
            }
            onClick {
                val s: UserSession = currentSession.awaitOnce() ?: return@onClick
                val fileRef = rContext.requestFile(listOf("image/*")) ?: return@onClick
                val serverFile = fileRef.toServerFile(s.api.uploadEarlyEndpoint) ?: return@onClick
                photoSignal.value = ServerFileWithMetadata(original = serverFile)
            }
        }
    }
}
