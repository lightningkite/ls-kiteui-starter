package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.lightningserver.ai.*
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import com.lightningkite.lskiteuistarter.utils.toRelativeTimeString

/**
 * Admin support dashboard.
 * Shows all user conversations and allows admins to take over from AI.
 */
@Routable("/admin/support")
class AdminSupportPage : Page {
    override val title: Reactive<String> get() = Constant("Support Dashboard")

    val conversations = remember {
        val s = currentSession() ?: throw PlainTextException("Not logged in")
        s.systemChatConversations.list(
            query = Query(
                condition = Condition.Always,
                orderBy = sort {
                    it.updatedAt.descending()
                },
                limit = 100
            ),
            maximumAge = 1.minutes,
            pullFrequency = 1.minutes
        )
    }

    // Cache of user IDs to emails - uses ModelCache for caching + WebSocket updates
    val userEmails = remember {
        val session = currentSession() ?: return@remember emptyMap<String, String>()
        val convs = conversations()()
        val subjectIds = convs.mapNotNull { conv ->
            runCatching { Uuid.parse(conv.subjectId) }.getOrNull()
        }.distinct()

        if (subjectIds.isEmpty()) return@remember emptyMap()

        session.users.list(
            query = Query(
                condition = condition { it._id inside subjectIds },
                limit = subjectIds.size
            ),
            maximumAge = 5.minutes,
            pullFrequency = 5.minutes
        )().associate { it._id.toString() to it.email.raw }
    }

    override fun ViewWriter.render() {

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        col {
            h2("Support Dashboard (Admin)")
            subtext("View and respond to user support conversations")

            // Filter controls
            val searchQuery = Signal("")
            val showNeedsAttentionOnly = Signal(false)

            val filteredConversations = remember {
                val convs = conversations()()
                val query = searchQuery().lowercase()
                val needsAttention = showNeedsAttentionOnly()
                val emails = userEmails()

                convs.filter { conv ->
                    val matchesAttention = !needsAttention || !conv.autoProcess
                    val matchesSearch = query.isBlank() ||
                        emails[conv.subjectId]?.lowercase()?.contains(query) == true ||
                        conv.name.lowercase().contains(query)
                    matchesAttention && matchesSearch
                }
            }

            row {
                expanding.textInput {
                    hint = "Search by email or name..."
                    content bind searchQuery
                }
                row {
                    checkbox { checked bind showNeedsAttentionOnly }
                    text("Needs Attention")
                }
            }

            // List of all conversations
            expanding.recyclerView {
                reactive {
                    if(lastIndex() > conversations().limit - 20) {
                        conversations().limit = lastIndex() + 40
                    }
                }
                children(items = remember { filteredConversations() }, id = { it._id }) { conversation ->
                    card.button {
                        row {
                            expanding.col {
                                bold.text {
                                    ::content { conversation().name.takeIf { it.isNotBlank() } ?: "Support Chat" }
                                }
                                row {
                                    subtext {
                                        ::content {
                                            val subjectId = conversation().subjectId
                                            val email = userEmails()[subjectId]
                                            "User: ${email ?: subjectId}"
                                        }
                                    }
                                }
                                row {
                                    subtext{
                                        ::content { "Last updated: ${conversation().updatedAt.toRelativeTimeString()}" }
                                    }
                                    text {
                                        ::content {
                                            if (!conversation().autoProcess) " (Human takeover)" else ""
                                        }
                                    }
                                }
                            }
                        }
                        onClick {
                            pageNavigator.navigate(AdminChatConversationPage(conversation()._id))
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

    val messages = remember {
        val s = currentSession() ?: throw PlainTextException("Not logged in")
        s.systemChatMessages.list(
            query = Query(
                condition = condition { it.conversationId eq conversationId },
                orderBy = sort {
                    it.createdAt.ascending()
                },
                limit = 500
            ),
            maximumAge = 30.seconds,
            pullFrequency = 5.seconds
        )
    }

    override fun ViewWriter.render() {
        val conversation = Signal<SystemChatConversation?>(null)
        val userEmail = Signal<String?>(null)
        val messageInput = Signal("")

        suspend fun loadConversation() {
            val session = currentSession() ?: return
            val conv = session.api.supportChat.conversations.detail(conversationId)
            conversation.value = conv

            // Load user email using ModelCache
            val userId = runCatching { Uuid.parse(conv.subjectId) }.getOrNull()
            if (userId != null) {
                val user = session.users[userId]()
                userEmail.value = user?.email?.raw
            }
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

            loadConversation()
        }

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        // Initial load
        launch {
            loadConversation()
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
                subtext { ::content {
                    val email = userEmail()
                    val subjectId = conversation()?.subjectId
                    "User: ${email ?: subjectId ?: "Unknown"}"
                } }
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
            }

            // Message list
            expanding.scrolling.col {
                reactiveScope {
                    clearChildren()
                    messages()().forEach { message ->
                        renderAdminMessage(message)
                    }
                }
            }

            // Input area (admin response)
            row {
                val sendAction = Action("Send") { sendAdminMessage() }
                expanding.textInput {
                    hint = "Type response as human support..."
                    content bind messageInput
                    action = sendAction
                }
                important.buttonTheme.button {
                    text("Send as Support")
                    ::enabled { messageInput().isNotBlank() }
                    action = sendAction
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
                    subtext(message.createdAt.toRelativeTimeString())
                }
                text(message.content)
            }

            if (!isUser) space() // Push non-user messages to the left
        }
    }
}
