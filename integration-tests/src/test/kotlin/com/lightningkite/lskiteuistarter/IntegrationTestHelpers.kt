// Test helpers for integration tests: real Lightning Server (RAM DB) + KiteUI frontend, no HTTP.
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.testing.UiTestScope
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.l2.navigatorView
import com.lightningkite.lightningserver.auth.GrantedScope
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.runtime.test.TestRunner
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lskiteuistarter.data.UserEndpoints.info
import com.lightningkite.lskiteuistarter.sdk.LiveApi
import com.lightningkite.lskiteuistarter.sdk.apiOverride
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.core.AppJob
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.insertOne
import kotlinx.coroutines.runBlocking

/** Captures the [TestRunner] (a [ServerRuntime]) from context parameters. */
context(runner: TestRunner<*>)
private fun getRuntime(): ServerRuntime = runner

/**
 * Runs a KiteUI [uiTest] backed by a real Lightning Server with a RAM database.
 * The frontend talks to the server via [TestRunnerFetcher] — no HTTP involved.
 *
 * The [setup] block runs inside the uiTest content block (before the page renders). Use it to seed
 * data and call [loginAs] so the session is established before any page guard checks the session.
 * Because the jvmSsr harness uses real (Unconfined) scheduling, wait for async results in [block]
 * with the built-in [UiTestScope.waitForText] / [UiTestScope.waitFor].
 *
 * @param initialPage The page to render initially.
 * @param setup Seeds data / logs in. Runs with [ServerRuntime] context before the page renders.
 * @param block The test body, with UI-driver helpers plus the [ServerRuntime].
 */
fun integrationTest(
    initialPage: Page,
    setup: (suspend (ServerRuntime) -> Unit)? = null,
    block: suspend UiTestScope.(runtime: ServerRuntime) -> Unit,
) {
    Server.test(settings = { database set Database.Settings("ram") }) {
        val runtime: ServerRuntime = getRuntime()
        val api = LiveApi(TestRunnerFetcher(runtime))
        val navigator = PageNavigator { AutoRoutes }

        // The root view whose element `job` tree owns every reactive process / live-query
        // coroutine the rendered pages launch. Captured from the content receiver (the root Frame)
        // so the test's `finally` can shut it down and cancel those coroutines — see below.
        var rootView: Element? = null

        // Persistent subscription that keeps `currentSession` ACTIVE for the whole test — see the
        // comment where it's established below. Released in teardown.
        var sessionKeepAlive: (() -> Unit)? = null

        uiTest(
            content = {
                // Clean stale global state from previous tests. The sdk `currentSession` reactive and
                // AppScope coroutines outlive individual tests and hold references to old server
                // runtimes.
                sessionToken.value = null
                apiOverride.value = null
                AppJob.children.forEach { it.cancel() }

                // Point the frontend at this test's in-process server. apiOverride is a reactive
                // Signal, so setting it forces the global `currentSession` reactive to recompute
                // against THIS test's server — otherwise a public (unauthenticated) test keeps a
                // session cached from a previous test's server/RAM-DB and seeded data appears
                // "missing".
                // The content receiver is the root Frame element; keep a handle for teardown.
                rootView = this as Element
                apiOverride.value = api
                // Mirror appBase(): pages read both of these context navigators (e.g. Links use
                // mainPageNavigator). Point them all at the one test navigator.
                context.pageNavigator = navigator
                context.mainPageNavigator = navigator

                // Keep `currentSession` ACTIVE for the whole test. Established BEFORE login so the
                // single reactive activation spans the sessionToken change loginAs() makes.
                // Without this, loginAs()'s await() adds a listener, reads the value, then removes
                // it — deactivating the reactive. When a rendered page later reads currentSession it
                // re-activates it, and that fresh startCalculation cancels the in-flight token
                // exchange (JobCancellationException -> getSelf fails -> null session). One-shot
                // pages such as HomePage then bounce to LoginPage. A persistent subscription
                // keeps the session cached and ready so page reads just attach without re-computing.
                sessionKeepAlive = currentSession.beginUse()

                // Seed data / log in BEFORE the page renders so the session resolves first.
                if (setup != null) runBlocking { setup(runtime) }

                navigatorView(navigator)
                navigator.navigate(initialPage)
            },
        ) {
            try {
                block(runtime)
            } finally {
                // Shut the root view down FIRST. The jvmSsr uiTest harness never tears the view
                // down, so every reactive process / KiteUI watch() live-query coroutine launched by
                // the rendered pages leaks past the test on the root element's `job` tree. Left
                // running, they accumulate across the suite and eventually starve later
                // permission-gated detail pages so their content never resolves in the wait window.
                // onShutdown() recursively cancels the whole element job tree, releasing them.
                @OptIn(OverrideOnly::class)
                rootView?.onShutdown()
                sessionKeepAlive?.invoke()
                apiOverride.value = null
                sessionToken.value = null
                AppJob.children.forEach { it.cancel() }
            }
        }
    }
}

// -- Server-side helpers (extensions on ServerRuntime) -------------------------------------------

/**
 * Create a user directly in the server database (bypasses endpoints/permissions).
 * Pass [role] to grant access, e.g. `UserRole.Root` for an admin.
 */
context(_: ServerRuntime)
suspend fun createUser(
    email: String = "test@example.com",
    name: String = "Test User",
    role: UserRole = UserRole.User,
): User = User.info.table().insertOne(
    User(email = email.toEmailAddress(), name = name, role = role)
)

/** Create a root-role (admin) user directly in the database. */
context(_: ServerRuntime)
suspend fun createAdmin(
    email: String = "admin@example.com",
    name: String = "Admin User",
): User = createUser(email, name, UserRole.Root)

/**
 * Create a real session for [user] and set the frontend's session token. Goes through the server's
 * real session system, so the frontend's `currentSession()` resolves to a real UserSession.
 */
context(_: ServerRuntime)
suspend fun loginAs(user: User) {
    val (_, refreshToken) = UserAuth.session.newSession(
        subjectId = user._id,
        scopes = setOf(GrantedScope.root),
    )
    sessionToken.value = refreshToken.string
    // Wait for the frontend session to actually resolve (getSelf) BEFORE the page renders. Otherwise
    // a permission-gated / owner-scoped page can render against the still-anonymous session, cache the
    // empty/403 result, and never show the authenticated data within the test window (ModelCache only
    // re-pulls after its 1-minute pullFrequency) — an in-suite race that makes such tests flaky.
    currentSession.await()
}
