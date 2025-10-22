package com.lightningkite.lskiteuistarter

import com.lightningkite.*
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.definition.builder.*
import com.lightningkite.lightningserver.deprecations.*
import com.lightningkite.lightningserver.encryption.*
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.pathing.*
import com.lightningkite.lightningserver.runtime.*
import com.lightningkite.lightningserver.serialization.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lightningserver.settings.*
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.websockets.*
import com.lightningkite.services.cache.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.email.*
import com.lightningkite.services.files.*
import com.lightningkite.services.notifications.*
import com.lightningkite.services.sms.*
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import kotlin.uuid.Uuid

object UserEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val allowedRoles = UserRole.entries.filter { it <= auth.userRole() }
            val admin: Condition<User> =
                if (this.auth.userRole() >= UserRole.Admin) condition { it.role inside allowedRoles } else Condition.Never
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

    val rest = path include ModelRestEndpoints(info)
//    val socketUpdates = ModelRestUpdatesWebsocket(path, Server.database, info)

    val initAdminUser = path.path("initAdminUser") bind startupOnce(Server.database) {
        println("Adding user")
        val email = "joseph+root@lightningkite.com".toEmailAddress()
        info.table().deleteMany(condition { it.email.eq(email) })
        info.table().insertOne(
            User(
                _id = Uuid.fromLongs(0L, 10L),
                email = email,
                name = "Joseph Root",
                role = UserRole.Root
            )
        )
    }
}