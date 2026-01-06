package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.ai.SystemChatConversation
import com.lightningkite.lightningserver.auth.testAuth
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.typed.AuthAccess
import com.lightningkite.lskiteuistarter.Server
import com.lightningkite.lskiteuistarter.User
import com.lightningkite.lskiteuistarter.UserAuth
import com.lightningkite.services.database.insertOne
import com.lightningkite.toEmailAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import kotlin.test.Test
import kotlin.time.Clock

class SupportChatEndpointsTest {
    @Test
    fun test() {
        Server.test(
            settings = {}
        ) {
            runBlocking {
                val user = users.info.table().insertOne(User(email = "test@test.com".toEmailAddress()))!!
                val conversation = supportChat.conversations.info.table().insertOne(
                    SystemChatConversation(
                        subjectId = user._id.toString(),
                        name = "Test Conversation",
                        createdAt = Clock.System.now()
                    )
                )!!
                println(supportChat.getPrompt(conversation, AuthAccess(UserAuth.testAuth(user))))
            }
        }
    }
}