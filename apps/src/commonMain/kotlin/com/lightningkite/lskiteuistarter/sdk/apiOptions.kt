package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.networking.BulkFetcher
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.files.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable


@Serializable
enum class ApiOption(val apiName: String, val http: String, val ws: String) {
//    Production(" ", "https://", "wss://"),
//    Staging("Staging", "https://", "wss://"),
//    Dev("Dev", "https://", "wss://"),
    Local("Local", "http://localhost:8080", "ws://localhost:8080")
    ;

    val baseFetcher
        get() = /*if (!debug) */BulkFetcher(
            httpBulk = "$http/meta/bulk",
            wsMultiplex = "$ws?path=/multiplex",
            pingTime = 30.seconds,
        ) /*else ConnectivityFetcher(
            http = http,
            ws = ws,
            pingTime = 30.seconds,
        )*/
    val api get() = LiveApi(baseFetcher)
    fun next(): ApiOption = ApiOption.entries[(ordinal + 1) % ApiOption.entries.size]
}

val selectedApi = PersistentProperty<ApiOption>("apiOption", getDefaultServerBackend())


expect fun getDefaultServerBackend(): ApiOption