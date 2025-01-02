package com.lightningkite.template.sdk

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.StringArrayFormat
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.DefaultJson
import com.lightningkite.lightningserver.batchFetch
import com.lightningkite.serialization.ClientModule
import kotlinx.serialization.*

val stringArrayFormat = StringArrayFormat(ClientModule)
inline fun <reified T> T.urlify(): String = stringArrayFormat.encodeToString(this)

// 2 problems:
// Retry logic AND batch logic
suspend inline fun <reified OUT> fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    noinline token: (suspend () -> String)? = null,
    headers: HttpHeaders = httpHeaders(),
    masquerade: String? = null,
    bodyJson: String?,
): OUT = batchFetch(url, method, { token?.invoke() }, bodyJson)

suspend inline fun <reified IN, reified OUT> fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    noinline token: (suspend () -> String)? = null,
    headers: HttpHeaders = httpHeaders(),
    masquerade: String? = null,
    body: IN,
): OUT = fetch(url, method, token, headers, masquerade, DefaultJson.encodeToString(body))

suspend inline fun <reified OUT> fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    noinline token: (suspend () -> String)? = null,
    headers: HttpHeaders = httpHeaders(),
    masquerade: String? = null,
): OUT = fetch(url, method, token, headers, masquerade, null)

//inline fun <reified IN, reified OUT> multiplexedSocket(
//    socketUrl: String,
//    path: String,
//    token: String?,
//): TypedWebSocket<IN, OUT> = multiplexSocket(
//    url = "$socketUrl/?path=multiplex${token?.let { "?authorization=${UrlEncoderUtil.encode(it)}" } ?: ""}",
//    path = path,
//    params = emptyMap(),
//    json = DefaultJson,
//    pingTime = 10_000,
//    log = ConsoleRoot.tag("multiplexedSocket")
//).typed(DefaultJson, DefaultJson.serializersModule.serializer(typeOf<IN>()) as KSerializer<IN>, DefaultJson.serializersModule.serializer(typeOf<OUT>()) as KSerializer<OUT>)

