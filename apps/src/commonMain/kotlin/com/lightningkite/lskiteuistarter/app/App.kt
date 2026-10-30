package com.lightningkite.lskiteuistarter.app

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.exceptions.installSmartHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.isDevelopment
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.produceExactlyOneElement
import com.lightningkite.lskiteuistarter.app.views.SecurityPage
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.appNav
import com.lightningkite.lskiteuistarter.app.sdk.currentSession
import com.lightningkite.lskiteuistarter.app.sdk.installLoggedOutErrors
import com.lightningkite.lskiteuistarter.app.utils.fcmSetup
import com.lightningkite.lskiteuistarter.app.utils.notificationPermissions
import com.lightningkite.lskiteuistarter.app.utils.requestNotificationPermissions
import com.lightningkite.lskiteuistarter.app.views.HomePage
import com.lightningkite.lskiteuistarter.app.views.checkAppVersion
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal

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

    val rootView = produceExactlyOneElement {
        appNav(navigator) {
            appName = "LS KiteUI Starter"
            ::navItems {
                listOf(
                    NavLink(title = "Home", icon = Icon.home) { HomePage() },
                    NavLink(title = "Members", icon = Icon.group) { MembersPage() },
                    NavLink(title = "Security", icon = Icon.person) { SecurityPage() },
                )
            }

            ::exists {
                navigator.currentPage() !is FullscreenPage
            }
        }
    }

    // Lets AI agents drive the running dev app; start the relay with `./gradlew :apps:aiDriverStart`.
    if (Platform.isDevelopment) AiDriver.connect(appName = "ls-kiteui-starter", rootView = { rootView }, navigator = { navigator })
}

interface FullscreenPage


