package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.Query
import com.lightningkite.lskiteuistarter.sdk.currentSession
import kotlinx.coroutines.launch

@Routable("/chat-rooms")
class ChatRoomsListPage : Page {
    override val title: Reactive<String> get() = Constant("Chat Rooms")

    override fun ViewWriter.render() {
        reactiveScope {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val rooms = remember {
            currentSession()?.chatRooms?.list(Query(Condition.Always))?.invoke() ?: emptyList()
        }

        val showCreateDialog = Signal(false)
        val roomName = Signal("")
        val roomDescription = Signal("")

        col {
            h1("Chat Rooms")
            space()

            // Create room button
            important.buttonTheme.button {
                row {
                    icon { Icon.add }
                    space()
                    text("Create New Room")
                }
                onClick {
                    showCreateDialog.value = true
                }
            }

            space()

            // List of rooms
            shownWhen { rooms().isEmpty() }.centered.text("No chat rooms yet")

            expanding.shownWhen { rooms().isNotEmpty() }.recyclerView {
                children(rooms, id = { it._id }) { room ->
                    card.button {
                        col {
                            text {
                                ::content { room().name }
                            }
                            shownWhen { room().description.isNotBlank() }.subtext {
                                ::content { room().description }
                            }
                        }
                        onClick {
                            pageNavigator.navigate(ChatRoomPage(room()._id))
                        }
                    }
                }
            }
        }

        // Create room dialog
        shownWhen { showCreateDialog() }.dismissBackground {
            onClick { showCreateDialog.value = false }
        }

        shownWhen { showCreateDialog() }.centered.card.padded.col {
            h2("Create Chat Room")
            space()

            textInput {
                content bind roomName
                hint = "Room name"
            }
            space()

            textInput {
                content bind roomDescription
                hint = "Description (optional)"
            }
            space()

            row {
                expanding.button {
                    text("Cancel")
                    onClick { showCreateDialog.value = false }
                }
                space()
                expanding.important.buttonTheme.button {
                    text("Create")
                    ::enabled { roomName().isNotBlank() }
                    onClick {
                        val session = currentSession() ?: return@onClick
                        val newRoom = ChatRoom(
                            name = roomName.value,
                            description = roomDescription.value,
                            createdBy = session.userId,
                            memberIds = setOf(session.userId)
                        )
                        session.api.chatRoom.insert(newRoom)
                        showCreateDialog.value = false
                        roomName.value = ""
                        roomDescription.value = ""
                    }
                }
            }
        }
    }
}
