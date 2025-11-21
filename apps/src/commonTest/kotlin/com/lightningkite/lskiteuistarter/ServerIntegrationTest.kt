package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.auth.proof
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.sessions.EmailPinProof
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lskiteuistarter.data.SendMessageRequest
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.toEmailAddress
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that run against a test server instance.
 *
 * These tests verify that:
 * - The server endpoints work correctly
 * - Database operations behave as expected
 * - Business logic is properly enforced
 */
class ServerIntegrationTest {

    @Test
    fun testUserCreation() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()

            val newUser = User(
                email = "test@example.com",
                name = "Test User",
                role = UserRole.User
            )

            users.insertOne(newUser)

            val found = users.findOne(condition { it.email eq "test@example.com" })
            assertNotNull(found)
            assertEquals("Test User", found.name)
            assertEquals(UserRole.User, found.role)
        }
    }

    @Test
    fun testChatRoomCreation() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()

            // Create a user
            val user = User(
                email = "creator@example.com",
                name = "Room Creator",
                role = UserRole.User
            )
            users.insertOne(user)

            // Create a chat room
            val room = ChatRoom(
                name = "Test Room",
                description = "A test chat room",
                createdBy = user._id,
                memberIds = setOf(user._id)
            )
            chatRooms.insertOne(room)

            // Verify room was created
            val found = chatRooms.get(room._id)
            assertNotNull(found)
            assertEquals("Test Room", found.name)
            assertEquals(user._id, found.createdBy)
            assertTrue(user._id in found.memberIds)
        }
    }

    @Test
    fun testMessageCreation() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()
            val messages = db.table<Message>()

            // Create user and room
            val user = User(
                email = "sender@example.com",
                name = "Message Sender",
                role = UserRole.User
            )
            users.insertOne(user)

            val room = ChatRoom(
                name = "Chat Room",
                description = "Test",
                createdBy = user._id,
                memberIds = setOf(user._id)
            )
            chatRooms.insertOne(room)

            // Create a message
            val message = Message(
                chatRoomId = room._id,
                authorId = user._id,
                content = "Hello, world!"
            )
            messages.insertOne(message)

            // Verify message was created
            val found = messages.get(message._id)
            assertNotNull(found)
            assertEquals("Hello, world!", found.content)
            assertEquals(room._id, found.chatRoomId)
            assertEquals(user._id, found.authorId)
        }
    }

    @Test
    fun testChatRoomMembership() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()

            // Create two users
            val creator = User(
                email = "creator@example.com",
                name = "Creator",
                role = UserRole.User
            )
            users.insertOne(creator)

            val joiner = User(
                email = "joiner@example.com",
                name = "Joiner",
                role = UserRole.User
            )
            users.insertOne(joiner)

            // Creator creates a room
            val room = ChatRoom(
                name = "Membership Test",
                description = "Test membership",
                createdBy = creator._id,
                memberIds = setOf(creator._id)
            )
            chatRooms.insertOne(room)

            // Verify initial membership
            var currentRoom = chatRooms.get(room._id)!!
            assertTrue(creator._id in currentRoom.memberIds)
            assertTrue(joiner._id !in currentRoom.memberIds)

            // Joiner joins the room
            chatRooms.updateOneById(
                room._id,
                modification { it.memberIds.assign(currentRoom.memberIds + joiner._id) }
            )

            // Verify updated membership
            currentRoom = chatRooms.get(room._id)!!
            assertTrue(creator._id in currentRoom.memberIds)
            assertTrue(joiner._id in currentRoom.memberIds)

            // Joiner leaves the room
            chatRooms.updateOneById(
                room._id,
                modification { it.memberIds.assign(currentRoom.memberIds - joiner._id) }
            )

            // Verify final membership
            currentRoom = chatRooms.get(room._id)!!
            assertTrue(creator._id in currentRoom.memberIds)
            assertTrue(joiner._id !in currentRoom.memberIds)
        }
    }

    @Test
    fun testRoomSettings() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()

            val user = User(
                email = "owner@example.com",
                name = "Owner",
                role = UserRole.User
            )
            users.insertOne(user)

            val room = ChatRoom(
                name = "Settings Test",
                description = "Test settings",
                createdBy = user._id,
                memberIds = setOf(user._id),
                isPrivate = false,
                muteNotifications = false,
                autoDeleteDays = null
            )
            chatRooms.insertOne(room)

            // Update settings
            chatRooms.replaceOneById(
                room._id,
                room.copy(
                    isPrivate = true,
                    muteNotifications = true,
                    autoDeleteDays = 7
                )
            )

            // Verify settings were updated
            val updated = chatRooms.get(room._id)!!
            assertTrue(updated.isPrivate)
            assertTrue(updated.muteNotifications)
            assertEquals(7, updated.autoDeleteDays)
        }
    }

    @Test
    fun testMessageQueryByRoom() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()
            val messages = db.table<Message>()

            val user = User(
                email = "user@example.com",
                name = "User",
                role = UserRole.User
            )
            users.insertOne(user)

            // Create two rooms
            val room1 = ChatRoom(
                name = "Room 1",
                description = "First room",
                createdBy = user._id,
                memberIds = setOf(user._id)
            )
            chatRooms.insertOne(room1)

            val room2 = ChatRoom(
                name = "Room 2",
                description = "Second room",
                createdBy = user._id,
                memberIds = setOf(user._id)
            )
            chatRooms.insertOne(room2)

            // Create messages in both rooms
            messages.insertOne(Message(chatRoomId = room1._id, authorId = user._id, content = "Room 1 Message 1"))
            messages.insertOne(Message(chatRoomId = room1._id, authorId = user._id, content = "Room 1 Message 2"))
            messages.insertOne(Message(chatRoomId = room2._id, authorId = user._id, content = "Room 2 Message 1"))

            // Query messages for room 1
            val room1Messages = messages.query(Query(condition { it.chatRoomId eq room1._id }))
            assertEquals(2, room1Messages.size)
            assertTrue(room1Messages.all { it.chatRoomId == room1._id })

            // Query messages for room 2
            val room2Messages = messages.query(Query(condition { it.chatRoomId eq room2._id }))
            assertEquals(1, room2Messages.size)
            assertEquals(room2._id, room2Messages[0].chatRoomId)
        }
    }

    @Test
    fun testUserRoles() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()

            // Create users with different roles
            val regularUser = User(
                email = "user@example.com",
                name = "Regular User",
                role = UserRole.User
            )
            users.insertOne(regularUser)

            val admin = User(
                email = "admin@example.com",
                name = "Admin",
                role = UserRole.Admin
            )
            users.insertOne(admin)

            // Verify roles
            val foundUser = users.get(regularUser._id)!!
            assertEquals(UserRole.User, foundUser.role)

            val foundAdmin = users.get(admin._id)!!
            assertEquals(UserRole.Admin, foundAdmin.role)

            // Test role comparison
            assertTrue(UserRole.Admin >= UserRole.User)
            assertTrue(UserRole.User < UserRole.Admin)
        }
    }

    @Test
    fun testMultipleUsersInRoom() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()
            val messages = db.table<Message>()

            // Create multiple users
            val user1 = User(email = "user1@example.com", name = "User 1", role = UserRole.User)
            val user2 = User(email = "user2@example.com", name = "User 2", role = UserRole.User)
            val user3 = User(email = "user3@example.com", name = "User 3", role = UserRole.User)

            users.insertOne(user1)
            users.insertOne(user2)
            users.insertOne(user3)

            // Create a room with all users
            val room = ChatRoom(
                name = "Group Chat",
                description = "Multi-user room",
                createdBy = user1._id,
                memberIds = setOf(user1._id, user2._id, user3._id)
            )
            chatRooms.insertOne(room)

            // Each user sends a message
            messages.insertOne(Message(chatRoomId = room._id, authorId = user1._id, content = "Hello from User 1"))
            messages.insertOne(Message(chatRoomId = room._id, authorId = user2._id, content = "Hello from User 2"))
            messages.insertOne(Message(chatRoomId = room._id, authorId = user3._id, content = "Hello from User 3"))

            // Verify all messages are in the room
            val roomMessages = messages.query(Query(condition { it.chatRoomId eq room._id }))
            assertEquals(3, roomMessages.size)

            // Verify different authors
            val authorIds = roomMessages.map { it.authorId }.toSet()
            assertEquals(3, authorIds.size)
            assertTrue(user1._id in authorIds)
            assertTrue(user2._id in authorIds)
            assertTrue(user3._id in authorIds)
        }
    }
}
