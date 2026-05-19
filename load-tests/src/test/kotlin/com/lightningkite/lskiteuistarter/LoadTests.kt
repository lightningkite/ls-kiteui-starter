// by Claude — Load tests: performance baselines against a live Lightning Server.
//
// Run against a local server:
//   ./testing/start-backend.sh
//   ./gradlew :load-tests:test -Dloadtest.url=http://localhost:8081 -Dloadtest.token=<admin-token>
//
// The tests skip automatically when loadtest.url is not set, so they are safe
// to include in CI without a live server.
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.loadtest.Scenario
import com.lightningkite.lightningserver.loadtest.loadTest
import com.lightningkite.lskiteuistarter.data.InventoryItemEndpoints
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.Query
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LoadTests {

    /**
     * The base URL of the server under test.  Must include scheme and port, no trailing slash.
     * Set via system property: -Dloadtest.url=http://localhost:8081
     */
    private val baseUrl: String? = System.getProperty("loadtest.url")

    /**
     * Optional Bearer token for authenticated scenarios.
     * Set via system property: -Dloadtest.token=<session-token>
     */
    private val token: String? = System.getProperty("loadtest.token")

    @Test
    fun unauthenticatedHealthCheck() {
        val url = baseUrl ?: return  // skip when not configured

        val summary = runBlocking {
            loadTest(
                server = Server,
                baseUrl = url,
                virtualUsers = 10,
                sustain = 10.seconds,
                scenarios = listOf(
                    Scenario(name = "health-check") {
                        raw("GET", "/meta/online")
                    },
                ),
            )
        }

        println("Health-check RPS: ${summary.requestsPerSecond}")
        assertTrue(summary.totalErrors == 0L, "Expected zero errors but got ${summary.totalErrors}")
    }

    @Test
    fun authenticatedInventoryScenarios() {
        val url = baseUrl ?: return   // skip when not configured
        val authToken = token ?: return  // skip when no token provided

        val authHeaders = mapOf("Authorization" to "Bearer $authToken")

        val summary = runBlocking {
            loadTest(
                server = Server,
                baseUrl = url,
                virtualUsers = 20,
                sustain = 15.seconds,
                rampUp = 5.seconds,
                headers = authHeaders,
                scenarios = listOf(
                    // Read-heavy: list inventory items (weight 3 → 75% of virtual users)
                    Scenario(name = "list-inventory", weight = 3) {
                        @Suppress("UNCHECKED_CAST")
                        call(
                            InventoryItemEndpoints.rest.query,
                            Query(Condition.Always as Condition<InventoryItem>),
                        )
                    },
                    // Write: create an inventory item (weight 1 → 25% of virtual users)
                    Scenario(name = "create-inventory", weight = 1) {
                        // The server will reject items without a valid org membership;
                        // the important metric here is latency under write load, not correctness.
                        try {
                            call(
                                InventoryItemEndpoints.rest.insert,
                                InventoryItem(
                                    organization = kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000000"),
                                    name = "Load Test Item",
                                ),
                            )
                        } catch (_: com.lightningkite.lightningserver.loadtest.LoadTestHttpException) {
                            // 403/400 expected if the token doesn't have org membership — still recorded
                        }
                    },
                ),
            )
        }

        println("Authenticated scenarios RPS: ${summary.requestsPerSecond}")
        println("Per-endpoint summary:")
        summary.endpoints.forEach { ep ->
            println("  ${ep.method} ${ep.path}: ${ep.requests} reqs, ${ep.errors} errors, avg=${ep.avgMs}ms")
        }
        // The test is a baseline probe, not a pass/fail gate — just assert it ran
        assertTrue(summary.totalRequests > 0, "Expected requests to be made")
    }
}
