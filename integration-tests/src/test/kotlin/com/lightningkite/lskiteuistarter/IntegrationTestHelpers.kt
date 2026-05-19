// by Claude — Test helpers for integration tests: real server + KiteUI frontend, no HTTP
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.testing.LocalUiTestBackend
import com.lightningkite.kiteui.testing.UiTestBackend
import com.lightningkite.kiteui.testing.UiTestScope
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.ssrDispatcher
import com.lightningkite.kiteui.views.l2.navigatorView
import com.lightningkite.lightningserver.auth.GrantedScope
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.runtime.test.TestRunner
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.data.OrganizationEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints
import com.lightningkite.lskiteuistarter.sdk.LiveApi
import com.lightningkite.lskiteuistarter.sdk.apiOverride
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.reactive.core.AppJob
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.insertOne
import com.lightningkite.toEmailAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

/** Captures the TestRunner from context parameters. */
context(runner: TestRunner<*>)
private fun getRuntime(): ServerRuntime = runner

/**
 * Runs a KiteUI UI test backed by a real Lightning Server with RAM database.
 * The frontend talks to the server via [TestRunnerFetcher] — no HTTP involved.
 *
 * Internally replicates `uiTest` setup so the [PageNavigator] is wired into the
 * test backend, enabling `navigate()` calls inside the test block.
 *
 * The [setup] block runs BEFORE the page renders to ensure sessions and data are
 * ready before any page guards check `currentSession()`.
 *
 * @param initialPage The page to navigate to initially (defaults to LandingPage).
 * @param mocks Optional [MockExternalServices] for file pickers, geolocation, etc.
 * @param setup Block to seed data and log in. Runs before page render.
 * @param block The test body with access to UI testing and server-side helpers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun integrationTest(
    initialPage: Page = LandingPage(),
    mocks: MockExternalServices? = null,
    setup: (suspend (ServerRuntime) -> Unit)? = null,
    block: suspend UiTestScope.(runtime: ServerRuntime) -> Unit,
) {
    Server.test(settings = { database set Database.Settings("ram") }) {
        val runtime: ServerRuntime = getRuntime()
        val fetcher = TestRunnerFetcher(runtime)
        val api = LiveApi(fetcher)
        val navigator = PageNavigator { AutoRoutes }

        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val context = RContext("/")
            // Use Unconfined directly for view coroutines so they don't route through
            // TestMainDispatcher. After resetMain(), TestMainDispatcher throws; Unconfined
            // doesn't, so dangling view coroutines (e.g. LandingPage's delay(1)) don't
            // crash the next test.
            context.ssrDispatcher = Dispatchers.Unconfined
            val root = Frame(context)

            // Install mock external services (file picker, geolocation, etc.) if provided
            if (mocks != null) {
                context.addons["externalServices"] = mocks
            }

            root.run {
                // Clean stale global state from any previous test
                sessionToken.value = null
                apiOverride = null
                AppJob.children.forEach { it.cancel() }

                apiOverride = api
                selectedThemeName.value = "Flat (Blue)"

                // Seed data and log in BEFORE the page renders so currentSession() is ready
                if (setup != null) {
                    runBlocking { setup(runtime) }
                }

                pageNavigator = navigator
                navigatorView(navigator)
                navigator.navigate(initialPage)
            }
            root.postSetup()

            // Wire the real navigator into the backend so navigate() works in tests
            val backend = LocalUiTestBackend(
                root = { root },
                navigator = { navigator },
            )
            val scope = UiTestScope(backend)

            runBlocking(Dispatchers.Unconfined) {
                try {
                    scope.block(runtime)
                } finally {
                    apiOverride = null
                    sessionToken.value = null
                    AppJob.children.forEach { it.cancel() }
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}

// -- Server-side helper functions (extension on ServerRuntime) --

/**
 * Create a user directly in the server database (bypasses endpoints/permissions).
 */
context(_: ServerRuntime)
suspend fun createUser(
    email: String = "test@example.com",
    name: String = "Test User",
    role: UserRole = UserRole.User,
): User {
    return UserEndpoints.info.table().insertOne(
        User(email = email.toEmailAddress(), name = name, role = role)
    )!!
}

/**
 * Create a real session for a user and set the frontend's session token.
 * This goes through the server's real session system (not a mock).
 *
 * After calling this, `currentSession()` in the KiteUI frontend will resolve
 * to a real UserSession backed by the server.
 */
context(_: ServerRuntime)
suspend fun loginAs(user: User) {
    val (_, refreshToken) = UserAuth.session.createSession(
        subjectId = user._id,
        scopes = setOf(GrantedScope.root),
    )
    sessionToken.value = refreshToken.string
}

// -- Test data seeding --

/** Bundled result from seeding a user + org + membership. */
data class TestOrg(val user: User, val org: Organization, val membership: Membership)

/** Creates a user, org, and membership directly in the server database. */
context(_: ServerRuntime)
suspend fun seedTestOrg(
    email: String = "member@test.com",
    userName: String = "Test Member",
    orgName: String = "Test Org",
    memberRole: MemberRole = MemberRole.Admin,
): TestOrg {
    val user = createUser(email = email, name = userName, role = UserRole.User)
    val org = OrganizationEndpoints.info.table().insertOne(
        Organization(name = orgName)
    )!!
    val membership = MembershipEndpoints.info.table().insertOne(
        Membership(organization = org._id, user = user._id, role = memberRole)
    )!!
    return TestOrg(user, org, membership)
}

// -- Snapshot polling helpers --

/** Poll until a component with [id] (debugName) appears in the view tree. */
suspend fun UiTestScope.pollForComponent(id: String) = waitForId(id)

/** Poll until all [texts] appear somewhere in the root snapshot. */
suspend fun UiTestScope.pollForTexts(vararg texts: String) {
    waitFor(description = "texts: ${texts.toList()}") {
        val snap = snapshot()
        texts.all { snap.contains(it) }
    }
}

/** Poll until none of [texts] appear in the root snapshot. */
suspend fun UiTestScope.pollForNoTexts(vararg texts: String) {
    waitFor(description = "no texts: ${texts.toList()}") {
        val snap = snapshot()
        texts.none { snap.contains(it) }
    }
}
