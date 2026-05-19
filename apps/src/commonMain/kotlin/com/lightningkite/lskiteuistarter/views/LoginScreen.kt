package com.lightningkite.lskiteuistarter.views

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.auth.AuthComponent
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.lightningserver.auth.AuthEndpoints
import com.lightningkite.lskiteuistarter.FullscreenPage
import com.lightningkite.lskiteuistarter.sdk.ApiOption
import com.lightningkite.lskiteuistarter.sdk.selectedApi
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember

@Routable("/login")
class LoginPage : Page, FullscreenPage {
    override val title: Reactive<String> get() = Constant("Home")

    companion object {
        const val SECRET_FOR_API_SELECTOR = "i am a dev"
    }

    val backendSelectorEnabled = PersistentProperty("backendSelectorEnabled", false)

    override fun ElementWriter.CanAddTheme.render() {
        val authUI = remember {
            val api = selectedApi().api
            val anon = AuthComponent(
                endpoints = AuthEndpoints(
                    subjects = mapOf("User" to api.userAuth),
                    emailProof = api.userAuth.email,
                    oneTimePasswordProof = api.userAuth.totp,
                    backupCodeProof = api.userAuth.backupCode,
//                    webAuthNProof = api.webAuthNProof,
//                    webAuthNIncludePasskeyUI = true,
                    passwordProof = api.userAuth.password,
                ),
//                subjectPath = "user",
                subject = api.userAuth
            ) { token ->
                sessionToken set token
                context.pageNavigator.reset(HomePage())
            }
            anon
        }

        frame {
            reactive {
                if (authUI().primaryIdentifier()?.value == SECRET_FOR_API_SELECTOR) backendSelectorEnabled.value = true
            }

            centered.sizedBox(SizeConstraints(maxWidth = 40.rem)).scrolling.col {
                centered.h4("Lightning Server and KiteUI Template")
                centered.text("This template is your bare bones starting point")
                centered.text("Sign in to get started")

                shownWhen { backendSelectorEnabled() }.field("Server") {
                    select {
                        bind(selectedApi, ApiOption.entries.toList().let(::Constant)) { it.apiName }
                    }
                }

                frame auth@{
                    reactive {
                        clearChildren()
                        authUI().render(this@auth)
                    }
                }
            }
        }
    }
}