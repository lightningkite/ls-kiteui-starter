// by Claude — Integration tests for HomePage: theme, logout, auth guard
package com.lightningkite.lskiteuistarter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeIntegrationTest {

    @Test
    fun authenticatedUserSeesHomePage() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("homeTitle")
        assertTextVisible("Welcome to your home page")
    }

    @Test
    fun themeSelectDefaultsToFlatBlue() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("themeSelect")
        assertTrue(snapshot("themeSelect").contains("Flat (Blue)"), "Expected default theme 'Flat (Blue)'")
    }

    @Test
    fun themeSelectCanBeChanged() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("themeSelect")
        setValue("themeSelect", "Material")
        waitFor(description = "theme changed to Material") { snapshot("themeSelect").contains("Material") }
    }

    @Test
    fun logoutRedirectsToLogin() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("logoutButton")
        click("logoutButton")
        pollForTexts("Sign in to get started")
    }

    @Test
    fun unauthenticatedUserRedirectedFromHome() = integrationTest(
        initialPage = HomePage(),
    ) { _ ->
        pollForTexts("Sign in to get started")
    }
}
