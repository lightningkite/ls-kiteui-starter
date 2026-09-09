// A Fetcher implementation that routes requests through a Lightning Server ServerRuntime instead
// of real HTTP. Lets the generated LiveApi(fetcher) run against a real in-memory server, enabling
// true end-to-end integration tests without network I/O. Intentionally has no project-specific imports.
package com.lightningkite.lskiteuistarter

import com.lightningkite.services.data.MediaType
import com.lightningkite.lightningserver.HttpMethod
import com.lightningkite.lightningserver.LSError
import com.lightningkite.lightningserver.LsErrorException
import com.lightningkite.lightningserver.http.HttpHeaders
import com.lightningkite.lightningserver.http.HttpRequest
import com.lightningkite.lightningserver.http.QueryParameters
import com.lightningkite.lightningserver.pathing.PathSpec
import com.lightningkite.lightningserver.pathing.RawHttpEndpoint
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.runtime.handle
import com.lightningkite.lightningserver.typed.ClientWebSocket
import com.lightningkite.lightningserver.typed.Fetcher
import com.lightningkite.services.data.TypedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * A [Fetcher] that dispatches requests through a [ServerRuntime] instead of real HTTP.
 *
 * Usage:
 * ```kotlin
 * Server.test(settings = { database set Database.Settings("ram") }) {
 *     val fetcher = TestRunnerFetcher(this@test)
 *     val api = LiveApi(fetcher)
 *     // api calls now go through the real server
 * }
 * ```
 */
class TestRunnerFetcher(
    private val runtime: ServerRuntime,
    private val headerCalculator: (suspend () -> List<Pair<String, String>>)? = null,
) : Fetcher {

    private val json: Json get() = runtime.externalSerialization.json

    override fun withHeaderCalculator(
        calculator: suspend () -> List<Pair<String, String>>
    ): Fetcher = TestRunnerFetcher(runtime, calculator)

    override suspend fun <I, O> invoke(
        url: String,
        method: HttpMethod,
        inSerializer: KSerializer<I>,
        body: I,
        outSerializer: KSerializer<O>,
    ): O {
        // 1. Serialize input to JSON body (skip for Unit input)
        val bodyData = if (inSerializer.descriptor == Unit.serializer().descriptor) null
        else TypedData.text(json.encodeToString(inSerializer, body), MediaType.Application.Json)

        // 2. Build headers (include auth headers from calculator)
        val extraHeaders = headerCalculator?.invoke() ?: emptyList()
        val headers = HttpHeaders(
            extraHeaders + listOf(
                "Accept" to "application/json",
                "Content-Type" to "application/json",
            )
        )

        // 3. Parse URL into path and query parameters
        val (path, queryParams) = parseUrl(url)

        // 4. Create request with RawHttpEndpoint (lazy match triggers inside ServerRuntime.handle)
        val request = HttpRequest<PathSpec>(
            path = RawHttpEndpoint(path, method),
            queryParameters = queryParams,
            headers = headers,
            domain = "localhost",
            protocol = "http",
            sourceIp = "127.0.0.1",
            body = bodyData,
        )

        // 5. Dispatch through server routing (handles interceptors, auth, error handling)
        val response = with(runtime) { runtime.handle(request) }

        // 6. Handle errors — mirror how ConnectivityFetcher/BulkFetcher parse errors
        if (response.status.code >= 400) {
            val text = response.body?.text() ?: ""
            throw try {
                LsErrorException(json.decodeFromString(LSError.serializer(), text))
            } catch (_: Exception) {
                LsErrorException(LSError(response.status.code, "Unknown", message = text))
            }
        }

        // 7. Deserialize response (skip for Unit output)
        if (outSerializer.descriptor == Unit.serializer().descriptor) {
            @Suppress("UNCHECKED_CAST")
            return Unit as O
        }
        val responseBody = response.body?.text()
            ?: throw IllegalStateException("Expected response body but got none for $method $url")
        return json.decodeFromString(outSerializer, responseBody)
    }

    /**
     * Returns a loopback WebSocket that immediately acknowledges any condition it is sent.
     *
     * ModelCache opens a `SharedCollectionUpdatesSocket` for live delta updates and, for low
     * pull-frequency reads (`Draft(id)` → `watch(id, pullFrequency = 0s)`), BLOCKS the initial fetch
     * on `withTimeoutOrNull(5s) { socket.wait() }` until the socket acknowledges the subscription.
     * A socket that never connects stalls that wait the full 5s, which loses the race against a
     * test's 5s `waitForText`. This loopback mirrors the framework's own mock updates socket
     * ([ClientModelRestEndpointsPlusUpdatesWebsocketMock]): it reports connected, fires `onOpen`, and
     * echoes each sent [com.lightningkite.services.database.Condition] straight back as a
     * `CollectionUpdates(condition = …)` — the exact acknowledgement `wait()` waits for. `satisfied`
     * flips immediately, the wait returns, and data is then fetched via plain HTTP through [invoke].
     * It carries no live data (empty updates/remove), so HTTP remains the single source of truth.
     */
    override fun <I, O> websocket(
        url: String,
        inSerializer: KSerializer<I>,
        outSerializer: KSerializer<O>,
    ): ClientWebSocket<I, O> = LoopbackClientWebSocket(json, inSerializer, outSerializer)

    override fun <T> url(value: T, serializer: KSerializer<T>): String {
        return runtime.externalSerialization.stringArrayFormat.encodeToString(serializer, value)
    }

    /** Split "some/path?key=val&k2=v2" into path string and QueryParameters. */
    private fun parseUrl(url: String): Pair<String, QueryParameters> {
        val questionMark = url.indexOf('?')
        if (questionMark == -1) return Pair(url, QueryParameters(emptyList()))
        val path = url.substring(0, questionMark)
        val queryString = url.substring(questionMark + 1)
        val params: List<Pair<String, String>> = queryString.split('&').mapNotNull { segment ->
            val eq = segment.indexOf('=')
            if (eq == -1) null
            else Pair(segment.substring(0, eq), segment.substring(eq + 1))
        }
        return Pair(path, QueryParameters(params))
    }
}

/**
 * A ClientWebSocket that reports connected and echoes each sent value straight back as an
 * acknowledgement, so a `SharedCollectionUpdatesSocket`'s `wait()` completes immediately instead of
 * stalling on a socket that never connects. See [TestRunnerFetcher.websocket] for why this matters.
 *
 * The send type is a `Condition<T>` and the receive type is a `CollectionUpdates<T, ID>` (whose only
 * required-for-ack field is `condition`, all others defaulted). Because this generic socket can't
 * name those types, the echo is built by a JSON round-trip through the provided serializers. If a
 * value can't be echoed that way (a non-model socket), it degrades to a silent no-op.
 */
private class LoopbackClientWebSocket<I, O>(
    private val json: Json,
    private val inSerializer: KSerializer<I>,
    private val outSerializer: KSerializer<O>,
) : ClientWebSocket<I, O> {
    override val connected: SharedFlow<Boolean> = MutableStateFlow(true)
    private val onOpenActions = ArrayList<() -> Unit>()
    private val onMessageActions = ArrayList<(O) -> Unit>()

    override fun connect() {
        onOpenActions.forEach { it() }
    }

    override fun send(data: I) {
        val ack = try {
            val condition = json.encodeToJsonElement(inSerializer, data)
            json.decodeFromJsonElement(outSerializer, JsonObject(mapOf("condition" to condition)))
        } catch (_: Exception) {
            return  // Not a model-updates socket (or unserializable); nothing to acknowledge.
        }
        onMessageActions.forEach { it(ack) }
    }

    override fun onOpen(action: () -> Unit) { onOpenActions.add(action) }
    override fun onMessage(action: (O) -> Unit) { onMessageActions.add(action) }
    override fun close(code: Short, reason: String) {}
    override fun onClose(action: (Short) -> Unit) {}
}
