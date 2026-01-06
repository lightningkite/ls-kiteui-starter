package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.lightningserver.ai.*
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*
import com.lightningkite.services.files.ServerFile
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import com.lightningkite.lskiteuistarter.utils.toRelativeTimeString

/**
 * User-facing support chat screen.
 * Allows users to chat with AI support and view their conversation history.
 */
@Routable("/support")
class SupportChatPage : Page {
    override val title: Reactive<String> get() = Constant("Support Chat")

    val conversations = remember {
        val s = currentSession() ?: throw PlainTextException("Not logged in")
        s.systemChatConversations.list(
            query = Query(
                condition = Condition.Always,
                orderBy = sort {
                    it.createdAt.descending()
                },
                limit = 50
            ),
            maximumAge = 1.minutes,
            pullFrequency = 1.minutes
        )
    }

    override fun ViewWriter.render() {

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

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

            // List of existing conversations
            expanding.recyclerView {
                reactive {
                    if (lastIndex() > conversations().limit - 20) {
                        conversations().limit = lastIndex() + 40
                    }
                }
                children(items = remember { conversations()() }, id = { it._id }) { conversation ->
                    card.row {
                        expanding.button {
                            col {
                                bold.text {
                                    ::content { conversation().name.takeIf { it.isNotBlank() } ?: "Support Chat" }
                                }
                                subtext {
                                    ::content { "Started: ${conversation().createdAt.toRelativeTimeString()}" }
                                }
                            }
                            onClick {
                                pageNavigator.navigate(ChatConversationPage(conversation()._id))
                            }
                        }
                        button {
                            text("Edit")
                            onClick {
                                val conv = conversation()
                                dialog { close ->
                                    val newName = Signal(conv.name)
                                    card.col {
                                        h3("Rename Conversation")
                                        field("Name") {
                                            val saveAction = Action("Save") {
                                                val session = currentSession() ?: return@Action
                                                session.api.supportChat.conversations.replace(
                                                    conv._id,
                                                    conv.copy(name = newName.value.trim())
                                                )
                                                close()
                                            }
                                            textInput {
                                                hint = "Conversation name"
                                                content bind newName
                                                action = saveAction
                                            }
                                        }
                                        row {
                                            button {
                                                text("Cancel")
                                                onClick { close() }
                                            }
                                            space()
                                            important.button {
                                                text("Save")
                                                action = Action("Save") {
                                                    val session = currentSession() ?: return@Action
                                                    session.api.supportChat.conversations.replace(
                                                        conv._id,
                                                        conv.copy(name = newName.value.trim())
                                                    )
                                                    close()
                                                }
                                            }
                                        }
                                    }
                                }
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

    val messages = remember {
        val s = currentSession() ?: throw PlainTextException("Not logged in")
        s.systemChatMessages.list(
            query = Query(
                condition = condition { it.conversationId eq conversationId },
                orderBy = sort {
                    it.createdAt.ascending()
                },
                limit = 200
            ),
            maximumAge = 30.seconds,
            pullFrequency = 5.seconds
        )
    }

    override fun ViewWriter.render() {
        val messageInput = Signal("")
        val pendingAttachments = Signal<List<ServerFile>>(emptyList())

        suspend fun sendMessage() {
            val session = currentSession() ?: return
            val inputContent = messageInput.value.trim()
            val attachments = pendingAttachments.value

            // Allow sending if there's content or attachments
            if (inputContent.isBlank() && attachments.isEmpty()) return

            messageInput.value = ""
            pendingAttachments.value = emptyList()

            session.api.supportChat.messages.insert(
                SystemChatMessage(
                    conversationId = conversationId,
                    subjectId = session.userId.toString(),
                    role = SystemChatMessage.Role.User,
                    content = inputContent,
                    attachments = attachments,
                    createdAt = Clock.System.now()
                )
            )
        }

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        col {
            // Message list
            expanding.scrolling.col {
                reactiveScope {
                    clearChildren()
                    messages()().forEach { message ->
                        // Skip system, thinking, and summary messages in user view
                        if (message.role != SystemChatMessage.Role.System &&
                            message.role != SystemChatMessage.Role.Thinking &&
                            message.role != SystemChatMessage.Role.Summary) {

                            val isUser = message.role == SystemChatMessage.Role.User
                            val isAssistant = message.role == SystemChatMessage.Role.Assistant
                            val isError = message.role == SystemChatMessage.Role.Error

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
                                    // Display attachments
                                    if (message.attachments.isNotEmpty()) {
                                        row {
                                            message.attachments.forEach { file ->
                                                button {
                                                    row {
                                                        text("📎")
                                                        text(file.location.substringAfterLast('/').take(20))
                                                    }
                                                    onClick {
                                                        context.openLink(file.location)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!isUser) space()
                            }
                        }
                    }
                }
            }

            // Pending attachments display
            shownWhen { pendingAttachments().isNotEmpty() }.row {
                subtext { ::content { "${pendingAttachments().size} file(s) attached" } }
                button {
                    text("Clear")
                    onClick { pendingAttachments.value = emptyList() }
                }
            }

            // Input area
            row {
                val sendAction = Action("Send") { sendMessage() }
                button {
                    text("📎")
                    onClick {
                        val session = currentSession() ?: return@onClick
                        val file = context.requestFile(listOf("*/*")) ?: return@onClick
                        launch {
                            val upload = session.api.uploadEarlyEndpoint.uploadFileForRequest()
                            val response = fetch(upload.uploadUrl, HttpMethod.PUT, body = file)
                            if (response.ok) {
                                pendingAttachments.value = pendingAttachments.value + ServerFile(upload.futureCallToken)
                            }
                        }
                    }
                }
                expanding.textInput {
                    hint = "Type your message..."
                    content bind messageInput
                    action = sendAction
                }
                important.buttonTheme.button {
                    text("Send")
                    ::enabled { messageInput().isNotBlank() || pendingAttachments().isNotEmpty() }
                    action = sendAction
                }
            }
        }
    }
}
