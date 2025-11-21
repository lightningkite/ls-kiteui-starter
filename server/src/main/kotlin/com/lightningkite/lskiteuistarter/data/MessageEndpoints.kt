package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lskiteuistarter.Message
import com.lightningkite.lskiteuistarter.Server
import com.lightningkite.lskiteuistarter.UserAuth
import com.lightningkite.lskiteuistarter.UserAuth.RoomMembershipCache.roomMemberships
import com.lightningkite.lskiteuistarter.authorId
import com.lightningkite.lskiteuistarter.chatRoomId
import com.lightningkite.services.database.*

object MessageEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val userId = auth.id
            val userRoomIds = auth.roomMemberships()
            val isAuthor = condition<Message> { it.authorId eq userId }
            val inUserRoom = condition<Message> { it.chatRoomId inside userRoomIds }

            ModelPermissions(
                // Users can only create messages in rooms they're members of
                create = inUserRoom and isAuthor,
                // Users can only read messages from rooms they're in
                read = inUserRoom,
                // Only authors can update/delete, and only in rooms they're in
                update = isAuthor and inUserRoom,
                delete = isAuthor and inUserRoom,
            )
        },
        postPermissionsForUser = {
            // Force authorId to be correct on create
            it.interceptCreate { model ->
                model.copy(
                    authorId = auth.id,
                    createdAt = kotlin.time.Clock.System.now()
                )
            }
        }
    )

    val rest = path include ModelRestEndpoints(info)
    val socketUpdates = path include ModelRestUpdatesWebsocket(info)
}
