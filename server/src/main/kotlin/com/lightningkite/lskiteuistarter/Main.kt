package com.lightningkite.lskiteuistarter

import com.lightningkite.kotlinercli.cli
import com.lightningkite.lightningserver.engine.ktor.KtorEngine
import com.lightningkite.lightningserver.settings.loadFromFile
import com.lightningkite.lightningserver.typed.sdk.CachingSdk
import com.lightningkite.lightningserver.typed.sdk.FetcherSdk
import com.lightningkite.lightningserver.typed.sdk.SDK.write
import com.lightningkite.services.kfile.KFile
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.*
import kotlin.time.Clock
import kotlin.time.TimeSource

private lateinit var settingsFile: KFile

fun setup(settings: KFile = KFile("settings.json")) {
    settingsFile = settings
}

private var engine: KtorEngine? = null
private val logger = KotlinLogging.logger("com.lightningkite.lskiteuistarter")

fun engine(setup: KtorEngine.() -> Unit) {
    engine?.let {
        setup(it)
        return
    }

    val before = TimeSource.Monotonic.markNow()
    val built = Server.build()
    println("Server built in ${before.elapsedNow()}")

    engine = KtorEngine(built, Clock.App).apply {
        settings.loadFromFile(settingsFile, internalSerializersModule)
        settings.ready()
        setup()
    }
}

fun serve() = engine { start(Netty) }

fun sdk() = engine {
    val folder = KFile("apps/src/commonMain/kotlin/com/lightningkite/lskiteuistarter/sdk")
    logger.info { "Generating FetcherSdk" }
    FetcherSdk("com.lightningkite.lskiteuistarter.sdk").write(folder)
    logger.info { "Generating CachingSdk" }
    CachingSdk("com.lightningkite.lskiteuistarter.sdk").write(folder)
    logger.info { "Done" }
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