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
 * Admin support dashboard.
 * Shows all user conversations and allows admins to take over from AI.
 */
@Routable("/admin/support")
class AdminSupportPage : Page {
    override val title: Reactive<String> get() = Constant("Support Dashboard")

    override fun ViewWriter.render() {
        val conversations = Signal<List<SystemChatConversation>>(emptyList())

        suspend fun loadConversations() {
            val session = currentSession() ?: return
            conversations.value = session.api.supportChat.conversations.query(
                Query(
                    condition = Condition.Always,
                    orderBy = listOf(SortPart(SystemChatConversation.path.updatedAt, ascending = false)),
                    limit = 100
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
            h2("Support Dashboard (Admin)")
            subtext("View and respond to user support conversations")

            // Refresh button
            row {
                button {
                    text("Refresh")
                    onClick { loadConversations() }
                }
            }

            // List of all conversations
            expanding.scrolling.col {
                reactiveScope {
                    clearChildren()
                    conversations().forEach { conversation ->
                        card.button {
                            row {
                                expanding.col {
                                    bold.text(conversation.name.takeIf { it.isNotBlank() } ?: "Support Chat")
                                    row {
                                        subtext("User: ${conversation.subjectId}")
                                    }
                                    row {
                                        subtext("Last updated: ${conversation.updatedAt}")
                                        text(if (!conversation.autoProcess) " (Human takeover)" else "")
                                    }
                                }
                            }
                            onClick {
                                pageNavigator.navigate(AdminChatConversationPage(conversation._id))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Admin view of a single conversation.
 * Allows admins to respond as human support, which disables AI auto-response.
 */
@Routable("/admin/support/chat/{conversationId}")
class AdminChatConversationPage(val conversationId: Uuid) : Page {
    override val title: Reactive<String> get() = Constant("Support Chat (Admin)")

    override fun ViewWriter.render() {
        val conversation = Signal<SystemChatConversation?>(null)
        val messages = Signal<List<SystemChatMessage>>(emptyList())
        val messageInput = Signal("")

        suspend fun loadConversation() {
            val session = currentSession() ?: return
            conversation.value = session.api.supportChat.conversations.detail(conversationId)
        }

        suspend fun loadMessages() {
            val session = currentSession() ?: return
            messages.value = session.api.supportChat.messages.query(
                Query(
                    condition = condition { it.conversationId eq conversationId },
                    orderBy = listOf(SortPart(SystemChatMessage.path.createdAt, ascending = true)),
                    limit = 500
                )
            ).toList()
        }

        suspend fun toggleAutoProcess() {
            val session = currentSession() ?: return
            val conv = conversation.value ?: return

            session.api.supportChat.conversations.replace(
                conv._id,
                conv.copy(autoProcess = !conv.autoProcess)
            )

            loadConversation()
        }

        suspend fun sendAdminMessage() {
            val session = currentSession() ?: return
            val content = messageInput.value.trim()
            if (content.isBlank()) return

            messageInput.value = ""

            // First, disable AI auto-process if it's enabled
            val conv = conversation.value
            if (conv?.autoProcess == true) {
                session.api.supportChat.conversations.replace(
                    conv._id,
                    conv.copy(autoProcess = false)
                )
            }

            // Insert the admin response as an Assistant message
            session.api.supportChat.messages.insert(
                SystemChatMessage(
                    conversationId = conversationId,
                    subjectId = conv?.subjectId ?: "",
                    role = SystemChatMessage.Role.Assistant,
                    content = content,
                    createdAt = Clock.System.now(),
                    skipAutoResponse = true // Don't trigger AI response
                )
            )

            // Refresh
            loadConversation()
            loadMessages()
        }

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        // Initial load
        launch {
            loadConversation()
            loadMessages()
        }

        col {
            // Header with conversation info
            card.col {
                row {
                    expanding.bold.text { ::content { conversation()?.name?.takeIf { it.isNotBlank() } ?: "Support Chat" } }
                    // Show AI status
                    text { ::content {
                        val autoProcess = conversation()?.autoProcess ?: true
                        if (autoProcess) "AI Active" else "Human Control"
                    } }
                }
                subtext { ::content { "User: ${conversation()?.subjectId ?: "Unknown"}" } }
            }

            // Toggle AI button
            row {
                button {
                    text { ::content {
                        val autoProcess = conversation()?.autoProcess ?: true
                        if (autoProcess) "Disable AI (Take Over)" else "Re-enable AI"
                    } }
                    onClick {
                        toggleAutoProcess()
                    }
                }
                button {
                    text("Refresh")
                    onClick {
                        loadConversation()
                        loadMessages()
                    }
                }
            }

            // Message list
            expanding.scrolling.col {
                reactiveScope {
                    clearChildren()
                    messages().forEach { message ->
                        renderAdminMessage(message)
                    }
                }
            }

            // Input area (admin response)
            row {
                expanding.textInput {
                    hint = "Type response as human support..."
                    content bind messageInput
                }
                important.buttonTheme.button {
                    text("Send as Support")
                    ::enabled { messageInput().isNotBlank() }
                    onClick {
                        sendAdminMessage()
                    }
                }
            }
        }
    }

    private fun ViewWriter.renderAdminMessage(message: SystemChatMessage) {
        val isUser = message.role == SystemChatMessage.Role.User
        val isAssistant = message.role == SystemChatMessage.Role.Assistant
        val isSystem = message.role == SystemChatMessage.Role.System
        val isError = message.role == SystemChatMessage.Role.Error
        val isThinking = message.role == SystemChatMessage.Role.Thinking
        val isSummary = message.role == SystemChatMessage.Role.Summary

        row {
            if (isUser) space() // Push user messages to the right

            card.col {
                row {
                    bold.text(when {
                        isUser -> "User"
                        isAssistant -> "AI/Support"
                        isSystem -> "System"
                        isThinking -> "AI Thinking"
                        isSummary -> "Summary"
                        isError -> "Error"
                        else -> message.role.name
                    })
                    space()
                    subtext(message.createdAt.toString())
                }
                text(message.content)
            }

            if (!isUser) space() // Push non-user messages to the left
        }
    }
}
