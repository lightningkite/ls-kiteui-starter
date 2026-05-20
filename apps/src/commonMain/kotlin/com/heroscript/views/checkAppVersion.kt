package com.heroscript.views

import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.heroscript.extensions.toAppPlatform
import com.heroscript.platform
import com.heroscript.sdk.selectedApi
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

            if (updateRequired) {
                // Breaking changes are likely; suppress incidental error dialogs until the user updates.
                context.exceptionHandlers += ExceptionHandler(5f) { e, meta ->
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
                context.pageNavigator.reset(ForcedUpdatePage())
                return@launch
            }

            context.dialog(dismissable = true) { close ->
                col {
                    h1 {
                        align = Align.Center
                        content = "New Version Available"
                    }

                    centered.sizeConstraints(width = 30.rem).padded.text {
                        align = Align.Center
                        content =
                            "Good news! Version ${latestRelease.version} is now available with new features and improvements. We recommend updating soon to get the best experience, but you can continue using the current version for now."
                    }

                    row {
                        card.buttonTheme.button {
                            centered.text("OK")
                            onClick { close() }
                        }
                    }
                }
            }
        }
    }
}