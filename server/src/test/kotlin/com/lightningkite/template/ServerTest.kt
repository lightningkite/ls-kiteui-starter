package com.lightningkite.template

import com.lightningkite.lightningdb.get
import com.lightningkite.lightningdb.insertOne
import com.lightningkite.lightningserver.jsonschema.lightningServerSchema
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.typed.test
import com.lightningkite.nowLocal
import com.lightningkite.toEmailAddress
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class ServerTest {
    init {
        TestSettings
    }
    @Test
    fun testSchema() {
        println(Serialization.json.encodeToString(lightningServerSchema))
    }
    @Test
    fun basicTest(): Unit = runBlocking {
        val user = Server.users.info.collection()
            .insertOne(User(email = "${Random.nextInt()}@test.com".toEmailAddress()))!!
        Server.fcmTokens.registerEndpoint.test(user, "some token")
        assertEquals(user._id, Server.fcmTokens.info.collection().get("some token")!!.user)
    }
}