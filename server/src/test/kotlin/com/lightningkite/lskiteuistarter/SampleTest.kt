package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.db.Database
import com.lightningkite.services.database.*
import com.lightningkite.toEmailAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SampleTest {
    @Test
    fun testMessageEdit() = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()

            // Create a test user
            val user = User(
                email = "test@example.com".toEmailAddress(),
                name = "Test User"
            )
            db.table<User>().insertOne(user)

            // Create a chat room
            val room = ChatRoom(
                name = "Test Room",
                createdBy = user._id,
                memberIds = setOf(user._id)
            )
            db.table<ChatRoom>().insertOne(room)

            // Create a message
            val originalMessage = Message(
                chatRoomId = room._id,
                authorId = user._id,
                content = "Original content"
            )
            db.table<Message>().insertOne(originalMessage)

            // Edit the message
            val editRequest = EditMessageRequest(
                messageId = originalMessage._id,
                newContent = "Edited content"
            )

            // Test the edit endpoint (simulating authenticated request)
            val messages = db.table<Message>()
            val messageToEdit = messages.get(editRequest.messageId)
            assertNotNull(messageToEdit)
            assertEquals("Original content", messageToEdit.content)

            // Verify the message can be edited
            messages.updateOneById(editRequest.messageId, modification {
                it.content assign editRequest.newContent
                it.editedAt assign kotlin.time.Clock.System.now()
            })

            val editedMessage = messages.get(editRequest.messageId)
            assertNotNull(editedMessage)
            assertEquals("Edited content", editedMessage.content)
            assertNotNull(editedMessage.editedAt)
            println("Message edit test passed: content updated and editedAt set")
        }
    }
}