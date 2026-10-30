package com.lightningkite.lskiteuistarter.api

import com.lightningkite.kotlinercli.cli
import com.lightningkite.lightningserver.engine.ktor.KtorEngine
import com.lightningkite.lightningserver.settings.loadFromFile
import com.lightningkite.lightningserver.typed.sdk.CachingSdk
import com.lightningkite.lightningserver.typed.sdk.FetcherSdk
import com.lightningkite.lightningserver.typed.sdk.SDK.writeUsingDefaultSettings
import com.lightningkite.lskiteuistarter.App
import com.lightningkite.services.kfile.KFile
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.*
import kotlin.time.Clock
import kotlin.time.TimeSource
import kotlin.time.measureTime

private lateinit var settingsFile: KFile

fun setup(settings: KFile = KFile("settings.json")) {
    settingsFile = settings
}

private var engine: KtorEngine? = null
private val logger = KotlinLogging.logger("com.lightningkite.lskiteuistarter")

fun withEngine(setup: KtorEngine.() -> Unit) {
    engine?.let {
        setup(it)
        return
    }

    val before = TimeSource.Monotonic.markNow()
    val built = Server.build()
    logger.info { "Server built in ${before.elapsedNow()}" }

    engine = KtorEngine(built, Clock.App).apply {
        settings.loadFromFile(settingsFile, internalSerializersModule)
        settings.ready()
        setup()
    }
}

fun serve() = withEngine { start(Netty) }
fun predeploy() = withEngine {
    val preDeployTime = TimeSource.Monotonic.measureTime { runPreDeploy() }
    println("Pre Deploy finished in $preDeployTime")
}

// Local development: prepare the database, then serve, in one process.
fun dev() = withEngine {
    runPreDeployTasksBlocking()
    start(Netty)
}

fun sdk() = withEngine {
    val folder = KFile("apps/src/commonMain/kotlin/com/lightningkite/lskiteuistarter/sdk")
    logger.info { "Generating FetcherSdk" }
    FetcherSdk("com.lightningkite.lskiteuistarter.sdk").writeUsingDefaultSettings(Server, folder)
    logger.info { "Generating CachingSdk" }
    CachingSdk("com.lightningkite.lskiteuistarter.sdk").writeUsingDefaultSettings(Server, folder)
    logger.info { "Done" }
}

fun main(vararg args: String) = cli(
    arguments = args,
    setup = ::setup,
    available = listOf(
        ::serve,
        ::predeploy,
        ::dev,
        ::sdk,
        ::seed,
    ),
    useInteractive = true,
)