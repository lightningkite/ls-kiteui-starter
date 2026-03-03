// by Claude — Updated home screen with theme picker and logout
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

@Routable("/dashboard")
class HomePage : Page {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ViewWriter.render() {

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        col {
            centered.h2 {
                debugName = "homeTitle" // by Claude — testId for UI testing
                content = "Welcome to your home page"
            }

            field("Theme") {
                select {
                    debugName = "themeSelect" // by Claude — testId
                    bind(selectedThemeName, Constant(themes.keys.toList())) { it }
                }
            }

            expanding.space()

            important.buttonTheme.button {
                debugName = "testNotificationsButton" // by Claude — testId
                centered.text("Test Notifications")
                ::enabled { fcmToken() != null }
                onClick {
                    currentSession()?.api?.fcmToken?.testInAppNotifications(fcmToken()!!)
                }
            }

            important.buttonTheme.button {
                debugName = "logoutButton" // by Claude — testId
                centered.text("Logout")
                onClick {
                    try {
                        currentSession()?.api?.userAuth?.terminateSession()
                    } catch (e: Exception) {

                    } finally {
                        sessionToken set null
                        pageNavigator.reset(LoginPage())
                    }
                }
            }
        }
    }
}
