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
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.lskiteuistarter.extensions.toAppPlatform
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.installLoggedOutErrors
import com.lightningkite.lskiteuistarter.sdk.selectedApi
import com.lightningkite.lskiteuistarter.utils.fcmSetup
import com.lightningkite.lskiteuistarter.utils.notificationPermissions
import com.lightningkite.lskiteuistarter.utils.requestNotificationPermissions
import com.lightningkite.lskiteuistarter.views.HomePage
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import kotlinx.coroutines.launch

//val defaultTheme = brandBasedExperimental("bsa", normalBack = Color.white)
val defaultTheme = Theme.flat2("default", Angle(0.55f))// brandBasedExperimental("bsa", normalBack = Color.white)
val appTheme = Signal(defaultTheme)

// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken = { token: String -> fcmToken.value = token } //This is for iOS. It is used in the iOS app. Do not remove.

private var appUpdateChecked = false

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
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

    if (Platform.current != Platform.Web && !appUpdateChecked) {
        appUpdateChecked = true
        AppScope.launch {
            val currentBuild = Build.version
            val releases = try {
                selectedApi.await().api.appRelease.query(
                    Query(
                        condition { it.platform.eq(Platform.current.toAppPlatform()) }
                    )
                )
            } catch (_: Exception) {
                return@launch
            }

            val currentRelease = releases.find { it.version == currentBuild } ?: return@launch
            val latestRelease = releases.maxByOrNull { it.releaseDate } ?: return@launch
            if (latestRelease._id != currentRelease._id) {
                val updateRequired = releases
                    .asSequence()
                    .filter { it.releaseDate > currentRelease.releaseDate }
                    .any { it.requiredUpdate }

                // If an update is required then there are probably breaking changes. No need to spam the user with error dialogs until the app has been updated.
                if (updateRequired) context.exceptionHandlers += ExceptionHandler(5f) { e, meta ->
                    val message = exceptionMessage(e, meta)
                    println(
                        buildString {
                            appendLine("Suppressing error: ${message?.title ?: e}")
                            message?.let { appendLine(message.body) }
                        }
                    )
                    e.printStackTrace()
                    return@ExceptionHandler {}
                }

                context.dialog(dismissable = !updateRequired) { close ->
                    col {
                        h1 {
                            align = Align.Center
                            content = "New Version Available"
                        }

                        centered.sizeConstraints(width = 30.rem).padded.text {
                            align = Align.Center
                            content =
                                if (updateRequired)
                                    "We've released version ${latestRelease.version} with important updates that are required to continue using the app. Please update now to enjoy the latest features, improvements, and security enhancements."
                                else
                                    "Good news! Version ${latestRelease.version} is now available with new features and improvements. We recommend updating soon to get the best experience, but you can continue using the current version for now."
                        }

                        row {
                            if (!updateRequired) card.buttonTheme.button {
                                centered.text("OK")
                                onClick { close() }
                            }

//                            TODO: When apps are published replace
//                            expanding.buttonTheme.button {
//                                centered.text("Go To Store")
//                                onClick {
//                                    context.toast("Replace toast with store url")
//                                }
//                            }
                        }
                    }
                }
            }
        }
    }

    return appNav(navigator, dialog) {
        appName = "LS KiteUI Starter"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
//                NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootPage } },
//                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
            )
        }

        ::exists {
            navigator.currentPage() !is FullscreenPage
        }
    }
}

interface FullscreenPage


