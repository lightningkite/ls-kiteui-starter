// Smoke tests validating the integration harness: real server (RAM DB) + KiteUI frontend in-process.
package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.lskiteuistarter.views.HomePage
import com.lightningkite.lskiteuistarter.views.LoginPage
import kotlin.test.Test

class SmokeTest {

    /** Unauthenticated: the login page renders its call-to-action. */
    @Test
    fun loginScreenRenders() = integrationTest(
        initialPage = LoginPage(),
    ) { _ ->
        waitForText("Sign in to get started")
    }

    /** Authenticated: a user lands on their home page. Proves seeding + real session + page render. */
    @Test
    fun authedUserSeesHome() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val user = createUser()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForText("Welcome to your home page")
    }

    /**
     * Logged-out: navigating straight to a guarded page (HomePage) must bounce through LandingPage
     * to LoginPage instead of hanging. HomePage's auth guard defers its `reset(LandingPage())` past a
     * real suspension point -- see the comment on the guard in HomeScreen.kt for why a same-tick
     * reset() silently never reaches the view. This is the one path the happy-path `authedUserSeesHome`
     * test above can't catch, since it only fires when logged out.
     */
    @Test
    fun loggedOutUserRedirectedFromHome() = integrationTest(
        initialPage = HomePage(),
    ) { _ ->
        waitForText("Sign in to get started")
    }

    /**
     * Session loss while already sitting on a guarded page (e.g. the token gets cleared by another
     * tab, or expires) must also redirect to LoginPage, not just the arrives-already-logged-out case
     * above. Exercises the guard's continuous (not just initial) reactivity.
     */
    @Test
    fun sessionLossWhileOnHomeRedirectsToLogin() = integrationTest(
        initialPage = HomePage(),
        setup = { runtime ->
            with(runtime) {
                val user = createUser()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForText("Welcome to your home page")
        sessionToken.value = null
        waitForText("Sign in to get started")
    }
}
