package com.lightningkite.template

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.VideoRemote
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.bind
import com.lightningkite.kiteui.reactive.equalTo
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactive
import com.lightningkite.kiteui.reactive.shared
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.nowLocal
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@Routable("/dashboard")
class HomeScreen : Screen {
    override val title: Readable<String> get() = Constant("Home")
    override fun ViewWriter.render() {
        scrolls - stack {
            col {
                spacing = 2.rem
                h4("Welcome, Joseph Ivie!")
                col {
                    onlyWhen { showSupport() } - critical - link {
                        row {
                            icon(Icon.chat, "Support")
                            expanding - h6("I'm missing parts")
//                            centered - danger - RoundSemantic.onNext - stack{}
                        }
                        to = { KustomerChatPage() }
                    }
                    onlyWhen { showOrder() } - important - link {
                        row {
                            centered - icon(Icon.shipping, "Order")
                            expanding - col {
                                spacing = 0.25.rem
                                h6("5 Replacement Parts for Omnivore")
                                text("Expected arrival " + nowLocal().plus(3.days).date.renderToString())
                            }
                        }
                    }
                }
                scrollsHorizontally - row {
                    sizeConstraints(width = 15.rem, height = 10.rem) - postRecipeHeader()
                    sizeConstraints(width = 15.rem, height = 10.rem) - postAnnouncementHeader()
                    sizeConstraints(width = 15.rem, height = 10.rem) - postQuestionHeader()
                    sizeConstraints(width = 15.rem, height = 10.rem) - postBragHeader()
                }
                card - col {

//                        spacing = 0.25.rem
                    h4("My Products")
                    stack {
                        scrollsHorizontally - row {
                            sizeConstraints(
                                width = 15.rem,
                                height = 10.rem
                            ) - myProductHeader(
                                "https://blackstoneproducts.com/cdn/shop/files/2266_36OriginalOmniGriddle_WalmartSequence_01.jpg?v=1730916854",
                                "36\" Omnivore Griddle"
                            )
                            sizeConstraints(
                                width = 15.rem,
                                height = 10.rem
                            ) - myProductHeader(
                                "https://blackstoneproducts.com/cdn/shop/files/1_2404_LeggeroProPizzaOven_OnWhite_Sequence_WEB.jpg?v=1736355196",
                                "Leggero Pro Pizza Oven"
                            )
                            sizeConstraints(
                                width = 15.rem,
                                height = 10.rem
                            ) - myProductHeader(
                                "https://blackstoneproducts.com/cdn/shop/files/2022_PelletGridllGriddleCombo_Sequences_Lowes_01.jpg?v=1727195502",
                                "Culinary 22\" XL Griddle Pellet Grill Combo"
                            )
                            sizeConstraints(
                                width = 15.rem,
                                height = 10.rem
                            ) - myProductHeader(
                                "https://blackstoneproducts.com/cdn/shop/files/1_OTG22inOmnivoreTabletopGriddle_OnWhite_Sequence_WEB.jpg?v=1733782213",
                                "On The Go 22” Tabletop Griddle with Hood"
                            )
                            space(2.0)
                        }
                        gravity(Align.End, Align.Center) - row {
                            spacing = 0.px
                            RoundSemantic.onNext - important - link {
                                icon(Icon.add, "Register Product")
                                to = { RegisterProductScanPage() }
                            }
                            space(0.5)
                        }
                    }

                }
//                card - col {
////                        spacing = 0.25.rem
//                    h4("My Posts")
//                    stack {
//                        scrollsHorizontally - row {
//                            sizeConstraints(width = 15.rem, height = 10.rem) - postQuestionHeader()
//                            sizeConstraints(width = 15.rem, height = 10.rem) - postBragHeader()
//                        }
//                    }
//                }
                card - col {

//                        spacing = 0.25.rem
                    h4("You might like these")
                    scrollsHorizontally - row {
                        sizeConstraints(
                            width = 15.rem,
                            height = 10.rem
                        ) - myProductHeader(
                            "https://blackstoneproducts.com/cdn/shop/products/5718.jpg?v=1652366830",
                            "Griddle Seasoning and Grease Cup Liner Bundle"
                        )
                        sizeConstraints(
                            width = 15.rem,
                            height = 10.rem
                        ) - myProductHeader(
                            "https://blackstoneproducts.com/cdn/shop/products/5462_01.jpg?v=1681313224",
                            "Smash Burger Kit"
                        )
                        sizeConstraints(
                            width = 15.rem,
                            height = 10.rem
                        ) - myProductHeader(
                            "https://blackstoneproducts.com/cdn/shop/products/taco-racks-647482.jpg?v=1676438529",
                            "Taco Racks"
                        )
                        space(2.0)
                    }
                }
                space()
            }
        }
    }
}

@Routable("register/scan")
class RegisterProductScanPage : Page {
    override val title: Readable<String> get() = Constant("Register")
    override fun ViewWriter.render2(): ViewModifiable = unpadded - stack {
        image {
            source = Resources.scan
            scaleType = ImageScaleType.Crop
        }
        col {
            spacing = 0.px
            weight(2f) - OverImageSemantic.onNext - stack {}
            weight(1f) - row {
                spacing = 0.px
                weight(0.1f) - OverImageSemantic.onNext - stack {}
                weight(10f) - stack {}
                weight(0.1f) - OverImageSemantic.onNext - stack {}

            }
            weight(2f) - OverImageSemantic.onNext - stack {}
        }
        padded - stack {
            atTopCenter - h3 {
                content = "Scan Your Serial Number"
                align = Align.Center
            }
            atBottomCenter - col {
                text {
                    content =
                        "Put your serial number in the center of the image. When it is scanned, we'll move to the next step."
                    align = Align.Center
                }
                card - link {
                    centered - text("Enter Manually")
                    to = { FinishRegistrationPage() }
                }
            }
        }
    }
}

@Routable("my-products/example")
class MyProductExamplePage : Page {
    override val title: Readable<String> get() = Constant("Griddle")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        centered - sizeConstraints(width = 15.rem, height = 10.rem) - image {
            source =
                ImageRemote("https://blackstoneproducts.com/cdn/shop/files/2266_36OriginalOmniGriddle_WalmartSequence_01.jpg?v=1730916854")
        }
        col {
            spacing = 0.25.rem
            h1 {
                align = Align.Center
                content = "36\" Omnivore Griddle"
            }
            centered - text("Serial Number: 12345678987")
        }

        onlyWhen { warrantyRegistered() } - row {
            expanding - card - link {
                to = { WarrantyInfoPage() }
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
            centered - important - link {
                to = { WarrantyUpgradePage() }
                text("Upgrade")
            }
        }

        onlyWhen { !warrantyRegistered() } - row {
            expanding - card - row {
                centered - warning - RoundSemantic.onNext - stack {
                    spacing = 0.25.rem
                    icon(Icon.warning, "Incomplete")
                }
                centered - text("Warranty Needs Proof of Purchase")
            }
            centered - important - link {
                text("Upload")
                to = { FinishRegistrationPage() }
            }
        }

        important - link {
            row {
                icon(Icon.sync, "")
                centered - text("Connect")
            }
            to = { AssemblyPage() }
        }
        important - link {
            row {
                icon(Icon.list, "")
                centered - text("Assembly Guide")
            }
            to = { AssemblyPage() }
        }
        important - link {
            row {
                icon(Icon.shipping, "")
                centered - text("Request Replacement Parts")
            }
            to = { OrderPartsExamplePage() }
        }
        important - externalLink {
            to = "https://cdn.shopify.com/s/files/1/0312/7695/7740/files/2411_2411CA_Manual_V3_English.pdf?v=1735245363"
            row {
                icon(Icon.info, "")
                centered - text("Manual")
            }
        }
        important - link {
            to = { HelpPage() }
            row {
                icon(Icon.info, "")
                centered - text("Help and Support")
            }
        }
    }
}

@Routable("warranty-info")
class WarrantyInfoPage : Page {
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        h1("Standard Warranty")
        text(
            """
            Blackstone Products (cooking stations and accessories) are backed by a one-year manufacturer's warranty.  Your warranty is active and registered! 

            We pride ourselves in providing high-quality and durable products that will last for years to come. However, in the case that you need it, your warranty covers any manufacturer defects. If you need to submit a warranty claim, please make sure to register your griddle and contact us by submitting a request here.
            
            Your manufacturer warranty is designed to ensure your product is free from any damage or dysfunction caused by a material defect. However, it is important to note that this doesn’t apply to normal wear-and-tear, damage caused by modification, or non-qualified griddle purchases.

            Any products not purchased from one of our retailers, purchased at a discount due to damage or missing parts, purchased as a floor model, not used or maintained correctly, or modified after purchase are not eligible for warranty.
        """.trimIndent()
        )
    }
}

@Routable("warranty-upgrade")
class WarrantyUpgradePage : Page {
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        h2("Upgrade your Warranty")
        h4("Extended Service Plan")
        text("Includes this, that, and this.")
        row {
            expanding - card - link { centered - text("Details") }
            expanding - important - link { centered - text("Purchase") }
        }
    }
}

@Routable("finish-registration")
class FinishRegistrationPage : Page {
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        field("Purchase Date") { localDateField() }
        field("Retailer") { textInput() }
        val image = Property<FileReference?>(null)
        important - button {
            onlyWhen { image() != null } - image {
                ::source { image()?.let(::ImageLocal) }
            }
            centered - onlyWhen { image() == null } - text("Upload Receipt (optional)")
            centered - onlyWhen { image() != null } - OverImageSemantic.onNext - text("Change Receipt")

            onClick {
                image.value = ExternalServices.requestFile(listOf("image/*"))
            }
        }
        important - link {
            centered - text("Submit")
            onNavigate { warrantyRegistered.value = true }
            to = { MyProductExamplePage() }
        }
        centered - text("or")
        card - link {
            centered - text("Skip, activate warranty later")
            to = { MyProductExamplePage() }
        }
        card - link {
            centered - text("Purchased Second-hand")
            to = { MyProductExamplePage() }
        }
    }
}

@Routable("help")
class HelpPage : Page {
    override val title: Readable<String> get() = Constant("Help")
    override fun ViewWriter.render2(): ViewModifiable = col {
        centered - h1("How can we help you?")
        subtext("(we should probably make this page categorized)")
        scrolls - col {
            card - link {
                to = { HelpFormExamplePage() }
                centered - text("My griddle is missing parts")
            }
            card - link {
                to = { HelpFormExamplePage() }
                centered - text("My griddle is damaged")
            }
            card - link {
                to = { HelpArticleExamplePage() }
                centered - text("My griddle isn't getting hot enough")
            }
            card - link {
                to = { HelpArticleExamplePage() }
                centered - text("My griddle won't light")
            }
            card - link {
                to = { HelpArticleExamplePage() }
                centered - text("My griddle top is peeling")
            }
            card - link {
                to = { HelpArticleExamplePage() }
                centered - text("My griddle top is rusty")
            }
            card - link {
                to = { KustomerChatPage() }
                centered - row {
                    text("Something else")
                    icon(Icon.chat, "Chat Now")
                }
            }
        }
    }
}

@Routable("help/article")
class HelpArticleExamplePage : Page {
    override val title: Readable<String> get() = Constant("Article")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        centered - h1("My griddle top is peeling")
        text(
            """
            Sometimes the seasoning on your griddle top can peel a little. This is usually caused by the seasoning drying up and peeling off, or by using too much water when cleaning your griddle. To be able to fix this, you simply need to scrape off the old seasoning and re-season your griddle top.

            To remove the old seasoning, use a metal-edge scraper to scrape off the large bits, and then use oil and griddle stone, steel wool, or even sandpaper to scrub off the old seasoning (similar to our rust removal process). Then scrape off the oil and wipe down the griddle with clean paper towels, finishing with a little water with the paper towels before you re-season the surface.

            Here’s a helpful video on how to restore a damaged griddle top—whether that damage be rust or peeling seasoning. It also goes over basic maintenance to help protect your seasoning in the future.
        """.trimIndent()
        )

        centered - sizeConstraints(width = 25.rem, height = 15.rem) - webView {
            permitJs = true
            url = "https://www.youtube-nocookie.com/embed/1UUWzPDDX6E"
        }
    }
}

@Routable("help/form")
class HelpFormExamplePage : Page {
    override val title: Readable<String> get() = Constant("Form")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        centered - h1("My griddle is damaged")
        if (warrantyRegistered.value) {
            text(
                """
            If your griddle is damaged and covered under the warranty, our Customer Support team can offer replacement or compensation options. For the fastest assistance, please finish registering your griddle to activate your warranty, as we can’t send anything out until that is completed.
            
            If your griddle isn't within warranty, you can order grill parts below.
        """.trimIndent()
            )
            important - link {
                centered - text("Finish Registration")
            }
            important - link {
                centered - text("Order Parts")
                to = { OrderPartsExamplePage() }
            }
        } else {
            text("Your warranty is active and may cover your particular issue.  Please fill out the form below.")
            space()
            field("What is damaged?") {
                textArea()
            }
            val image = Property<FileReference?>(null)
            important - button {
                onlyWhen { image() != null } - image {
                    ::source { image()?.let(::ImageLocal) }
                }
                centered - onlyWhen { image() == null } - text("Upload Picture")
                centered - onlyWhen { image() != null } - OverImageSemantic.onNext - text("Change Picture")

                onClick {
                    image.value = ExternalServices.requestFile(listOf("image/*"))
                }
            }
            onlyWhen { image() != null }
            space()
            important - link {
                centered - text("Submit And Chat")
                to = { KustomerChatPage() }
            }
        }
    }
}

@Routable("kustomer-chat")
class KustomerChatPage : Page {
    override val title: Readable<String> get() = Constant("Chat")
    override fun ViewWriter.render2(): ViewModifiable = col {
        showSupport.value = true
        centered - h4("Chat Topic")
        expanding - scrolls - col {
            atEnd - important - stack {
                col {
                    spacing = 0.25.rem
                    text("I have a question")
                    subtext(nowLocal().dateTime.renderToString())
                }
            }
            atStart - card - stack {
                col {
                    spacing = 0.25.rem
                    text("I have an answer, and here it is.")
                    subtext(nowLocal().dateTime.renderToString())
                }
            }
            atEnd - important - stack {
                col {
                    spacing = 0.25.rem
                    text("I have a question")
                    subtext(nowLocal().dateTime.renderToString())
                }
            }
            atStart - card - stack {
                col {
                    spacing = 0.25.rem
                    text("I have an answer, and here it is.")
                    subtext(nowLocal().dateTime.renderToString())
                }
            }
            atEnd - important - stack {
                col {
                    spacing = 0.25.rem
                    text("I have a question")
                    subtext(nowLocal().dateTime.renderToString())
                }
            }
            atStart - card - stack {
                col {
                    spacing = 0.25.rem
                    text("I have an answer, and here it is.")
                    subtext(nowLocal().dateTime.renderToString())
                }
            }
        }
        fieldTheme - row {
            spacing = 0.5.rem
            expanding - textArea {
                hint = "Send a message..."
            }
            button {
                icon(Icon.send, "Send")
            }
        }
    }
}

@Routable("order-parts/example")
class OrderPartsExamplePage : Page {
    override val title: Readable<String> get() = Constant("Parts")
    override fun ViewWriter.render2(): ViewModifiable = col {
        expanding - WhiteBackSemantic.onNext - image {
            source = Resources.exploded
            scaleType = ImageScaleType.Fit
        }
        expanding - recyclerView {
            children(shared { (1..100).toList() }) {
                card - row {
                    text { ::content { it().toString() } }
                    expanding - text("Specific Part Name")
                    val count = Property(0)
                    compact - important - button {
                        centered - text("-"); action =
                        Action("X", Icon.done, frequencyCap = 20.milliseconds) { count.value-- }
                    }
                    sizeConstraints(5.rem) - fieldTheme - text { ::content { count().toString() } }
                    compact - important - button {
                        centered - text("+"); action =
                        Action("X", Icon.done, frequencyCap = 20.milliseconds) { count.value++ }
                    }
                }
            }
        }
        important - link {
            centered - text("Order")
            to = { OrderPartsExampleFinishPage() }
        }
    }
}

@Routable("posts")
class PostsPage : Page {
    override val title: Readable<String> get() = Constant("Posts")
    override fun ViewWriter.render2(): ViewModifiable = stack {
        scrolls - col {
            postRecipeHeaderFullWidth()
            postAnnouncementHeaderFullWidth()
            postQuestionHeaderFullWidth()
            postBragHeaderFullWidth()
        }
        atBottomEnd - RoundSemantic.onNext - important - link {
            icon(Icon.add, "Register Product")
            to = { RegisterProductScanPage() }
        }
    }
}

@Routable("my-stuff")
class MyStuffPage : Page {
    override val title: Readable<String> get() = Constant("My Stuff")
    override fun ViewWriter.render2(): ViewModifiable = stack {
        scrolls - col {
            myProductHeaderFullWidth(
                "https://blackstoneproducts.com/cdn/shop/files/2266_36OriginalOmniGriddle_WalmartSequence_01.jpg?v=1730916854",
                "36\" Omnivore Griddle"
            )
            myProductHeaderFullWidth(
                "https://blackstoneproducts.com/cdn/shop/files/1_2404_LeggeroProPizzaOven_OnWhite_Sequence_WEB.jpg?v=1736355196",
                "Leggero Pro Pizza Oven"
            )
            myProductHeaderFullWidth(
                "https://blackstoneproducts.com/cdn/shop/files/2022_PelletGridllGriddleCombo_Sequences_Lowes_01.jpg?v=1727195502",
                "Culinary 22\" XL Griddle Pellet Grill Combo"
            )
            myProductHeaderFullWidth(
                "https://blackstoneproducts.com/cdn/shop/files/1_OTG22inOmnivoreTabletopGriddle_OnWhite_Sequence_WEB.jpg?v=1733782213",
                "On The Go 22” Tabletop Griddle with Hood"
            )
        }
        atBottomEnd - RoundSemantic.onNext - important - link {
            icon(Icon.add, "Register Product")
            to = { RegisterProductScanPage() }
        }
    }
}

@Routable("profile")
class ProfilePage : Page {
    override val title: Readable<String> get() = Constant("Profile")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        centered - sizeConstraints(width = 5.rem, height = 5.rem) - card - RoundSemantic.onNext - image {
            source = Icon.person.toImageSource(Color.gray)
        }
        centered - h1("Joseph Ivie")
        card - col {
            h4("Shipping Address")
            field("Line 1") { textInput { content.value = "255 S 300 W" } }
            field("Line 2") { textInput { content.value = "Downstairs" } }
            rowCollapsingToColumn(50.rem) {
                expanding - field("City") { textInput { content.value = "Logan" } }
                expanding - field("State") {
                    select {
                        bind(
                            Property(UsState.UT),
                            UsState.entries.toList().let(::Constant)
                        ) { it.text }
                    }
                }
                expanding - field("ZIP") { textInput { content.value = "84321" } }
            }
            atEnd - important - button {
                centered - text("Save")
            }
        }
    }
}

@Routable("order-parts/example/finish")
class OrderPartsExampleFinishPage : Page {
    override val title: Readable<String> get() = Constant("Order")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        h1("Order")
        card - row {
            expanding - col {
                spacing = 0.25.rem
                h5("Sending to your address:")
                text("255 S 300 W, Logan, UT 84321")
            }
            centered - important - compact - link {
                centered - text("Change")
                to = { ProfilePage() }
            }
        }
        val underWarranty = Property(false)
        card - col {
            row {
                centered - expanding - text("This order should be free under warranty")
                centered - switch {
                    checked bind underWarranty
                }
            }
            onlyWhen { underWarranty() } - col {
                field("What's wrong?") { textArea() }
                val image = Property<FileReference?>(null)
                important - button {
                    onlyWhen { image() != null } - image {
                        ::source { image()?.let(::ImageLocal) }
                    }
                    centered - onlyWhen { image() == null } - text("Upload Image")
                    centered - onlyWhen { image() != null } - OverImageSemantic.onNext - text("Change Image")

                    onClick {
                        image.value = ExternalServices.requestFile(listOf("image/*"))
                    }
                }
            }
        }
        important - button {
            centered - text {
                ::content { if (underWarranty()) "Request" else "Purchase" }
            }
        }
    }
}


@Routable("welcome")
class InitialWelcome : Page, UseFullScreen {
    override val title: Readable<String> get() = Constant("Welcome")
    override fun ViewWriter.render2(): ViewModifiable = col {
        expanding - space()
        centered - text("Thank you for choosing")
        centered - sizeConstraints(width = 25.rem, height = 8.rem) - image {
            source = Resources.logo
            description = "Blackstone"
        }
        important - link {
            centered - text("Let's get started!")
            to = { WelcomeEmailPage() }
        }
        card - link {
            centered - text("I don't want to make an account")
            to = { AssemblyPage() }
        }
        expanding - space()
    }
}

@Routable("welcome/assembly")
class AssemblyPage : Page, UseFullScreen {
    override val title: Readable<String> get() = Constant("Assembly")
    val step = Property(0)
    override fun ViewWriter.render2(): ViewModifiable = col {
        expanding - viewPager {
            index bind step
            children((0..19).toList().let(::Constant)) {
                col {
                    sizeConstraints(aspectRatio = 16 to 9) - video {
                        source =
                            VideoRemote("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4")
                        scaleType = ImageScaleType.Fit
                        loop = true
                        showControls = true

                        val shouldPlay = shared { it() == index() }
                        reactive {
                            val play = shouldPlay()
                            launch {
                                volume set 0f
                                time set 0.0
                                playing set play
                            }
                        }
                    }
                    h4("Assemble This Part")
                    text("Some text instruction here, maybe some images.")
                }
            }
        }
        row {
            onlyWhen { step() > 0 } - important - RoundSemantic.onNext - button {
                icon(Icon.chevronLeft, "Back")
                onClick { step.value-- }
            }
            expanding - centered - text {
                align = Align.Center
                ::content { "Step ${step() + 1} / 20" }
            }
            onlyWhen { step() < 19 } - important - RoundSemantic.onNext - button {
                icon(Icon.chevronRight, "Next")
                onClick { step.value++ }
            }
            RoundSemantic.onNext - button {
                dynamicTheme { if (step() == 19) ImportantSemantic else CardSemantic }
                icon(Icon.done, "Done")
                onClick {
                    mainScreenNavigator.navigate(HomeScreen())
                }
            }
        }
    }
}

@Routable("welcome/email")
class WelcomeEmailPage : Page, UseFullScreen {
    override val title: Readable<String> get() = Constant("Welcome")
    val readyForVerify = Property(false)
    override fun ViewWriter.render2(): ViewModifiable = col {
        expanding - space()
        centered - sizeConstraints(width = 25.rem, height = 8.rem) - image {
            source = Resources.logo
            description = "Blackstone"
        }
        h4("What is your email address?")
        field("Email") { textInput() }
        onlyWhen { !readyForVerify() } - important - button {
            centered - text("Send Me a Verification Code")
            onClick { readyForVerify.value = true }
        }
        onlyWhen { readyForVerify() } - col {
            field("Code") {
                textInput()
            }
            important - link {
                centered - text("Verify")
                to = { WelcomeNamePhone() }
            }
        }
        expanding - space()
    }
}

@Routable("welcome/email")
class WelcomeNamePhone : Page, UseFullScreen {
    override val title: Readable<String> get() = Constant("Welcome")
    override fun ViewWriter.render2(): ViewModifiable = col {
        expanding - space()
        centered - sizeConstraints(width = 25.rem, height = 8.rem) - image {
            source = Resources.logo
            description = "Blackstone"
        }
        h4("What is your name and phone number?")
        field("Name") { textInput() }
        field("Phone Number") { textInput() }
        row {
            expanding - card - link {
                centered - text("Skip")
                to = { WelcomeAddress() }
            }
            expanding - important - link {
                centered - text("Submit")
                to = { WelcomeAddress() }
            }
        }
        expanding - space()
    }
}

@Routable("welcome/address")
class WelcomeAddress : Page, UseFullScreen {
    override val title: Readable<String> get() = Constant("Welcome")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        expanding - space()
        centered - sizeConstraints(width = 25.rem, height = 8.rem) - image {
            source = Resources.logo
            description = "Blackstone"
        }
        h4("Where should we ship your items to?")
        field("Line 1") { textInput { content.value = "" } }
        field("Line 2") { textInput { content.value = "" } }
        rowCollapsingToColumn(50.rem) {
            expanding - field("City") { textInput { content.value = "" } }
            expanding - field("State") {
                select {
                    bind(
                        Property(UsState.UT),
                        UsState.entries.toList().let(::Constant)
                    ) { it.text }
                }
            }
            expanding - field("ZIP") { textInput { content.value = "" } }
        }
        row {
            expanding - card - link {
                centered - text("Skip")
                to = { WelcomeReceipt() }
            }
            expanding - important - link {
                centered - text("Submit")
                to = { WelcomeReceipt() }
            }
        }
        expanding - space()
    }
}

@Routable("welcome/receipt")
class WelcomeReceipt : Page, UseFullScreen {
    override val title: Readable<String> get() = Constant("Welcome")
    override fun ViewWriter.render2(): ViewModifiable = scrolls - col {
        expanding - space()
        centered - sizeConstraints(width = 25.rem, height = 8.rem) - image {
            source = Resources.logo
            description = "Blackstone"
        }
        h4("How did you buy your product?")
        field("Purchase Date") { localDateField() }
        field("Retailer") { textInput() }
        val image = Property<FileReference?>(null)
        important - button {
            onlyWhen { image() != null } - image {
                ::source { image()?.let(::ImageLocal) }
            }
            centered - onlyWhen { image() == null } - text("Upload Receipt (optional)")
            centered - onlyWhen { image() != null } - OverImageSemantic.onNext - text("Change Receipt")

            onClick {
                image.value = ExternalServices.requestFile(listOf("image/*"))
            }
        }
        row {
            expanding - card - link {
                centered - text("Skip")
                to = { AssemblyPage() }
            }
            expanding - important - link {
                centered - text("Submit")
                to = { AssemblyPage() }
            }
        }
        expanding - space()
    }
}


@Routable("recipes/item")
class RecipePage : Page {
    override val title: Readable<String> get() = Constant("Happy Camper Cornbread")

    enum class Section { Recipe, Post, Responses }

    val section = Property(Section.Recipe)
    override fun ViewWriter.render2(): ViewModifiable = col {
        row {
            expanding - radioToggleButton { centered - text("Recipe"); checked bind section.equalTo(Section.Recipe) }
            expanding - radioToggleButton { centered - text("Post"); checked bind section.equalTo(Section.Post) }
            expanding - radioToggleButton { centered - text("Responses"); checked bind section.equalTo(Section.Responses) }
        }
        expanding - swapView {
            swapping(current = { section() }) {
                when (it) {
                    Section.Recipe -> stack {
                        scrolls - col {
                            centered - sizeConstraints(width = 20.rem, height = 20.rem) - image {
                                source =
                                    ImageRemote("https://blackstoneproducts.com/cdn/shop/articles/20250211220932-roman-20dough.jpg?v=1739312647")
                            }
                            h4("Ingredients")
                            text(
                                """
                            1 box Jiffy Cornbread Mix
                            ⅓ cup whole milk
                            2 large eggs
                            6-7 slices of breakfast ham, diced
                            1 cup shredded cheddar cheese
                            ½ stick salted butter 
                        """.trimIndent()
                            )
                            space()
                            h4("Directions")
                            text(
                                """    
                            1. Heat your portable pizza oven to 500°F, leaving the pan inside to preheat.
                            2. In a mixing bowl, combine the Jiffy Cornbread Mix, milk, and eggs. Stir until smooth.
                            3. Dice the breakfast ham and fold it into the batter. Then, mix in the shredded cheddar cheese.
                            4. Once the oven reaches 500°F, remove the preheated pan carefully. Add the salted butter to the pan and swirl it around to coat the bottom evenly.
                            5. Pour the prepared batter into the buttered pan, spreading it evenly.
                            6. Carefully return the pan to the pizza oven and bake for 8-9 minutes, or until the cornbread is golden brown and set in the center.
                            7. Remove the pan carefully from the oven, let it cool slightly, and enjoy your delicious cornbread! 
                        """.trimIndent()
                            )
                        }
                    }

                    Section.Post -> stack {
                        scrolls - col {
                            h4("See How Easy It Is To Make This Cornbread Recipe!")
                            text(
                                """
                            My family has been making this easy cornbread recipe for decades, and thousands of people on the internet have, too. This recipe has over 1000 comments and 600 glowing reviews and is officially THE favorite cornbread recipe!
                        """.trimIndent()
                            )
                            h4("What Makes This THE Best Cornbread")
                            text(
                                """
                            One of the reasons I love this cornbread recipe is that you don’t need anything fancy to make it. Many cornbread recipes require buttermilk or creamed corn or a very specific kind of pan.

                            - Well for starters, it tastes DIVINE.
                            - It’s sweet but not over the top.
                            - It’s soft but not cakey.
                            - And crumbly without being a plain mess.

                            I serve this cornbread alongside all my soups, favorite chilis, and along with my favorite barbecue. 
                        """.trimIndent()
                            )
                            h4("Practical Substitutions")
                            text(
                                """
                            There’s a LOT of room to play with this recipe so don’t feel that you have to make it exactly as it’s written.

                            - Use melted butter OR oil—canola, avocado, vegetable oil, or even coconut oil.
                            - Use fine OR medium ground cornmeal, and you can use white OR yellow cornmeal.
                            - Use all-purpose flour OR a white whole wheat flour.
                            - Don’t like a sweet cornbread? Feel free to reduce the sugar to as little as 2 tablespoons.
                            - Use cow’s milk—whole milk is perfect—OR alternative milk, like almond or soy.
                            - Use an egg OR a flax egg.

                        """.trimIndent()
                            )
                        }
                    }

                    Section.Responses -> stack {
                        scrolls - col {
                            fun make() {
                                card - row {
                                    sizeConstraints(width = 10.rem, height = 10.rem) - image {
                                        scaleType = ImageScaleType.Crop
                                        source =
                                            ImageRemote("https://blackstoneproducts.com/cdn/shop/articles/20250211220932-roman-20dough.jpg?v=1739312647")
                                    }
                                    expanding - col {
                                        spacing = 0.25.rem
                                        h4("Tasty!")
                                        subtext("someuser")
                                        row {
                                            spacing = 0.25.rem
                                            icon(Icon.starFilled, "1")
                                            icon(Icon.starFilled, "1")
                                            icon(Icon.starFilled, "1")
                                            icon(Icon.starFilled, "1")
                                            icon(Icon.star, "1")
                                        }
                                        expanding - text(
                                            """
                                        It was pretty good!
                                    """.trimIndent()
                                        )
                                    }
                                }
                            }
                            make()
                            make()
                            make()
                            make()
                            make()
                            make()
                            make()
                        }
                    }
                }
            }
        }
    }
}


@Routable("recipes")
class RecipesPage : Page {
    override fun ViewWriter.render2(): ViewModifiable = stack {
        col {
            fieldTheme - row {
                expanding - textInput()
                unpadded - menuButton {
                    icon(Icon.filterList, "Filters")
                    opensMenu {
                        sizeConstraints(width = 50.rem) - col {
                            row {
                                expanding - toggleButton { checked.value = true; centered - text("Chicken") }
                                expanding - toggleButton { checked.value = true; centered - text("Beef") }
                                expanding - toggleButton { checked.value = true; centered - text("Pork") }
                                expanding - toggleButton { checked.value = true; centered - text("Seafood") }
                                expanding - toggleButton { checked.value = true; centered - text("Other") }
                            }
                            row {
                                val all = Property(true)
                                expanding - radioToggleButton() { text("All Recipes"); checked bind all.equalTo(true) }
                                expanding - radioToggleButton() { text("My Recipes"); checked bind all.equalTo(false) }
                            }
                        }
                    }
                }
            }
            expanding - scrolls - col {
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
                postRecipeHeaderFullWidth()
            }
        }
        atBottomEnd - RoundSemantic.onNext - important - link {
            icon(Icon.add, "Register Product")
        }
    }
}

val showRecipes = Property(false)
val showSupport = Property(false)
val showOrder = Property(true)
val warrantyRegistered = Property(false)

val Icon.Companion.shipping
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        pathDatas = listOf("M240-160q-50 0-85-35t-35-85H40v-440q0-33 23.5-56.5T120-800h560v160h120l120 160v200h-80q0 50-35 85t-85 35q-50 0-85-35t-35-85H360q0 50-35 85t-85 35Zm0-80q17 0 28.5-11.5T280-280q0-17-11.5-28.5T240-320q-17 0-28.5 11.5T200-280q0 17 11.5 28.5T240-240ZM120-360h32q17-18 39-29t49-11q27 0 49 11t39 29h272v-360H120v360Zm600 120q17 0 28.5-11.5T760-280q0-17-11.5-28.5T720-320q-17 0-28.5 11.5T680-280q0 17 11.5 28.5T720-240Zm-40-200h170l-90-120h-80v120ZM360-540Z")
    )
val Icon.Companion.smartphone
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        pathDatas = listOf("M280-40q-33 0-56.5-23.5T200-120v-720q0-33 23.5-56.5T280-920h400q33 0 56.5 23.5T760-840v720q0 33-23.5 56.5T680-40H280Zm0-120v40h400v-40H280Zm0-80h400v-480H280v480Zm0-560h400v-40H280v40Zm0 0v-40 40Zm0 640v40-40Z")
    )