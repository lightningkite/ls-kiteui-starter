package com.lightningkite.lskiteuistarter

import com.lightningkite.EmailAddress
import com.lightningkite.services.data.GenerateDataClassPaths
import com.lightningkite.services.data.Index
import com.lightningkite.services.data.MaxLength
import com.lightningkite.services.data.References
import com.lightningkite.services.database.HasId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid


@Serializable
enum class AppPlatform {
    iOS,
    Android,
    Web,
    Desktop,
    ;

    companion object
}

@GenerateDataClassPaths
@Serializable
data class AppRelease(
    override val _id: Uuid = Uuid.random(),
    val version: String,
    val platform: AppPlatform,
    val releaseDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val requiredUpdate: Boolean,
) : HasId<Uuid>

@Serializable
@GenerateDataClassPaths
data class User(
    override val _id: Uuid = Uuid.random(),
    val email: EmailAddress,
    val name: String = "No Name Specified",
    val role: UserRole = UserRole.User,
) : HasId<Uuid>

@Serializable
enum class UserRole {
    NoOne,
    User,
    Admin,
    Developer,
    Root
}

@Serializable
@GenerateDataClassPaths
data class FcmToken(
    @MaxLength(160, 142) override val _id: String,
    @Index @References(User::class) val user: Uuid,
    val active: Boolean = true,
    val created: Instant = Clock.System.now(),
    val lastRegisteredAt: Instant = created,
    val userAgent: String? = null,
) : HasId<String>

@Serializable
@GenerateDataClassPaths
data class ChatRoom(
    override val _id: Uuid = Uuid.random(),
    val name: String,
    val description: String = "",
    @Index @References(User::class) val createdBy: Uuid,
    val createdAt: Instant = Clock.System.now(),
    val memberIds: Set<Uuid> = setOf(),
    val isPrivate: Boolean = false,
    val muteNotifications: Boolean = false,
    val autoDeleteDays: Int? = null,  // null = never, 1, 7, 30
) : HasId<Uuid>

@Serializable
@GenerateDataClassPaths
data class Message(
    override val _id: Uuid = Uuid.random(),
    @Index @References(ChatRoom::class) val chatRoomId: Uuid,
    @Index @References(User::class) val authorId: Uuid,
    val content: String,
    val createdAt: Instant = Clock.System.now(),
    val editedAt: Instant? = null,
) : HasId<Uuid>

@Serializable
data class SendMessageRequest(
    val chatRoomId: Uuid,
    val content: String
)

@Serializable
data class EditMessageRequest(
    val messageId: Uuid,
    val newContent: String
)
