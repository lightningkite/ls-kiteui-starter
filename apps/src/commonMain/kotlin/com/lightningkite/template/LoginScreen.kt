package com.lightningkite.template

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.forms.AuthComponent
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lightningserver.auth.AuthClientEndpoints
import com.lightningkite.template.sdk.AbstractAnonymousSession
import com.lightningkite.template.sdk.currentSession
import com.lightningkite.template.sdk.selectedApi
import com.lightningkite.template.sdk.sessionToken
import kotlinx.coroutines.launch

@Routable("/login")
class LoginScreen : Screen, UseFullScreen {
    override val title: Readable<String> get() = Constant("Home")
    override fun ViewWriter.render() {

        val authUI = shared {
            val api = selectedApi().api
            val anon = object : AbstractAnonymousSession(api) {}
            AuthComponent(
                endpoints = AuthClientEndpoints(
                    subjects = mapOf("user" to anon.userAuth),
                    authenticatedSubjects = emptyMap(),
                    emailProof = anon.emailProof,
                    oneTimePasswordProof = anon.oneTimePasswordProof,
                    passwordProof = anon.passwordProof,
                ),
                subjectPath = "user",
                subject = anon.userAuth
            ) { token ->
                sessionToken set token
                screenNavigator.reset(HomeScreen())
            }
        }

        stack {
            centered - sizedBox(SizeConstraints(maxWidth = 40.rem)) - scrolls - col {

                centered - h4("Lightning Server and KiteUI Template")

                centered - text("This template is your bare bones starting point")

                centered - text("Sign in to get started")


                stack {
                    reactive {
                        clearChildren()
                        with(authUI()) {
                            render()
                        }
                    }
                }
            }
        }

    }
}