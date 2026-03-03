// by Claude — UI tests for auth guard redirects on protected pages
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.lskiteuistarter.testing.*
import kotlin.test.Test
import kotlin.test.assertTrue

class AuthGuardUiTest {

    @Test
    fun landingRedirectsToLoginUnauthenticated() = unauthenticatedUiTest(
        renderContent = { with(LandingPage()) { render() } },
    ) {
        // LandingPage checks currentSession, redirects to LoginPage when null
        waitFor(description = "redirect to LoginPage") {
            val snap = it
            snap.page.contains("Login") || snap.components.any { c ->
                c.id.contains("Login", ignoreCase = true) || c.value?.contains("Sign in") == true
            }
        }
    }

    @Test
    fun landingRedirectsToHomeAuthenticated() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(LandingPage()) { render() } },
    ) {
        // LandingPage checks currentSession, redirects to HomePage when authenticated
        waitFor(description = "redirect to HomePage") {
            val snap = it
            snap.page.contains("Home") || snap.components.any { c ->
                c.id.contains("homeTitle", ignoreCase = true)
            }
        }
    }

    @Test
    fun homeRedirectsUnauthenticated() = unauthenticatedUiTest(
        renderContent = { with(HomePage()) { render() } },
    ) {
        // HomePage has reactive guard: if currentSession() == null → reset to LandingPage
        waitFor(description = "redirect when unauthenticated") {
            val snap = it
            snap.page.contains("Landing") || snap.page.contains("Login") || snap.components.any { c ->
                c.value?.contains("Sign in") == true
            }
        }
    }

    @Test
    fun membersRedirectsUnauthenticated() = unauthenticatedUiTest(
        renderContent = { with(MembersPage()) { render() } },
    ) {
        waitFor(description = "redirect when unauthenticated") {
            val snap = it
            snap.page.contains("Landing") || snap.page.contains("Login") || snap.components.any { c ->
                c.value?.contains("Sign in") == true
            }
        }
    }

    @Test
    fun inventoryRedirectsUnauthenticated() = unauthenticatedUiTest(
        renderContent = { with(InventoryPage()) { render() } },
    ) {
        waitFor(description = "redirect when unauthenticated") {
            val snap = it
            snap.page.contains("Landing") || snap.page.contains("Login") || snap.components.any { c ->
                c.value?.contains("Sign in") == true
            }
        }
    }
}
