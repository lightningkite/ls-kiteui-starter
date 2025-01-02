package com.lightningkite.template

import android.os.Bundle
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.Throwable_report
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.ReactiveContext
import io.sentry.Sentry

class MainActivity : KiteUiActivity() {
    companion object {
        val main = ScreenNavigator { AutoRoutes }
        val dialog = ScreenNavigator { AutoRoutes }
    }

    override val theme: ReactiveContext.() -> Theme
        get() = { appTheme() }

    override val mainNavigator: ScreenNavigator get() = main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        codeCacheDir.setReadOnly()

        Throwable_report = { ex, ctx ->
            ex.printStackTrace2()
            Sentry.captureException(ex)
        }

        with(viewWriter) {
            app(main, dialog)
        }
    }
}
