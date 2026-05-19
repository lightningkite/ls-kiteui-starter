// Shared data models. All models need @Serializable and @GenerateDataClassPaths. — by Claude
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.media.ServerFileWithMetadata
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
    @MaxLength(160, average = 142) override val _id: ID,
    @Index val user: User.ID,
    val active: Boolean = true,
    val created: Instant = now(),
    val lastRegisteredAt: Instant = created,
    val userAgent: String? = null,
) : HasId<FcmToken.ID> {
    @Serializable
    @JvmInline
    @References(FcmToken::class)
    value class ID(override val raw: String) : TypedId<String, ID> {
        override fun toString(): String = raw
    }
}

@Serializable
enum class MemberRole {
    NoOne,
    Member,
    Admin,
    Owner,
}

@Serializable
@GenerateDataClassPaths
data class Organization(
    override val _id: ID = ID(Uuid.random()),
    val name: String,
    val logo: ServerFileWithMetadata? = null,
    val createdAt: Instant = Clock.System.now(),
) : HasId<Organization.ID> {
    @Serializable
    @JvmInline
    @References(Organization::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
@GenerateDataClassPaths
data class Membership(
    override val _id: ID = ID(Uuid.random()),
    @Index val organization: Organization.ID,
    @Index val user: User.ID,
    val role: MemberRole = MemberRole.Member,
    val deactivatedAt: Instant? = null,
    val createdAt: Instant = Clock.System.now(),
) : HasId<Membership.ID> {

    @Serializable
    @JvmInline
    @References(Membership::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
enum class FeatureFlag {
    // Add your project's feature flags here
}
