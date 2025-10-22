package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.exceptions.ExceptionToMessages
import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.DefaultSerializersModule
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.confirmDanger
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lskiteuistarter.utils.fcmSetup
import com.lightningkite.lskiteuistarter.utils.notificationPermissions
import com.lightningkite.lskiteuistarter.utils.requestNotificationPermissions
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.files.*
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.installLoggedOutErrors
import kotlin.uuid.Uuid

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat2("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Signal<Theme>(defaultTheme)

// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken =
    { token: String -> fcmToken.value = token } //This is for iOS. It is used in the iOS app. Do not remove.


fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator): ViewModifiable {
    ExceptionToMessages.root.installLsError()
    ExceptionToMessages.root.installLoggedOutErrors()

    AppScope.reactiveSuspending {
        if(currentSession() == null) return@reactiveSuspending
        val permission = notificationPermissions()
        when (permission) {
            false -> {}

            true -> {
                fcmSetup()
            }

            null -> {
                confirmDanger(
                    "Send notifications?",
                    "LS KiteUI Starter would like to send you notifications.",
                    "Allow"
                ) {
                    requestNotificationPermissions()
                }
            }
        }
    }

    navigator.navigate(LandingPage() )
    return appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
//                NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootPage } },
//                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
            )
        }

        ::exists {
            navigator.currentPage() !is UseFullPage
        }
    }
}

interface UseFullPage


