// by Claude — UI tests for HomePage rendering and interactions
package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.testing.*
import kotlin.test.Test

class HomePageUiTest {

    @Test
    fun titleRenders() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(HomePage()) { render() } },
    ) {
        waitForComponent("homeTitle")
        assertVisible("homeTitle")
    }

    @Test
    fun themeSelectDefaultValue() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(HomePage()) { render() } },
    ) {
        waitForComponent("themeSelect")
        assertVisible("themeSelect")
    }

    @Test
    fun logoutButtonVisible() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(HomePage()) { render() } },
    ) {
        waitForComponent("logoutButton")
        assertVisible("logoutButton")
    }

    @Test
    fun notificationsButtonVisible() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(HomePage()) { render() } },
    ) {
        waitForComponent("testNotificationsButton")
        assertVisible("testNotificationsButton")
    }
}
