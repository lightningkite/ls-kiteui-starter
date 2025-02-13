package com.lightningkite.template

import com.lightningkite.kiteui.models.BarSemantic
import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.CriticalSemantic
import com.lightningkite.kiteui.models.FieldSemantic
import com.lightningkite.kiteui.models.FocusSemantic
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.HeaderSemantic
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.models.NavSemantic
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.darken
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.models.lighten
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.systemDefaultFont
import com.lightningkite.kiteui.models.turns


val blackstoneOrange = Color.fromHexString("e15426")
val blackstoneBlue = blackstoneOrange.toHSP().let {
    it.copy(hue = it.hue + 0.5.turns, brightness = 0.5f, saturation = 1f)
}.toRGB()
val blackstoneBlack = Color.gray(0.15f)
val invalidRed = Color.fromHexString("f21e1e")

val blackstoneTheme = Theme(
    id = "blackstoneMat",
    font = FontAndStyle(font = systemDefaultFont, weight = 400),
    elevation = 0.dp,
    cornerRadii = CornerRadii.Constant(10.dp),
    spacing = 1.rem,
    outline = Color.white,
    outlineWidth = 0.dp,
    foreground = Color.white,
    background = blackstoneBlack,
    derivations = mapOf(
        HeaderSemantic to {
            it.copy(
                id = HeaderSemantic.key,
                font = FontAndStyle(font = systemDefaultFont, bold = true)
            ).withoutBack
        },
        BarSemantic to { it[NavSemantic] },
        CardSemantic to {
            it.copy(
                id = CardSemantic.key,
                background = it.background.lighten(0.1f)
            ).withBack
        },
        NavSemantic to {
            it.copy(
                id = NavSemantic.key,
                background = blackstoneOrange,
                foreground = Color.white,
                outline = Color.white,
                spacing = 0.5.rem,
                navSpacing = 0.rem,
                derivations = mapOf(
                    SelectedSemantic to { it.copy(SelectedSemantic.key, background = it.background.darken(0.2f)).withBack },
                    UnselectedSemantic to { it.withBack }
                )
            ).withBack
        },
        ImportantSemantic to {
            it.copy(
                id = ImportantSemantic.key,
                background = blackstoneOrange,
                foreground = Color.white,
                outline = Color.white,
            ).withBack
        },
        CriticalSemantic to {
            it.copy(
                id = CriticalSemantic.key,
                background = blackstoneBlue,
                foreground = Color.white,
                outline = Color.white,
            ).withBack
        },
        MainContentSemantic to { it.withoutBack },
        FocusSemantic to {
            it.copy(
                id = FocusSemantic.key,
                outline = blackstoneBlue,
                outlineWidth = 3.dp
            ).withBack
        },
        SelectedSemantic to {
            it.copy(
                outline = Color.white,
                outlineWidth = 3.dp,
                background = blackstoneOrange,
            ).withBack
        },
        UnselectedSemantic to {
            it.copy(
                outline = Color.white,
                outlineWidth = 3.dp,
                background = Color.transparent,
            ).withBack
        },
        FieldSemantic to {
            it.copy(
                id = FieldSemantic.key,
                outlineWidth = 1.dp,
                cornerRadii = CornerRadii.ForceConstant(10.dp)
            ).withBack
        },
        HoverSemantic to {
            it.copy(
                cornerRadii = CornerRadii.RatioOfSpacing(1f),
                background = it.background.lighten(0.1f)
            ).withBack
        }
    ),
)


data object PrimaryTextSemantic : Semantic {
    override val key: String = "pmry-txt"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "pmry-txt",
        foreground = blackstoneOrange,
    ).withoutBack
}

data object MassiveSemantic: Semantic {
    override val key: String
        get() = "massive"

    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        font = theme.font.copy(size = 4.rem)
    ).withoutBack
}

data object RoundSemantic : Semantic {
    override val key: String = "round"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        cornerRadii = CornerRadii.RatioOfSize()
    ).withoutBack
}
