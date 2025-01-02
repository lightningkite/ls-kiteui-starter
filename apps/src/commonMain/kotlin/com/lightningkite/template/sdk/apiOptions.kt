package com.lightningkite.template.sdk

import com.lightningkite.kiteui.reactive.PersistentProperty
import kotlinx.serialization.Serializable


@Serializable
enum class ApiOption(val apiName: String, val http: String, val ws: String) {
//    Production(" ", "https://", "wss://"),
//    Staging("Staging", "https://", "wss://"),
//    Dev("Dev", "https://", "wss://"),
    Local("Local", "http://localhost:8080", "ws://localhost:8080")
    ;

    val api get() = LiveApi(http, ws)

}

val selectedApi = PersistentProperty<ApiOption>("apiOption", getDefaultServerBackend())


expect fun getDefaultServerBackend(): ApiOption