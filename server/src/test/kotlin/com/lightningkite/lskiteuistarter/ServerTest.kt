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
import com.lightningkite.toEmailAddress
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

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
        val user = Server.users.info.table()
            .insertOne(User(email = "${Random.nextInt()}@test.com".toEmailAddress()))!!
        Server.fcmTokens.registerEndpoint.test(user, "some token")
        assertEquals(user._id, Server.fcmTokens.info.table().get("some token")!!.user)
    }
}