package com.heroscript.views

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.openLink
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.heroscript.FullscreenPage
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

// TODO: replace placeholders with real store URLs once the apps are published.
private const val ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.heroscript"
private const val IOS_STORE_URL = "https://apps.apple.com/app/heroscript/id000000000"

@Routable("/update-required")
class ForcedUpdatePage : Page, FullscreenPage {
    override val title: Reactive<String> get() = Constant("Update Required")

    override fun ElementWriter.CanAddTheme.render() {
        frame {
            centered.sizedBox(SizeConstraints(maxWidth = 30.rem)).padded.col {
                h1 {
                    align = Align.Center
                    content = "Update Required"
                }
                text {
                    align = Align.Center
                    content = "An update is required to continue using HeroScript."
                }
                centered.important.button {
                    text("Update now")
                    onClick {
                        val url = when (Platform.current) {
                            Platform.iOS -> IOS_STORE_URL
                            Platform.Android -> ANDROID_STORE_URL
                            else -> return@onClick
                        }
                        context.openLink(url)
                    }
                }
            }
        }
    }
}
