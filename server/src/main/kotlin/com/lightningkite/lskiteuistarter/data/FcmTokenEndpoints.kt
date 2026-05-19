package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.ForbiddenException
import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.noAuth
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.http.post
import com.lightningkite.lightningserver.pathing.arg1
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*
import com.lightningkite.services.notifications.Notification
import com.lightningkite.services.notifications.NotificationData

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
            val token = info.table().getOrNotFound(request.arg1)
            if (token.user != auth.id && auth.userRole() < UserRole.Admin) throw ForbiddenException("You don't own this token.")
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