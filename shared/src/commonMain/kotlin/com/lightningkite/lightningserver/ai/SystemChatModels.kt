package com.lightningkite.lightningserver.ai

import com.lightningkite.services.data.GenerateDataClassPaths
import com.lightningkite.services.data.IndexSet
import com.lightningkite.services.data.References
import com.lightningkite.services.database.HasId
import com.lightningkite.services.files.ServerFile
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A distributed lock for response processing.
 */
@Serializable
@GenerateDataClassPaths
data class ProcessingLock(
    val holderId: String,
    val acquiredAt: Instant,
)

/**
 * A distributed lock for tool execution.
 */
@Serializable
@GenerateDataClassPaths
data class ToolExecutionLock(
    val holderId: String,
    val acquiredAt: Instant,
)

/**
 * Record of tool approval decision.
 */
@Serializable
@GenerateDataClassPaths
data class ToolApproval(
    val approved: Boolean,
    val approvedBy: String,
    val approvedAt: Instant,
    val reason: String? = null,
)

/**
 * Tool authorization for a conversation.
 */
@Serializable
@GenerateDataClassPaths
data class ToolAuthorization(
    val toolName: String,
    val authorizedBy: String,
    val authorizedAt: Instant,
    val expiresAt: Instant? = null,
)

/**
 * Data for tool request messages.
 */
@Serializable
@GenerateDataClassPaths
data class ToolRequestData(
    val toolName: String,
    val arguments: String,
    val requiresApproval: Boolean = false,
    val approvalReason: String? = null,
    val approval: ToolApproval? = null,
    val executionLock: ToolExecutionLock? = null,
    val result: String? = null,
    val error: String? = null,
)

/**
 * A conversation in the system chat.
 */
@Serializable
@GenerateDataClassPaths
@IndexSet(["subjectId", "createdAt"])
data class SystemChatConversation(
    override val _id: Uuid = Uuid.random(),
    val subjectId: String,
    val name: String = "",
    val autoProcess: Boolean = true,
    val processingLock: ProcessingLock? = null,
    val toolAuthorizations: List<ToolAuthorization> = emptyList(),
    val summaryUpTo: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
) : HasId<Uuid>

/**
 * A single message in a system chat conversation.
 */
@Serializable
@GenerateDataClassPaths
@IndexSet(["conversationId", "createdAt"])
@IndexSet(["subjectId", "createdAt"])
data class SystemChatMessage(
    override val _id: Uuid = Uuid.random(),
    @References(SystemChatConversation::class)
    val conversationId: Uuid,
    val subjectId: String,
    val role: Role,
    val channel: String? = null,
    val externalIdentifier: String? = null,
    val content: String,
    val attachments: List<ServerFile> = emptyList(),
    val createdAt: Instant,
    val tool: ToolRequestData? = null,
    val skipAutoResponse: Boolean = false,
) : HasId<Uuid> {

    @Serializable
    enum class Role {
        User,
        Assistant,
        System,
        ToolRequest,
        Thinking,
        Error,
        Summary
    }
}

/**
 * Request for approving/rejecting a tool request.
 */
@Serializable
data class ToolApprovalRequest(
    val approved: Boolean,
    val reason: String? = null,
)

/**
 * Request for authorizing a tool.
 */
@Serializable
data class AuthorizeToolRequest(
    val toolName: String,
    val durationSeconds: Long? = null,
)
