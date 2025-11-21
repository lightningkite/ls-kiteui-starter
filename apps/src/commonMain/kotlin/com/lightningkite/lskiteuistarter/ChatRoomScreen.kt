package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.SendMessageRequest
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Routable("/chat-room/{roomId}")
class ChatRoomPage(val roomId: Uuid) : Page {
    override val title: Reactive<String> get() = Constant("Chat Room")

    val room = remember {
        currentSession()?.chatRooms?.get(roomId)?.invoke()
    }

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val newMessageText = Signal("")

        val sendMessage = Action("Send Message") {
            if (newMessageText.value.isBlank()) return@Action

            val s = currentSession() ?: return@Action
            s.api.message.sendAMessageToAChatRoom(SendMessageRequest(
                chatRoomId = roomId,
                content = newMessageText.value
            ))
            newMessageText.value = ""
        }

        col {
            // Header
            row {
                button {
                    icon(Icon.arrowBack, "Back")
                    onClick {
                        pageNavigator.goBack()
                    }
                }
                space()
                expanding.h2 {
                    ::content { room()?.name ?: "Loading..." }
                }
                // Settings button (only for room creator)
                val isCreator = remember { currentSession()?.userId == room()?.createdBy }
                shownWhen { isCreator() }.button {
                    icon(Icon.settings, "Settings")
                    onClick {
                        val currentRoom = room() ?: return@onClick
                        dialog { close ->
                            // Local settings state
                            val localIsPrivate = Signal(currentRoom.isPrivate)
                            val localMuteNotifications = Signal(currentRoom.muteNotifications)
                            val localAutoDeleteDays = Signal(currentRoom.autoDeleteDays)

                            val saveSettings = Action("Save Settings") {
                                val s = currentSession() ?: return@Action
                                s.api.chatRoom.bulkReplace(listOf(
                                    currentRoom.copy(
                                        isPrivate = localIsPrivate.value,
                                        muteNotifications = localMuteNotifications.value,
                                        autoDeleteDays = localAutoDeleteDays.value
                                    )
                                ))
                                close()
                            }

                            card.padded.col {
                                h2("Room Settings")

                                space()

                                // Switch for isPrivate
                                row {
                                    expanding.col {
                                        text("Private Room")
                                        subtext("Only members can see this room")
                                    }
                                    space()
                                    switch {
                                        checked bind localIsPrivate
                                    }
                                }

                                space()

                                // Checkbox for muteNotifications
                                row {
                                    expanding.col {
                                        text("Mute Notifications")
                                        subtext("Disable notifications for this room")
                                    }
                                    space()
                                    checkbox {
                                        checked bind localMuteNotifications
                                    }
                                }

                                space()

                                // Radio buttons for autoDeleteDays
                                col {
                                    text("Auto-delete Messages")
                                    space()

                                    val autoDeleteOptions = listOf(
                                        null to "Never",
                                        1 to "After 1 day",
                                        7 to "After 7 days",
                                        30 to "After 30 days"
                                    )

                                    autoDeleteOptions.forEach { (days, label) ->
                                        button {
                                            text {
                                                ::content {
                                                    val selected = if (localAutoDeleteDays.value == days) "[X] " else "[ ] "
                                                    selected + label
                                                }
                                            }
                                            onClick {
                                                localAutoDeleteDays.value = days
                                            }
                                        }
                                        space()
                                    }
                                }

                                space()

                                // Action buttons
                                row {
                                    expanding.button {
                                        text("Cancel")
                                        onClick {
                                            close()
                                        }
                                    }
                                    space()
                                    expanding.important.buttonTheme.button {
                                        text("Save")
                                        action = saveSettings
                                    }
                                }
                            }
                        }
                    }
                }
            }

            separator()

            // Messages area
            expanding.frame {
                val messages = remember {
                    currentSession()?.messages?.list(Query(condition { it.chatRoomId eq roomId }))?.invoke()
                        ?.sortedBy { it.createdAt } ?: emptyList()
                }

                val users = remember {
                    currentSession()?.users?.list(Query(Condition.Always))?.invoke()?.associateBy { it._id }
                        ?: emptyMap()
                }

                shownWhen { messages().isEmpty() }.centered.text("No messages yet. Be the first to send one!")
                shownWhen { !messages().isEmpty() }.recyclerView {
                    children(messages, id = { it._id }) { message ->
                        val isAuthor = remember { currentSession()?.userId == message().authorId }

                        val editMessage = Action("Edit Message") {
                            val s = currentSession() ?: return@Action
                            val currentMessage = message()  // Capture the message value
                            dialog { close ->
                                val editedContent = Signal(currentMessage.content)

                                val saveEdit = Action("Save Edit") {
                                    s.api.message.editAMessage(EditMessageRequest(
                                        messageId = currentMessage._id,
                                        newContent = editedContent.value
                                    ))
                                    close()
                                }

                                card.padded.col {
                                    h2("Edit Message")
                                    space()

                                    textInput {
                                        content bind editedContent
                                        hint = "Message content"
                                    }

                                    space()

                                    row {
                                        expanding.button {
                                            text("Cancel")
                                            onClick { close() }
                                        }
                                        space()
                                        expanding.important.buttonTheme.button {
                                            text("Save")
                                            ::enabled { editedContent().isNotBlank() }
                                            action = saveEdit
                                        }
                                    }
                                }
                            }
                        }

                        col {
                            card.padded.col {
                                row {
                                    expanding.subtext {
                                        ::content { users()[message().authorId]?.name ?: "Unknown" }
                                    }
                                    // Show edit button only for author
                                    shownWhen { isAuthor() }.button {
                                        text("Edit")
                                        action = editMessage
                                    }
                                }
                                space()
                                text {
                                    ::content { message().content }
                                }
                                space()
                                row {
                                    subtext {
                                        ::content { message().createdAt.toString() }
                                    }
                                    // Show "edited" indicator if message was edited
                                    shownWhen { message().editedAt != null }.space()
                                    shownWhen { message().editedAt != null }.subtext("(edited)")
                                }
                            }
                            space()
                        }
                    }
                }
            }

            separator()

            // Message input
            padded.row {
                expanding.textInput {
                    content bind newMessageText
                    hint = "Type a message..."
                    action = sendMessage
                }
                space()
                important.buttonTheme.button {
                    icon(Icon.send, "Send")
                    ::enabled { newMessageText().isNotBlank() }
                    action = sendMessage
                }
            }
        }
    }
}
