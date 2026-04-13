package com.lightningkite.lskiteuistarter.views

import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.buttonTheme
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.exceptionMessage
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.lskiteuistarter.extensions.toAppPlatform
import com.lightningkite.lskiteuistarter.platform
import com.lightningkite.lskiteuistarter.sdk.selectedApi
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import kotlinx.coroutines.launch

private var appUpdateChecked = false

fun ElementWriter.checkAppVersion() {
    if (Platform.current == Platform.Web || appUpdateChecked) return
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