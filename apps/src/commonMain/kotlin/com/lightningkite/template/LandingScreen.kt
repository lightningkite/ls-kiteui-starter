package com.lightningkite.template

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.template.sdk.currentSession
import kotlinx.coroutines.launch

@Routable("/")
class LandingScreen: Screen, UseFullScreen {
    override val title: Readable<String> get() = Constant("Home")
    override fun ViewWriter.render(){
        col {
            important - link {
                centered - text("QR Code Flow")
                to = { InitialWelcome() }
                resetsStack = true
            }
            important - link {
                centered - text("Instantly log in")
                to = { HomeScreen() }
                resetsStack = true
            }
            important - link {
                centered - text("Alternate Mode")
                to = {
                    showRecipes.value = true
                    HomeScreen()
                }
                resetsStack = true
            }
        }
    }
}