package com.heroscript

import com.lightningkite.kotlinercli.cli
import com.lightningkite.lightningserver.engine.ktor.KtorEngine
import com.lightningkite.lightningserver.settings.loadFromFile
import com.lightningkite.lightningserver.typed.sdk.CachingSdk
import com.lightningkite.lightningserver.typed.sdk.FetcherSdk
import com.lightningkite.lightningserver.typed.sdk.SDK.write
import com.lightningkite.services.kfile.KFile
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private lateinit var settingsFile: KFile

fun setup(settings: KFile = KFile("settings.json")) {
    settingsFile = settings
}

private var engine: KtorEngine? = null

fun engine(setup: KtorEngine.() -> Unit) {
    engine?.let {
        setup(it)
        return
    }

    val before = TimeSource.Monotonic.markNow()
    val built = Server.build()
    println("Server built in ${before.elapsedNow()}")

    engine = KtorEngine(built, Clock.lsKiteuiStarter).apply {
        settings.loadFromFile(settingsFile, internalSerializersModule)
        settings.ready()
        setup()
    }
}

fun serve() = engine { start(Netty) }

fun sdk() = engine {
    val folder = KFile("apps/src/commonMain/kotlin/com/heroscript/sdk")
    Utils.logger.info { "Generating FetcherSdk" }
    FetcherSdk("com.heroscript.sdk").write(folder)
    Utils.logger.info { "Generating CachingSdk" }
    CachingSdk("com.heroscript.sdk").write(folder)
    Utils.logger.info { "Done" }
}

fun main(vararg args: String) = cli(
    arguments = args,
    setup = ::setup,
    available = listOf(
        ::serve,
        ::sdk,
        ::seed,
    ),
    useInteractive = true,
)


object Utils {
    val logger: KLogger = KotlinLogging.logger("com.heroscript")

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