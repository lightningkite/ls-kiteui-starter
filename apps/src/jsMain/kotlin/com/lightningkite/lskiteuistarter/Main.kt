package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.root

fun main() {
    root(appTheme) {
        app(PageNavigator { AutoRoutes })
    }
}
