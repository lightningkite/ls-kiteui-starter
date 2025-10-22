package com.lightningkite.lskiteuistarter

import com.lightningkite.kotlinercli.cli
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.definition.builder.*
import com.lightningkite.lightningserver.deprecations.*
import com.lightningkite.lightningserver.encryption.*
import com.lightningkite.lightningserver.engine.ktor.KtorEngine
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.pathing.*
import com.lightningkite.lightningserver.runtime.*
import com.lightningkite.lightningserver.serialization.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lightningserver.settings.*
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.typed.sdk.FetcherSdk
import com.lightningkite.lightningserver.typed.sdk.SDK.writeSdk
import com.lightningkite.lightningserver.typed.sdk.CachingSdk
import com.lightningkite.lightningserver.typed.sdk.plus
import com.lightningkite.lightningserver.websockets.*
import com.lightningkite.services.cache.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.email.*
import com.lightningkite.services.files.*
import com.lightningkite.services.notifications.*
import com.lightningkite.services.sms.*
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

private lateinit var settingsFile: KFile

fun setup(settings: KFile = KFile("settings.json")) {
    settingsFile = settings
}

private var engine: KtorEngine? = null

// not sure what this is
fun engine(setup: KtorEngine.() -> Unit) {
    engine?.let {
        setup(it)
        return
    }

    val before = TimeSource.Monotonic.markNow()
    val built = Server.build()
    println("Server built in ${before.elapsedNow()}")

    engine = KtorEngine(built).apply {
        settings.loadFromFile(settingsFile, internalSerializersModule)
        setup()
    }
}

fun serve() = engine { start(Netty) }

fun sdk() = engine {
    Utils.logger.info { "Generating FetcherSdk" }
    Server.writeSdk(
        FetcherSdk + CachingSdk,
        KFile("apps/src/commonMain/kotlin/com/lightningkite/template/sdk"),
        "com.lightningkite.lskiteuistarter.sdk",
    )
    Utils.logger.info { "Done" }
}
fun main(vararg args: String) = cli(
    arguments = args,
    setup = ::setup,
    available = listOf(
        ::serve,
        ::sdk,
    ),
    useInteractive = true,
)


object Utils {
    val logger: KLogger = KotlinLogging.logger("com.lightningtime")

    // This is serverless, right? An explanation of how serverless works would be good
    suspend fun <T> runForEach(seconds: Int, items: Collection<T>, action: suspend (T) -> Unit): List<T> {
        val loopStart = TimeSource.Monotonic.markNow()
        val duration = seconds.seconds

        val remaining = items.toMutableList()
        while (loopStart.elapsedNow() < duration && remaining.isNotEmpty()) {
            try {
                action(remaining.removeFirst())
            } catch (e: Throwable) {
                KotlinLogging.logger("runForEach").error(e) { "Exception encountered in runForEach" }
            }
        }

        return remaining
    }

    suspend fun <T> runFor(seconds: Int, startingValue: T, action: suspend (T) -> T?): T? {

        val loopStart = TimeSource.Monotonic.markNow()
        val duration = seconds.seconds

        var value = startingValue

        while (loopStart.elapsedNow() < duration) {
            value = action(value) ?: return null
        }

        return value
    }
}