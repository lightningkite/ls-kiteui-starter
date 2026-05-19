// by Claude — Integration tests for navigation flows and auth guards across pages
package com.lightningkite.lskiteuistarter

import kotlin.test.Test

class NavigationIntegrationTest {

    // -- Auth guard redirects --

    @Test
    fun landingPageRedirectsToLoginWhenUnauthenticated() = integrationTest(
        initialPage = LandingPage(),
    ) { _ ->
        // LandingPage checks currentSession, finds null, redirects to LoginPage
        pollForTexts("Sign in to get started")
    }

    @Test
    fun landingPageRedirectsToHomeWhenAuthenticated() = integrationTest(
        initialPage = LandingPage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        // LandingPage checks currentSession, finds session, redirects to HomePage
        pollForComponent("homeTitle")
    }

    // -- Cross-page navigation --

    @Test
    fun navigateFromHomeToInventory() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        pollForComponent("homeTitle")
        navigate("inventory")
        pollForComponent("inventoryTitle")
    }

    @Test
    fun navigateFromHomeToMembers() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        pollForComponent("homeTitle")
        navigate("members")
        pollForComponent("membersTitle")
    }

    @Test
    fun navigateFromInventoryToEditAndBack() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        pollForComponent("addItemButton")
        click("addItemButton")

        pollForComponent("editTitle")
        click("cancelButton")

        pollForComponent("inventoryTitle")
    }

    @Test
    fun fullNavigationCycle() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        // Home → Members → Inventory → Home
        pollForComponent("homeTitle")

        navigate("members")
        pollForComponent("membersTitle")

        navigate("inventory")
        pollForComponent("inventoryTitle")

        navigate("dashboard")
        pollForComponent("homeTitle")
    }
}
