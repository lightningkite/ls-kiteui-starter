// by Claude — UI tests for programmatic navigation between screens
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.testing.UiTestConfig
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.l2.navigatorView
import com.lightningkite.lskiteuistarter.sdk.apiOverride
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.lskiteuistarter.testing.*
import kotlin.test.Test
import kotlin.test.assertTrue

class NavigationUiTest {

    /**
     * Runs a navigation test with a real PageNavigator and navigatorView,
     * so page swaps are rendered and observable via snapshot.
     */
    private fun navigationTest(
        initialPage: com.lightningkite.kiteui.navigation.Page,
        block: suspend com.lightningkite.kiteui.testing.UiTestScope.(PageNavigator) -> Unit,
    ) {
        val mock = defaultMockApi()
        apiOverride = mock
        sessionToken.value = "fake-test-token"
        try {
            val navigator = PageNavigator { testRoutes }
            uiTest(
                config = UiTestConfig(navigator = navigator),
                content = {
                    pageNavigator = navigator
                    navigatorView(navigator)
                    navigator.navigate(initialPage)
                },
            ) {
                block(navigator)
            }
        } finally {
            apiOverride = null
            sessionToken.value = null
        }
    }

    @Test
    fun navigateToMembers() = navigationTest(initialPage = HomePage()) { navigator ->
        waitForComponent("homeTitle")

        navigator.navigate(MembersPage())

        waitForComponent("membersTitle")
        assertVisible("membersTitle")
    }

    @Test
    fun navigateToInventory() = navigationTest(initialPage = HomePage()) { navigator ->
        waitForComponent("homeTitle")

        navigator.navigate(InventoryPage())

        waitForComponent("inventoryTitle")
        assertVisible("inventoryTitle")
    }

    @Test
    fun backNavigation() = navigationTest(initialPage = HomePage()) { navigator ->
        waitForComponent("homeTitle")

        navigator.navigate(MembersPage())
        waitForComponent("membersTitle")

        navigator.goBack()
        waitForComponent("homeTitle")
        assertVisible("homeTitle")
    }
}
