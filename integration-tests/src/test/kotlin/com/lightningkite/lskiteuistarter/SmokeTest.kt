// Smoke tests validating the integration harness: real server (RAM DB) + KiteUI frontend in-process.
package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.api.*
import com.lightningkite.lskiteuistarter.app.*
import com.lightningkite.lskiteuistarter.app.views.*

import com.lightningkite.lskiteuistarter.app.sdk.currentSession
import com.lightningkite.lskiteuistarter.app.sdk.sessionCookieChanged
import com.lightningkite.reactive.context.await
import com.lightningkite.lskiteuistarter.app.views.HomePage
import com.lightningkite.lskiteuistarter.app.views.LoginPage
import kotlin.test.Test
import kotlin.test.assertNull

class SmokeTest {

    /** Unauthenticated: the login page renders its call-to-action. */
    @Test
    fun loginScreenRenders() = integrationTest(
        initialPage = LoginPage(),
    ) {
        waitForText("Sign in to get started")
    }

    /** Authenticated: a user lands on their home page. Proves seeding + real session + page render. */
    @Test
    fun authedUserSeesHome() = integrationTest(
        initialPage = HomePage(),
        setup = {
            val user = createUser()
            loginAs(user)
        },
    ) {
        waitForText("Welcome to your home page")
    }

    /**
     * Logged-out: navigating straight to a guarded page (HomePage) must bounce through LandingPage
     * to LoginPage instead of hanging: the guard resets the navigator during the render the
     * navigation itself caused. This is the one path the happy-path `authedUserSeesHome` test above
     * can't catch, since it only fires when logged out.
     */
    @Test
    fun loggedOutUserRedirectedFromHome() = integrationTest(
        initialPage = HomePage(),
    ) {
        waitForText("Sign in to get started")
    }

    /**
     * Session loss while already sitting on a guarded page (e.g. logged out in another tab) must also
     * redirect to LoginPage, not just the arrives-already-logged-out case above. Exercises the guard's continuous (not just initial) reactivity.
     */
    @Test
    fun sessionLossWhileOnHomeRedirectsToLogin() = integrationTest(
        initialPage = HomePage(),
        setup = {
            val user = createUser()
            loginAs(user)
        },
    ) {
        waitForText("Welcome to your home page")
        currentSession.await()!!.api.userAuth.terminateSession()
        sessionCookieChanged.value++
        waitForText("Sign in to get started")
    }

    @Test
    fun logoutEndsTheSession() = integrationTest(
        initialPage = HomePage(),
        setup = {
            val user = createUser()
            loginAs(user)
        },
    ) {
        waitForText("Welcome to your home page")
        click(findClickable("Logout").single().path)
        waitForText("Sign in to get started")
        // Asking again goes back to the server, which must no longer accept the cookie.
        sessionCookieChanged.value++
        assertNull(currentSession.await())
    }
}
