package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.exceptions.installSmartHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.buttonTheme
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.exceptionMessage
import com.lightningkite.kiteui.views.l2.appNav
import com.lightningkite.lskiteuistarter.extensions.toAppPlatform
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.installLoggedOutErrors
import com.lightningkite.lskiteuistarter.sdk.selectedApi
import com.lightningkite.lskiteuistarter.utils.fcmSetup
import com.lightningkite.lskiteuistarter.utils.notificationPermissions
import com.lightningkite.lskiteuistarter.utils.requestNotificationPermissions
import com.lightningkite.lskiteuistarter.views.HomePage
import com.lightningkite.lskiteuistarter.views.checkAppVersion
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import kotlinx.coroutines.launch

val defaultTheme = Theme.flat2("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Signal(defaultTheme)

// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken = { token: String -> fcmToken.value = token } // This is for iOS. It is used in the iOS app. Do not remove.

fun ViewWriter.app(navigator: PageNavigator) {
    context.exceptionHandlers.installSmartHandlers()
    context.exceptionHandlers.installLsError()
    context.exceptionHandlers.installLoggedOutErrors()

    AppScope.reactiveSuspending {
        if (currentSession() == null) return@reactiveSuspending
        val permission = notificationPermissions()
        when (permission) {
            false -> {}

            true -> {
                fcmSetup()
            }

            null -> {
                context.confirmDanger(
                    "Send notifications?",
                    "LS KiteUI Starter would like to send you notifications.",
                    "Allow"
                ) {
                    requestNotificationPermissions()
                }
            }
        }
    }

    checkAppVersion()

    return appNav(navigator) {
        appName = "LS KiteUI Starter"
        ::navItems {
            listOf(
                NavLink(title = "Home", icon = Icon.home) { HomePage() },
            )
        }

        ::exists {
            navigator.currentPage() !is FullscreenPage
        }
    }
}

interface FullscreenPage


