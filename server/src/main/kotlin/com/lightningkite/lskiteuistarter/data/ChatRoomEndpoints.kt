package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.http.get
import com.lightningkite.lightningserver.http.post
import com.lightningkite.lightningserver.typed.ApiHttpHandler
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.ModelRestEndpointsAndUpdatesWebsocket.Companion.plus
import com.lightningkite.lightningserver.typed.ModelRestUpdatesWebsocket
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.services.database.*
import com.lightningkite.services.database.query
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

object ChatRoomEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val userId = auth.id
            val isCreator = condition<ChatRoom> { it.createdBy eq userId }

            ModelPermissions(
                create = Condition.Always,
                read = Condition.Always, // Filtered in custom endpoints
                update = isCreator,
                updateRestrictions = updateRestrictions {
                    it.createdBy.cannotBeModified()
                    it.createdAt.cannotBeModified()
                },
                delete = isCreator,
            )
        }
    )

    // REST endpoints + WebSocket live updates
    val rest = path include ModelRestEndpoints(info) + ModelRestUpdatesWebsocket(info)

    // Custom endpoint: Join a room
    val join = path.path("join").post bind ApiHttpHandler(
        summary = "Join a chat room",
        description = "Adds the current user to the chat room's member list",
        auth = UserAuth.require(),
        implementation = { roomId: Uuid ->
            val userId = auth.id
            val room = info.table().get(roomId) ?: throw NotFoundException("Room not found")

            if (userId in room.memberIds) {
                return@ApiHttpHandler room // Already a member
            }

            info.table().updateOneById(roomId, modification {
                it.memberIds.assign(room.memberIds + userId)
            })

            // TODO: Invalidate room membership cache when cache invalidation API is available

            info.table().get(roomId)!!
        }
    )

    // Custom endpoint: Leave a room
    val leave = path.path("leave").post bind ApiHttpHandler(
        summary = "Leave a chat room",
        description = "Removes the current user from the chat room's member list",
        auth = UserAuth.require(),
        implementation = { roomId: Uuid ->
            val userId = auth.id
            val room = info.table().get(roomId) ?: throw NotFoundException("Room not found")

            if (userId !in room.memberIds) {
                return@ApiHttpHandler room // Not a member
            }

            info.table().updateOneById(roomId, modification {
                it.memberIds.assign(room.memberIds - userId)
            })

            // TODO: Invalidate room membership cache when cache invalidation API is available

            info.table().get(roomId)!!
        }
    )
}
