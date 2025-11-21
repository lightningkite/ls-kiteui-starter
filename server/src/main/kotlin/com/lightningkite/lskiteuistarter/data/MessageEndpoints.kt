package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.ForbiddenException
import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.http.post
import com.lightningkite.lightningserver.typed.ApiHttpHandler
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.ModelRestEndpointsAndUpdatesWebsocket.Companion.plus
import com.lightningkite.lightningserver.typed.ModelRestUpdatesWebsocket
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.RoomMembershipCache.roomMemberships
import com.lightningkite.services.database.*
import com.lightningkite.services.database.query
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.uuid.Uuid

object MessageEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val userId = auth.id
            val userRoomIds = with(UserAuth) { auth.roomMemberships() }
            val isAuthor = condition<Message> { it.authorId eq userId }
            val inUserRoom = condition<Message> { it.chatRoomId inside userRoomIds }

            ModelPermissions(
                create = inUserRoom and isAuthor,
                read = inUserRoom,
                update = isAuthor and inUserRoom,
                updateRestrictions = updateRestrictions {
                    it.chatRoomId.cannotBeModified()
                    it.authorId.cannotBeModified()
                    it.createdAt.cannotBeModified()
                },
                delete = isAuthor and inUserRoom,
            )
        },
        postPermissionsForUser = {
            // Force createdAt to be correct
            it.interceptCreate { it.copy(createdAt = Clock.System.now()) }
        }
    )

    // REST endpoints + WebSocket live updates
    val rest = path include ModelRestEndpoints(info) + ModelRestUpdatesWebsocket(info)

    // Custom endpoint: Send a message to a room
    // This ensures authorId is always set from auth context, preventing impersonation
    val send = path.path("send").post bind ApiHttpHandler(
        summary = "Send a message to a chat room",
        description = "Creates a new message in the specified chat room. User must be a member of the room.",
        auth = UserAuth.require(),
        implementation = { request: SendMessageRequest ->
            val message = Message(
                chatRoomId = request.chatRoomId,
                authorId = auth.id,
                content = request.content
            )

            // Permission system validates room membership
            info.table().insertOne(message)!!
        }
    )

    // Custom endpoint: Edit a message
    val edit = path.path("edit").post bind ApiHttpHandler(
        summary = "Edit a message",
        description = "Updates the content of an existing message. Only the author can edit their messages.",
        auth = UserAuth.require(),
        implementation = { request: EditMessageRequest ->
            val userId = auth.id
            val message = info.table().get(request.messageId) ?: throw NotFoundException("Message not found")

            // Verify user is the author
            if (message.authorId != userId) {
                throw ForbiddenException("Only the author can edit this message")
            }

            // Update the message content and editedAt timestamp
            info.table().updateOneById(request.messageId, modification {
                it.content assign request.newContent
                it.editedAt assign Clock.System.now()
            })

            info.table().get(request.messageId)!!
        }
    )
}

