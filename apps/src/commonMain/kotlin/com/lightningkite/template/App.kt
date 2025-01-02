package com.lightningkite.template

import com.lightningkite.kiteui.forms.prepareModelsClient
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.DefaultSerializersModule
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.serialization.ClientModule

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Property<Theme>(defaultTheme)

// Notification Items
val fcmToken: Property<String?> = Property(null)
val setFcmToken =
    { token: String -> fcmToken.value = token } //This is for iOS. It is used in the iOS app. Do not remove.


fun ViewWriter.app(navigator: ScreenNavigator, dialog: ScreenNavigator) {

    prepareModelsShared()
    prepareModelsClient()
    com.lightningkite.prepareModelsShared()

    DefaultSerializersModule = ClientModule

    navigator.navigate(LandingScreen() )
    appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomeScreen() } },
//                NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootScreen } },
//                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchScreen } },
            )
        }

        ::exists {
            navigator.currentScreen() !is UseFullScreen
        }

    }
}

interface UseFullScreen
