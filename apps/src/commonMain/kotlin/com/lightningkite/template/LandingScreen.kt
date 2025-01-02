package com.lightningkite.template

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.activityIndicator
import com.lightningkite.kiteui.views.direct.h2
import com.lightningkite.template.sdk.currentSession
import kotlinx.coroutines.launch

@Routable("/")
class LandingScreen: Screen, UseFullScreen {
    override val title: Readable<String> get() = Constant("Home")
    override fun ViewWriter.render(){
        centered - activityIndicator()
        launch {
            if(currentSession.await() != null){
                println("Have Session")
                screenNavigator.reset(HomeScreen())
            }else {
                println("NO Session")
                screenNavigator.reset(LoginScreen())
            }
        }
    }
}