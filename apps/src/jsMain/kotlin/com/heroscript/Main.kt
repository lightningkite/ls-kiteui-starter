package com.heroscript

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.root
import com.heroscript.utils.installKiteUiLogFilter
import com.heroscript.views.AutoRoutes

fun main() {
    // Silence noisy kiteui internal debug logs that flood the JS console.
    // Set window.heroscriptVerboseLogs = true before reload to re-enable.
    installKiteUiLogFilter()
    root(appTheme) {
        app(PageNavigator { AutoRoutes }, PageNavigator { AutoRoutes })
    }
}
