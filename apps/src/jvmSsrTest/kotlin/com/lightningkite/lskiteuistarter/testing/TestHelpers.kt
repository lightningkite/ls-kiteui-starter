// by Claude — Test helper functions for KiteUI uiTest() with mock API injection
package com.lightningkite.lskiteuistarter.testing

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.testing.UiTestConfig
import com.lightningkite.kiteui.testing.UiTestScope
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.sdk.apiOverride
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain

/** Minimal Routes that doesn't parse URLs — tests navigate programmatically. */
val testRoutes = Routes(parsers = listOf(), renderers = mapOf(), fallback = LandingPage())

/**
 * Ensures Dispatchers.Main is set before any app code initializes.
 * AppScope (used by currentSession) requires Dispatchers.Main.immediate.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private val mainDispatcherSetup: Unit by lazy {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

private fun ensureMainDispatcher() {
    mainDispatcherSetup
}

/**
 * Runs a uiTest with the mock API injected and a session token set,
 * so `currentSession` resolves to an authenticated UserSession.
 *
 * [renderContent] receives the ViewWriter and should render the page(s) under test.
 */
fun authenticatedUiTest(
    mockApi: MockApi,
    renderContent: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit,
) {
    ensureMainDispatcher()
    apiOverride = mockApi
    sessionToken.value = "fake-test-token"
    try {
        val navigator = PageNavigator { testRoutes }
        uiTest(
            config = UiTestConfig(navigator = navigator),
            content = {
                pageNavigator = navigator
                renderContent()
            },
            block = block,
        )
    } finally {
        apiOverride = null
        sessionToken.value = null
    }
}

/**
 * Runs a uiTest without authentication (sessionToken is null).
 * Useful for testing auth-guard redirects.
 *
 * [renderContent] receives the ViewWriter and should render the page(s) under test.
 */
fun unauthenticatedUiTest(
    renderContent: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit,
) {
    ensureMainDispatcher()
    apiOverride = null
    sessionToken.value = null
    try {
        val navigator = PageNavigator { testRoutes }
        uiTest(
            config = UiTestConfig(navigator = navigator),
            content = {
                pageNavigator = navigator
                renderContent()
            },
            block = block,
        )
    } finally {
        apiOverride = null
        sessionToken.value = null
    }
}
