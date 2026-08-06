// Smoke tests validating the integration harness: real server (RAM DB) + KiteUI frontend in-process.
package com.lightningkite.lskiteuistarter

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
}
