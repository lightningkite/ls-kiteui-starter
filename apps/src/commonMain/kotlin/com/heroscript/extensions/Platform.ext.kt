package com.heroscript.extensions

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.heroscript.AppPlatform


fun Platform.toAppPlatform(): AppPlatform = when (this) {
    Platform.iOS -> AppPlatform.iOS
    Platform.Android -> AppPlatform.Android
    Platform.Web -> AppPlatform.Web
    Platform.Desktop -> AppPlatform.Desktop
}

