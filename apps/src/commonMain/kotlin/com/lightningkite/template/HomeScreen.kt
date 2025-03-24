package com.lightningkite.template

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.readable.Constant
import com.lightningkite.readable.Readable
import com.lightningkite.readable.await
import com.lightningkite.readable.reactive
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.template.sdk.currentSession
import com.lightningkite.template.sdk.sessionToken
import kotlinx.coroutines.launch

@Routable("/dashboard")
class HomePage: Page {
    override val title: Readable<String> get() = Constant("Home")
    override fun ViewWriter.render(): ViewModifiable {

        reactive {
            if(currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        return col {
            centered - h2("Welcome to your home page")

            expanding - space()

            important - buttonTheme - button {
                centered - text("Logout")
                onClick {
                    try {
                        currentSession.await()?.userAuth?.terminateSession()
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