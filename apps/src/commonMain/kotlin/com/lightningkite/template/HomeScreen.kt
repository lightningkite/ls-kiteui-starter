package com.lightningkite.template

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactive
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.template.sdk.currentSession
import com.lightningkite.template.sdk.sessionToken
import kotlinx.coroutines.launch

@Routable("/dashboard")
class HomeScreen: Screen {
    override val title: Readable<String> get() = Constant("Home")
    override fun ViewWriter.render(){

        reactive {
            if(currentSession() == null)
                screenNavigator.reset(LandingScreen())
        }

        col {
            centered - h2("Welcome to your home screen")

            expanding - space()

            important - buttonTheme - button {
                centered - text("Logout")
                onClick {
                    try {
                        currentSession.await()?.userAuth?.terminateSession()
                    } catch (e:Exception){

                    } finally {
                        sessionToken set null
                        screenNavigator.reset(LoginScreen())
                    }
                }
            }
        }
    }
}