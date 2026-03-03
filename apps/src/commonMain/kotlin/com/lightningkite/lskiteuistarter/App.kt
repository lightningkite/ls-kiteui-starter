// Application root. Navigation links, theme config, and page registration. — by Claude
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.aidriver.AiDriver
import com.lightningkite.kiteui.exceptions.ExceptionToMessages
import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.confirmDanger
import com.lightningkite.kiteui.views.l2.appNav
import com.lightningkite.kiteui.views.produceOne
import com.lightningkite.lskiteuistarter.extensions.toAppPlatform
import com.lightningkite.lskiteuistarter.sdk.*
import com.lightningkite.lskiteuistarter.utils.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import com.lightningkite.services.database.*
import kotlinx.coroutines.launch

// by Claude — Theme options for the app
val themes = mapOf(
    "Flat (Blue)" to Theme.flat2("flat-blue", Angle(0.55f)),
    "Flat (Green)" to Theme.flat2("flat-green", Angle(0.33f)),
    "Flat (Purple)" to Theme.flat2("flat-purple", Angle(0.75f)),
    "Material" to Theme.material3("material3"),
)
val selectedThemeName = PersistentProperty("selectedTheme", "Flat (Blue)")
val appTheme = Signal<Theme>(themes.values.first()).also {
    // by Claude — sync persisted theme name to appTheme signal
    AppScope.reactiveSuspending {
        val name = selectedThemeName()
        it.value = themes[name] ?: themes.values.first()
    }
}

// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken =
    { token: String -> fcmToken.value = token } //This is for iOS. It is used in the iOS app. Do not remove.

var appUpdateChecked = false

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
    ExceptionToMessages.root.installLsError()
    ExceptionToMessages.root.installLoggedOutErrors()

    AppScope.reactiveSuspending {
        if (currentSession() == null) return@reactiveSuspending
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


    if (Platform.current != Platform.Web && !appUpdateChecked) {
        appUpdateChecked = true
        AppScope.launch {
            val currentBuild = Build.version
            val releases = try {
                selectedApi.await().api.appRelease.query(
                    Query(
                        condition { it.platform.eq(Platform.current.toAppPlatform()) }
                    ))
            } catch (_: Exception) {
                return@launch
            }

            val currentRelease = releases.find { it.version == currentBuild } ?: return@launch
            val latestRelease = releases.maxByOrNull { it.releaseDate } ?: return@launch
            if (latestRelease._id != currentRelease._id) {
                dialogPageNavigator.navigate(
                    UpdateDialog(
                        newVersion = latestRelease.version,
                        forceUpdate = releases.any { it.requiredUpdate && it.releaseDate > currentRelease.releaseDate }
                    )
                )
            }
        }
    }

    navigator.navigate(LandingPage())
    // by Claude — Updated app name and added Members nav link
    val rootView = produceOne {
        appNav(navigator, dialog) {
            appName = "My App"
            ::navItems {
                listOfNotNull(
                    NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                    if (currentSession() != null)
                        NavLink(title = { "Members" }, icon = { Icon.group }) { { MembersPage() } }
                    else null,
                    if (currentSession() != null)  // by Claude
                        NavLink(title = { "Inventory" }, icon = { Icon.list }) { { InventoryPage() } }
                    else null,
                )
            }

            ::exists {
                navigator.currentPage() !is UseFullPage
            }
        }
    }
    // by Claude — connect AI driver for LLM-driven UI testing (dev builds only)
    if (Platform.isDevelopment) {
        AiDriver.connect(
            appName = "LS KiteUI Starter",
            rootViewProvider = { rootView },
            navigatorProvider = { navigator }
        )
    }
}

interface UseFullPage


