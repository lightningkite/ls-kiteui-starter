package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.encryption.secureHash
import com.lightningkite.lightningserver.runtime.now
import com.lightningkite.lightningserver.sessions.PasswordSecret
import com.lightningkite.lightningserver.sessions.subjectId
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.ModelRestEndpointsAndUpdatesWebsocket.Companion.plus
import com.lightningkite.lightningserver.typed.ModelRestUpdatesWebsocket
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lightningserver.typed.startupOnce
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.lskiteuistarter._id
import com.lightningkite.lskiteuistarter.email
import com.lightningkite.lskiteuistarter.role
import com.lightningkite.services.data.EmailAddress
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.ModelPermissions
import com.lightningkite.services.database.Table
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.findOne
import com.lightningkite.services.database.insertOne
import com.lightningkite.services.database.inside
import com.lightningkite.services.database.or
import com.lightningkite.services.database.updateRestrictions
import kotlin.uuid.Uuid

object UserEndpoints : ServerBuilder() {
    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        tableName = "User",
        permissions = {
            val allowedRoles = UserRole.entries.filter { it <= auth.userRole() }

            val admin: Condition<User> =
                if (auth.userRole() >= UserRole.Admin) condition { it.role inside allowedRoles }
                else Condition.Never

            val self = condition<User> { it._id eq auth.id }

            ModelPermissions(
                create = admin,
                read = admin or self,
                update = admin or self,
                updateRestrictions = updateRestrictions {
                    it.role.requires(admin) { it.inside(allowedRoles) }
                },
                delete = admin or self,
            )
        }
    )

    val User.Companion.info get() = this@UserEndpoints.info

    val rest = path.path("rest") include ModelRestEndpoints(info) + ModelRestUpdatesWebsocket(info, User_email)


    private suspend fun Table<User>.init(user: User): User =
        findOne(condition { it.email.eq(user.email) }) ?: insertOne(user)
            ?: throw IllegalStateException("Failed to insert user ${user.email}")

    private val appStoreTesterId = User.ID(Uuid.parse("f00ffbaa-abf9-497d-a75a-442f1c77c1e9"))
    val User.ID.Companion.AppStoreTester get() = appStoreTesterId

    val initAppStoreTester = path.path("initAppStoreTester") bind startupOnce(Server.database) {
        val user = info.table().init(
            User(
                _id = User.ID.AppStoreTester,
                email = "appstoretester@lightningkite.com".toEmailAddress(),
                name = "AppStoreTester",
                role = UserRole.User
            )
        )
        UserAuth.password.modelInfo.table().run {
            findOne(condition { it.subjectId eq user._id.toString() }) ?: insertOne(
                PasswordSecret(
                    subjectId = user._id.toString(),
                    subjectType = "User",
                    hash = "r[cFRPyBkRqY".secureHash(),
                    hint = "Would have been given to you via separate instructions",
                    establishedAt = now()
                )
            )!!
        }
    }

    val initAdminUsers = path.path("initAdminUser") bind startupOnce(Server.database) {
        suspend fun root(
            email: EmailAddress,
            id: Uuid = Uuid.random()
        ): User = info.table().init(
            User(
                _id = User.ID(id),
                email = email,
                name = email.raw.substringBefore('@').substringBefore('+'),
                role = UserRole.Root
            )
        )
    }
}