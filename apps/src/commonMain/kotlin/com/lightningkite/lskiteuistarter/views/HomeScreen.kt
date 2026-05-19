package com.lightningkite.lskiteuistarter.views

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lskiteuistarter.FcmToken
import com.lightningkite.lskiteuistarter.fcmToken
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

@Routable("/dashboard")
class HomePage : Page {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            if (currentSession() == null)
                context.pageNavigator.reset(LandingPage())
        }

        col {
            centered.h2("Welcome to your home page")

            expanding.space()

            important.buttonTheme.button {
                centered.text("Test Notifications")
                ::enabled { fcmToken() != null }
                onClick {
                    currentSession()?.api?.fcmToken?.testInAppNotifications(FcmToken.ID(fcmToken()!!))
                }
            }

            important.buttonTheme.button {
                centered.text("Logout")
                onClick {
                    try {
                        currentSession()?.api?.userAuth?.terminateSession()
                    } catch (_: Exception) {

                    } finally {
                        sessionToken set null
                        context.pageNavigator.reset(LoginPage())
                    }
                }
            }
        }
    }
}