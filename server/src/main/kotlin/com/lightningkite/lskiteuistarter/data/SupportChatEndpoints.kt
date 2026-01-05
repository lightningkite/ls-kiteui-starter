package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.ai.*
import com.lightningkite.lightningserver.auth.fetch
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.ServerSetting
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.typed.AuthAccess
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lskiteuistarter.Server
import com.lightningkite.lskiteuistarter.User
import com.lightningkite.lskiteuistarter.UserAuth
import com.lightningkite.lskiteuistarter.UserRole
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.ai.koog.LLMClientAndModel
import com.lightningkite.services.database.*
import kotlin.uuid.Uuid

/**
 * AI-powered support chat for users.
 *
 * Features:
 * - Users can chat with AI for support questions
 * - Admins can see all conversations and take over from AI
 * - When an admin responds, AI auto-response is disabled
 */
class SupportChatEndpoints(
    database: ServerSetting<Database.Settings, Database>,
    override val defaultLlm: ServerSetting<LLMClientAndModel.Settings, LLMClientAndModel>,
) : LLMChatEndpoints<User>(
    database = database,
    authRequirement = UserAuth.require(),
    conversationPermissions = {
        val userId = authOrNull?.rawId?.toString() ?: ""
        val isAdmin = (authOrNull?.userRole() ?: UserRole.NoOne) >= UserRole.Admin
        ModelPermissions(
            create = Condition.Always,
            read = if (isAdmin) Condition.Always else condition { it.subjectId eq userId },
            update = if (isAdmin) Condition.Always else condition { it.subjectId eq userId },
            delete = if (isAdmin) Condition.Always else condition { it.subjectId eq userId },
        )
    },
    messagePermissions = {
        val userId = authOrNull?.rawId?.toString() ?: ""
        val isAdmin = (authOrNull?.userRole() ?: UserRole.NoOne) >= UserRole.Admin
        ModelPermissions(
            // Only the conversation owner can create messages (unless admin)
            create = if (isAdmin) Condition.Always else condition { it.subjectId eq userId },
            read = if (isAdmin) Condition.Always else condition { it.subjectId eq userId },
            // Messages cannot be updated or deleted
            update = Condition.Never,
            delete = Condition.Never,
        )
    },
) {
    // No database tools for now - this is purely a support chat
    override val tools: Map<String, ChatTool<User, *>> = emptyMap()

    context(serverRuntime: ServerRuntime)
    override suspend fun promptPreMessages(
        builder: PromptBuilderAlt,
        auth: AuthAccess<User>,
        conversation: SystemChatConversation
    ) = with(builder) {
        system("""
            You are a helpful customer support assistant. Your role is to:
            - Answer user questions politely and professionally
            - Help users understand features and functionality
            - Troubleshoot common issues
            - Escalate complex issues by suggesting they contact human support

            Be friendly, concise, and helpful. If you don't know the answer to something,
            be honest about it and suggest they wait for a human support agent to respond.

            Important: If the user's issue requires human intervention (account issues,
            billing problems, complex technical issues), let them know that a human
            support agent will review their conversation and respond soon.
        """.trimIndent())
    }

    context(serverRuntime: ServerRuntime)
    override suspend fun promptPostMessages(
        builder: PromptBuilderAlt,
        auth: AuthAccess<User>,
        conversation: SystemChatConversation
    ) = with(builder) {
        val user = auth.auth.fetch()
        system("You are assisting: ${user.name} (${user.email})")
    }
}
