package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.activityIndicator
import com.lightningkite.kiteui.views.direct.h2
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
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("/")
class LandingPage: Page, UseFullPage {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ViewWriter.render() {
        launch {
            delay(1)
            if(currentSession.await() != null){
                println("Have Session")
                pageNavigator.reset(HomePage())
            }else {
                println("NO Session")
                pageNavigator.reset(LoginPage())
            }
        }
        centered.activityIndicator()
    }
}