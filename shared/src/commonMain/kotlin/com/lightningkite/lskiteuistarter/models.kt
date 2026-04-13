package com.lightningkite.lskiteuistarter

import com.lightningkite.EmailAddress
import com.lightningkite.services.data.*
import com.lightningkite.services.database.HasId
import com.lightningkite.services.database.TypedId
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@GenerateDataClassPaths
@Serializable
data class AppRelease(
    override val _id: ID = ID(Uuid.random()),
    val version: String,
    val platform: AppPlatform,
    val releaseDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val requiredUpdate: Boolean,
) : HasId<AppRelease.ID> {
    @Serializable
    @JvmInline
    @References(AppRelease::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
@GenerateDataClassPaths
data class User(
    override val _id: ID = ID(Uuid.random()),
    @Index(IndexUniqueness.Unique) val email: EmailAddress,
    val name: String,
    val role: UserRole = UserRole.User,
) : HasId<User.ID> {
    @Serializable
    @JvmInline
    @References(User::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
enum class UserRole {
    User,
    Admin,
    Developer,
    Root
}

@Serializable
@GenerateDataClassPaths
data class FcmToken(
    @MaxLength(160, average = 142) override val _id: String,
    @Index val user: User.ID,
    val active: Boolean = true,
    val created: Instant = now(),
    val lastRegisteredAt: Instant = created,
    val userAgent: String? = null,
) : HasId<String>
