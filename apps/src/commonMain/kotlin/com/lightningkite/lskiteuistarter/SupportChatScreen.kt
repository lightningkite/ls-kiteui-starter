package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lightningserver.ai.*
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * User-facing support chat screen.
 * Allows users to chat with AI support and view their conversation history.
 */
@Routable("/support")
class SupportChatPage : Page {
    override val title: Reactive<String> get() = Constant("Support Chat")

    override fun ViewWriter.render() {
        val conversations = Signal<List<SystemChatConversation>>(emptyList())

        suspend fun loadConversations() {
            val session = currentSession() ?: return
            conversations.value = session.api.supportChat.conversations.query(
                Query(
                    condition = Condition.Always,
                    orderBy = listOf(SortPart(SystemChatConversation.path.createdAt, ascending = false)),
                    limit = 50
                )
            ).toList()
        }

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        // Initial load
        launch { loadConversations() }

        col {
            h2("Support")

            // Start new conversation button
            important.buttonTheme.button {
                centered.text("New Conversation")
                onClick {
                    val session = currentSession() ?: return@onClick
                    val userId = session.userId.toString()
                    val conversation = session.api.supportChat.conversations.insert(
                        SystemChatConversation(
                            subjectId = userId,
                            name = "Support Chat",
                            createdAt = Clock.System.now()
                        )
                    )
                    pageNavigator.navigate(ChatConversationPage(conversation._id))
                }
            }

            // Refresh button
            row {
                button {
                    text("Refresh")
                    onClick { loadConversations() }
                }
            }

            // List of existing conversations
            expanding.scrolling.col {
                reactiveScope {
                    clearChildren()
                    conversations().forEach { conversation ->
                        card.button {
                            row {
                                expanding.col {
                                    bold.text(conversation.name.takeIf { it.isNotBlank() } ?: "Support Chat")
                                    subtext("Started: ${conversation.createdAt}")
                                }
                            }
                            onClick {
                                pageNavigator.navigate(ChatConversationPage(conversation._id))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single conversation view with message history and input.
 */
@Routable("/support/chat/{conversationId}")
class ChatConversationPage(val conversationId: Uuid) : Page {
    override val title: Reactive<String> get() = Constant("Chat")

    override fun ViewWriter.render() {
        val messages = Signal<List<SystemChatMessage>>(emptyList())
        val messageInput = Signal("")

        suspend fun loadMessages() {
            val session = currentSession() ?: return
            messages.value = session.api.supportChat.messages.query(
                Query(
                    condition = condition { it.conversationId eq conversationId },
                    orderBy = listOf(SortPart(SystemChatMessage.path.createdAt, ascending = true)),
                    limit = 200
                )
            ).toList()
        }

        suspend fun sendMessage() {
            val session = currentSession() ?: return
            val content = messageInput.value.trim()
            if (content.isBlank()) return

            messageInput.value = ""

            session.api.supportChat.messages.insert(
                SystemChatMessage(
                    conversationId = conversationId,
                    subjectId = session.userId.toString(),
                    role = SystemChatMessage.Role.User,
                    content = content,
                    createdAt = Clock.System.now()
                )
            )

            loadMessages()
        }

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        // Initial load
        launch { loadMessages() }

        col {
            // Refresh button
            row {
                button {
                    text("Refresh")
                    onClick { loadMessages() }
                }
            }

            // Message list
            expanding.scrolling.col {
                reactiveScope {
                    clearChildren()
                    messages().forEach { message ->
                        val isUser = message.role == SystemChatMessage.Role.User
                        val isAssistant = message.role == SystemChatMessage.Role.Assistant
                        val isError = message.role == SystemChatMessage.Role.Error

                        // Skip system and thinking messages in user view
                        if (message.role != SystemChatMessage.Role.System && message.role != SystemChatMessage.Role.Thinking) {
                            row {
                                if (isUser) space()

                                card.col {
                                    bold.text(when {
                                        isUser -> "You"
                                        isAssistant -> "Support"
                                        isError -> "Error"
                                        else -> message.role.name
                                    })
                                    text(message.content)
                                }

                                if (!isUser) space()
                            }
                        }
                    }
                }
            }

            // Input area
            row {
                expanding.textInput {
                    hint = "Type your message..."
                    content bind messageInput
                }
                important.buttonTheme.button {
                    text("Send")
                    ::enabled { messageInput().isNotBlank() }
                    onClick {
                        sendMessage()
                    }
                }
            }
        }
    }
}
