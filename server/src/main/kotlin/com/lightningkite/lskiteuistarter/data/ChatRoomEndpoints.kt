package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.RoomMembershipCache.roomMemberships
import com.lightningkite.services.database.*

object ChatRoomEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val userId = auth.id
            val userRoomIds = auth.roomMemberships()
            val isCreator = condition<ChatRoom> { it.createdBy eq userId }
            val isMember = condition<ChatRoom> { it._id inside userRoomIds }

            ModelPermissions(
                // Anyone authenticated can create a room
                create = Condition.Always,
                // Users can read rooms they're members of
                read = isMember,
                // Only creator can update room details
                update = isCreator,
                // Only creator can delete room
                delete = isCreator,
            )
        },
        postPermissionsForUser = {
            // Ensure creator is set correctly and is a member
            it.interceptCreate { model ->
                val creatorId = auth.id
                model.copy(
                    createdBy = creatorId,
                    memberIds = model.memberIds.plus(creatorId)
                )
            }
        }
    )

    val rest = path include ModelRestEndpoints(info)
    val socketUpdates = path include ModelRestUpdatesWebsocket(info)
}
