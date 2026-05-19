// by Claude — Fetcher implementation that routes requests through a Lightning Server TestRunner
// instead of real HTTP. Zero project-specific imports — designed for extraction to lightning-server-kiteui.
package com.lightningkite.lskiteuistarter

import com.lightningkite.MediaType
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * A [Fetcher] that dispatches requests through a [ServerRuntime] instead of real HTTP.
 *
 * This allows using the generated `LiveApi(fetcher)` against a real in-memory server
 * (via TestRunner), enabling true end-to-end integration tests without network I/O.
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

    override fun <I, O> websocket(
        url: String,
        inSerializer: KSerializer<I>,
        outSerializer: KSerializer<O>,
    ): ClientWebSocket<I, O> {
        TODO("WebSocket not needed for integration tests yet")
    }

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
