package com.lightningkite.lskiteuistarter.views

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.activityIndicator
import com.lightningkite.lskiteuistarter.FullscreenPage
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("/")
class LandingPage : Page, FullscreenPage {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ElementWriter.CanAddTheme.render() {
        launch {
            delay(1)
            if (currentSession.await() != null) {
                println("Have Session")
                pageNavigator.reset(HomePage())
            } else {
                println("NO Session")
                pageNavigator.reset(LoginPage())
            }
        }
        frame {
            centered.activityIndicator()
        }
    }
}