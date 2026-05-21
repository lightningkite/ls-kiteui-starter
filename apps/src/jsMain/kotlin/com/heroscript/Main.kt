package com.heroscript

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.root
import com.heroscript.views.AutoRoutes

fun main() {
    root(appTheme) {
        app(PageNavigator { AutoRoutes }, PageNavigator { AutoRoutes })
    }
}
