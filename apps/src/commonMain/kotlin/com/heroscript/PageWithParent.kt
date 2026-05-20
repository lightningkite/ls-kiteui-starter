package com.heroscript

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

interface SizedPage : Page {
    val minWidth: Reactive<Dimension> get() = Constant(25.rem)
}

interface PageWithParent : Page, SizedPage {
    val parentPage: Page? get() = null
}
