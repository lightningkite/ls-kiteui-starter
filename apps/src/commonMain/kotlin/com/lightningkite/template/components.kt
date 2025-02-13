package com.lightningkite.template

import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.nowLocal
import kotlin.time.Duration.Companion.days

fun ViewWriter.postRecipeHeader() = card - unpadded - link {
    image {
        scaleType = ImageScaleType.Crop
        source = ImageRemote("https://blackstoneproducts.com/cdn/shop/articles/20250211220932-roman-20dough.jpg?v=1739312647")
    }
    atTopCenter - padded - row {
        spacing = 0.25.rem
        icon(Icon.starFilled, "1")
        icon(Icon.starFilled, "1")
        icon(Icon.starFilled, "1")
        icon(Icon.starFilled, "1")
        icon(Icon.star, "1")
    }
    atBottom - OverImageSemantic.onNext - stack {
        text {
            content = "Happy Camper Cornbread"
            wraps = false
        }
    }
}

fun ViewWriter.postAnnouncementHeader() = card - col {
    h4("Smoker App Update Available")
    expanding - text {
        content = "A new update to the smoker app is now available! "
        ellipsis = true
    }
}

fun ViewWriter.postQuestionHeader() = card - col {
    h4("What are your favorite cooking tools?")
    expanding - text {
        content = "Hello!  What is the best way to clean griddles?  My griddle's got a bunch of stuff stuck on it..."
        ellipsis = true
    }
}
fun ViewWriter.postBragHeader() = card - unpadded - link {
    image {
        scaleType = ImageScaleType.Crop
        source = ImageRemote("https://blackstoneproducts.com/cdn/shop/articles/20250131204943-photo-20jan-2008-202025-2c-203-2000-2044-20pm.jpg?v=1738380505")
    }
    atBottom - OverImageSemantic.onNext - stack {
        text {
            content = "Check out my cookies!"
            wraps = false
        }
    }
}

fun ViewWriter.postRecipeHeaderFullWidth() = card - link {
    row {
        sizeConstraints(width = 7.rem, height = 7.rem) - image {
            scaleType = ImageScaleType.Crop
            source = ImageRemote("https://blackstoneproducts.com/cdn/shop/articles/20250211220932-roman-20dough.jpg?v=1739312647")
        }
        expanding - col {
            spacing = 0.25.rem
            h4("Happy Camper Cornbread")
            text("A cornbread breakfast for your pizza oven.")
            padded - row {
                spacing = 0.25.rem
                icon(Icon.starFilled, "1")
                icon(Icon.starFilled, "1")
                icon(Icon.starFilled, "1")
                icon(Icon.starFilled, "1")
                icon(Icon.star, "1")
            }
        }
    }
    to = { RecipePage() }
}

fun ViewWriter.postAnnouncementHeaderFullWidth() = card - link {
    row {
        sizeConstraints(width = 7.rem, height = 7.rem) - image {
            scaleType = ImageScaleType.Crop
            source = Icon.smartphone.toImageSource(Color.white)
        }
        expanding - col {
            spacing = 0.25.rem
            h4("Smoker App Update Available")
            expanding - text {
                ellipsis = true
                content = "A new update to the smoker app is now available! \nThis update contains connectivity improvements and a new named timer feature."
            }
        }
    }
    to = { MyProductExamplePage() }
}

fun ViewWriter.postQuestionHeaderFullWidth() = card - link {
    row {
        sizeConstraints(width = 7.rem, height = 7.rem) - image {
            scaleType = ImageScaleType.Crop
            source = Icon.info.toImageSource(Color.white)
        }
        expanding - col {
            spacing = 0.25.rem
            h4("What are your favorite cooking tools?")
            expanding - text {
                ellipsis = true
                content = """
                    I've had a Blackstone for a few months and am loving it. I bought a cheap set of tools off of Amazon but am wanting to upgrade.

                    What are your favorite cooking and cleaning tools for your Blackstone? Looking for top-tier stuff.

                    Thanks!
                """.trimIndent()
            }
        }
    }
    to = { MyProductExamplePage() }
}
fun ViewWriter.postBragHeaderFullWidth() = card - link {
    row {
        sizeConstraints(width = 7.rem, height = 7.rem) - image {
            scaleType = ImageScaleType.Crop
            source = ImageRemote("https://i.redd.it/0yn7jsdmsoie1.jpeg")
        }
        expanding - col {
            spacing = 0.25.rem
            h4("Late night cook")
            subtext("BaseballandSneakers")
            expanding - text {
                ellipsis = true
                content = """
                    Needed some late night burgers and couldn't resist.
                """.trimIndent()
            }
        }
    }
    to = { MyProductExamplePage() }
}
//    card - unpadded - link {
//    image {
//        scaleType = ImageScaleType.Crop
//        source = ImageRemote("https://blackstoneproducts.com/cdn/shop/articles/20250131204943-photo-20jan-2008-202025-2c-203-2000-2044-20pm.jpg?v=1738380505")
//    }
//    atBottom - OverImageSemantic.onNext - stack {
//        text {
//            content = "Check out my cookies!"
//            wraps = false
//        }
//    }
//}

fun ViewWriter.myProductHeader(image: String, name: String) = card - unpadded - link {
    WhiteBackSemantic.onNext - unpadded - col {
        expanding - image {
            scaleType = ImageScaleType.Fit
            source = ImageRemote(image)
        }
        space()
    }
    atBottom - OverImageSemantic.onNext - stack {
        text {
            content = name
            wraps = false
            ellipsis = true
        }
    }
    to = { MyProductExamplePage() }
}

fun ViewWriter.myProductHeaderFullWidth(image: String, name: String) = card - link {
    row {
        sizeConstraints(width = 10.rem, height = 10.rem) - image {
            scaleType = ImageScaleType.Fit
            source = ImageRemote(image)
        }
        expanding - col {
            h6(name)
            text("Serial 17283491")
            row {
                centered - affirmative - RoundSemantic.onNext - stack {
                    spacing = 0.25.rem
                    icon(Icon.done, "OK")
                }
                col {
                    spacing = 0.25.rem
                    text("Standard Warranty")
                    subtext("Until " + nowLocal().plus(3.days).date.renderToString())
                }
            }
        }
    }
    to = { MyProductExamplePage() }
}

object OverImageSemantic: Semantic {
    override val key: String = "overimage"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(id=key, background = theme.background.applyAlpha(0.75f)).withBack
}
object WhiteBackSemantic: Semantic {
    override val key: String = "whiteback"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(id=key, background = Color.white, foreground = Color.black).withBack
}