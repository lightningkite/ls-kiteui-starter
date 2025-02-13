package com.lightningkite.template

import com.lightningkite.kiteui.forms.prepareModelsClient
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.DefaultSerializersModule
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.serialization.ClientModule

val appTheme = Property<Theme>(blackstoneTheme)

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
            listOfNotNull(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomeScreen() } },
                NavLink(title = { "My Stuff" }, icon = { Icon.star }) { { MyStuffPage() } },
                NavLink(title = { "Recipes" }, icon = { Icon.list }) { { RecipesPage() } }.takeIf { showRecipes() },
                NavLink(title = { "Social" }, icon = { Icon.email }) { { PostsPage() } },
                NavLink(title = { "Profile" }, icon = { Icon.person }) { { ProfilePage() } },
            )
        }

        ::actions {
            listOf(
                NavAction("Notifications", Icon.notificationFilled) {

                }
            )
        }

        ::exists {
            navigator.currentScreen() !is UseFullScreen
        }

    }
}

interface UseFullScreen
