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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("/dashboard")
class HomePage : Page {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ElementWriter.CanAddTheme.render() {
        // Resetting the nav stack can't happen synchronously here, on the page's own initial
        // render: the swapView backing navigatorView hasn't finished subscribing to
        // navigator.currentPage yet at that exact moment, so a same-tick reset() lands in the
        // stack (stack.value updates) but never reaches the view -- the page silently hangs on
        // this screen instead of swapping to LandingPage. Confirmed empirically: a bare
        // `launch { reset(...) }` (no delay) reproduces the hang every time; a real delay lets
        // the swapView's own setup finish first, exactly like LandingPage's redirect already
        // works reliably because its currentSession.await() naturally takes longer than that
        // window. 50ms is a deliberate, generous margin verified stable across repeated runs
        // under load -- see integration-tests SmokeTest.loggedOutUserRedirectedFromHome.
        reactive {
            if (currentSession() == null)
                launch { delay(50); context.pageNavigator.reset(LandingPage()) }
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