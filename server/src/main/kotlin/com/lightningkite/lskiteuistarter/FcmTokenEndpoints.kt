package com.lightningkite.lskiteuistarter

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
import kotlin.text.get
import kotlin.uuid.Uuid

object FcmTokenEndpoints : ServerBuilder() {
    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val admin = condition<FcmToken>(auth.userRole() >= UserRole.Admin)
            val mine = condition<FcmToken> { it.user eq auth.id }
            ModelPermissions(
                create = admin or mine,
                read = admin or mine,
                update = admin or mine,
                delete = admin or mine,
            )
        })
    val rest = path include ModelRestEndpoints(info)

    val registerEndpoint = path.path("register").post bind ApiHttpHandler(
        summary = "Register Token",
        auth = UserAuth.require(),
        implementation = { id: String ->
            info.table().upsertOne(
                condition { it._id eq id },
                modification { it.user assign auth.id },
                FcmToken(id, auth.id, userAgent = request.headers["User-Agent"]?.root ?: "?")
            )
        }
    )
    val clearEndpoint = rest.detailPath.path("clear").post bind ApiHttpHandler(
        summary = "Clear Token",
        auth = noAuth,
        implementation = { _: Unit ->
            info.table().deleteOneById(request.arg1)
        }
    )

    val testEndpoint = rest.detailPath.path("test").post bind ApiHttpHandler(
        summary = "Test In-App Notifications",
        auth = UserAuth.require(),
        implementation = { _: Unit ->
            val token = info.table().get(this.request.arg1) ?: throw NotFoundException("Token not found in database.")
            if(token.user != auth.untypedId && auth.userRole() < UserRole.Admin) throw ForbiddenException("You don't own this token.")
            Server.notifications().send(
                listOf(token._id), NotificationData(
                    notification = Notification(
                        title = "Test Notification",
                        body = "This is the test notification you requested.",
                        link = Server.webUrl()
                    )
                )
            )
            "Notification sent"
        }
    )
}

