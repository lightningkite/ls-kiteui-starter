package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.SortPart
import com.lightningkite.lskiteuistarter.sdk.currentSession
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Routable("/chat-room/{roomId}")
@Serializable
data class ChatRoomPage(val roomId: Uuid) : Page {
    override val title: Reactive<String> get() = Constant("Chat Room")

    override fun ViewWriter.render() {
        reactiveScope {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val room = remember {
            currentSession()?.chatRooms?.get(roomId)?.invoke()
        }

        val messages = remember {
            currentSession()?.messages?.list(
                Query(
                    condition = Condition.Always,
                    orderBy = listOf(SortPart(Message.path.createdAt, ascending = true))
                )
            )?.invoke()?.filter { it.chatRoomId == roomId } ?: emptyList()
        }

        val users = remember {
            currentSession()?.users?.list(Query(Condition.Always))?.invoke() ?: emptyList()
        }

        val messageContent = Signal("")

        col {
            h2 {
                ::content { room()?.name ?: "Loading..." }
            }
            shownWhen { room()?.description?.isNotBlank() == true }.subtext {
                ::content { room()?.description ?: "" }
            }

            separator()

            // Messages list
            expanding.frame {
                shownWhen { messages().isEmpty() }.centered.text("No messages yet. Start the conversation!")

                shownWhen { messages().isNotEmpty() }.recyclerView {
                    children(messages, id = { it._id }) { message ->
                        val author = remember {
                            users().find { it._id == message().authorId }
                        }
                        card.padded.col {
                            row {
                                expanding.subtext {
                                    ::content { author()?.name ?: "Unknown User" }
                                }
                                subtext {
                                    ::content {
                                        message().createdAt.toString().substringBefore('T')
                                    }
                                }
                            }
                            space()
                            text {
                                ::content { message().content }
                            }
                        }
                    }
                }
            }

            separator()

            // Message input
            row {
                expanding.textInput {
                    content bind messageContent
                    hint = "Type a message..."
                }
                space()
                button {
                    icon(Icon.send, "Send")
                    ::enabled { messageContent().isNotBlank() }
                    onClick {
                        val s = currentSession() ?: return@onClick
                        val content = messageContent.value.trim()
                        if (content.isBlank()) return@onClick

                        launch {
                            val message = Message(
                                chatRoomId = roomId,
                                authorId = s.userId,
                                content = content
                            )
                            s.api.message.insert(message)
                            messageContent.value = ""
                        }
                    }
                }
            }
        }
    }
}
