package com.lightningkite.template

import com.lightningkite.EmailAddress
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.files.*
import kotlin.uuid.Uuid
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Clock


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
