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
import kotlin.uuid.Uuid

@Routable("/chat-rooms")
class ChatRoomsListPage : Page {
    override val title: Reactive<String> get() = Constant("Chat Rooms")

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val showAllRooms = Signal(false)
        val searchQuery = Signal("")

        val allRooms = remember {
            currentSession()?.chatRooms?.list(Query(Condition.Always))?.invoke() ?: emptyList()
        }

        val displayedRooms = remember {
            val rooms = allRooms()
            val userId = currentSession()?.userId
            val query = searchQuery().trim().lowercase()

            rooms.filter { room ->
                // Filter by membership if not showing all rooms
                val membershipMatch = showAllRooms() ||
                        room.createdBy == userId ||
                        userId in room.memberIds

                // Filter by search query
                val searchMatch = query.isEmpty() ||
                        room.name.lowercase().contains(query) ||
                        room.description.lowercase().contains(query)

                membershipMatch && searchMatch
            }
        }

        col {
            col {
                row {
                    expanding.h1("Chat Rooms")
                    button {
                        text {
                            ::content { if (showAllRooms()) "My Rooms" else "Browse All" }
                        }
                        onClick {
                            showAllRooms.value = !showAllRooms.value
                        }
                    }
                }

                // Search field with debounced input
                field("Search") {
                    textInput {
                        hint = "Search rooms..."
                        content bind searchQuery
                    }
                }
            }

            important.buttonTheme.button {
                row {
                    icon(Icon.add, "Add")
                    space()
                    text("Create New Room")
                }
                onClick {
                    dialog { close ->
                        val newRoomName = Signal("")
                        val newRoomDescription = Signal("")

                        val createRoomAction = Action("Create Room") {
                            val s = currentSession() ?: return@Action
                            val room = ChatRoom(
                                name = newRoomName.value,
                                description = newRoomDescription.value,
                                createdBy = s.userId,
                                memberIds = setOf(s.userId)
                            )
                            s.api.chatRoom.insert(room)
                            close()
                        }

                        card.padded.col {
                            h2("Create New Chat Room")

                            col {
                                text("Room Name:")
                                textInput {
                                    content bind newRoomName
                                    hint = "Enter room name"
                                }
                            }

                            space()

                            col {
                                text("Description (optional):")
                                textInput {
                                    content bind newRoomDescription
                                    hint = "Enter description"
                                }
                            }

                            space()

                            row {
                                expanding.button {
                                    text("Cancel")
                                    onClick {
                                        close()
                                    }
                                }
                                space()
                                expanding.important.buttonTheme.button {
                                    text("Create")
                                    ::enabled { newRoomName().isNotBlank() }
                                    action = createRoomAction
                                }
                            }
                        }
                    }
                }
            }

            space()

            expanding.frame {
                shownWhen { displayedRooms().isEmpty() }.centered.text {
                    ::content {
                        if (showAllRooms()) "No rooms available"
                        else "No rooms yet. Create one or browse all rooms!"
                    }
                }

                shownWhen { displayedRooms().isNotEmpty() }.recyclerView {
                    children(displayedRooms, id = { it._id }) { room ->
                        val userId = remember { currentSession()?.userId }
                        val isMember = remember {
                            userId() == room().createdBy || userId() in room().memberIds
                        }
                        val isCreator = remember { userId() == room().createdBy }

                        val toggleMembership = Action("Toggle Membership") {
                            val s = currentSession() ?: return@Action
                            val currentRoom = room()
                            val memberNow = userId() in currentRoom.memberIds

                            if (memberNow) {
                                s.api.chatRoom.leaveAChatRoom(currentRoom._id)
                            } else {
                                s.api.chatRoom.joinAChatRoom(currentRoom._id)
                            }
                        }

                        card.col {
                            // Main content - clickable if member
                            shownWhen { isMember() }.button {
                                col {
                                    row {
                                        expanding.h3 {
                                            ::content { room().name }
                                        }
                                        icon(Icon.chevronRight, "Open")
                                    }
                                    shownWhen { room().description.isNotEmpty() }.text {
                                        ::content { room().description }
                                    }
                                }
                                onClick {
                                    pageNavigator.navigate(ChatRoomPage(room()._id))
                                }
                            }

                            // Non-clickable header if not member
                            shownWhen { !isMember() }.col {
                                row {
                                    expanding.h3 {
                                        ::content { room().name }
                                    }
                                }
                                shownWhen { room().description.isNotEmpty() }.text {
                                    ::content { room().description }
                                }
                            }

                            space()

                            // Status and action row
                            row {
                                expanding.subtext {
                                    ::content {
                                        when {
                                            isCreator() -> "Created by: You"
                                            isMember() -> "Member"
                                            else -> "${room().memberIds.size} members"
                                        }
                                    }
                                }

                                // Join/Leave button
                                shownWhen { !isCreator() }.button {
                                    text {
                                        ::content { if (isMember()) "Leave" else "Join" }
                                    }
                                    action = toggleMembership
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
