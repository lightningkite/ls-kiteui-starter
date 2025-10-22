package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.files.*
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch

@Routable("/dashboard")
class HomePage: Page {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ViewWriter.render(): ViewModifiable {

        reactive {
            if(currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        return col {
            centered - h2("Welcome to your home page")

            expanding - space()

            important - buttonTheme - button {
                centered - text("Test Notifications")
                ::enabled { fcmToken() != null }
                onClick {
                    currentSession()?.api?.fcmToken?.testInAppNotifications(fcmToken()!!)
                }
            }

            important - buttonTheme - button {
                centered - text("Logout")
                onClick {
                    try {
                        currentSession()?.api?.userAuth?.terminateSession()
                    } catch (e:Exception){

                    } finally {
                        sessionToken set null
                        pageNavigator.reset(LoginPage())
                    }
                }
            }
        }
    }
}