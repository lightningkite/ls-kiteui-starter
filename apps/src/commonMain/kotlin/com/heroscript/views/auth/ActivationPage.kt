package com.heroscript.views.auth

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.toast
import com.heroscript.FullscreenPage
import com.heroscript.views.DashboardPage
import com.heroscript.views.LoginPage
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal

/**
 * Lands users arriving from a ClinicMembership invite email. The token identifies a pending
 * membership; consuming it should create the user (if new), set the password, optionally
 * enroll MFA, and log the user in. The server-side "consume invite token" endpoint does
 * not exist yet — see the activate button handler for the expected signature.
 */
@Routable("/activate/{token}")
class ActivationPage(val token: String) : Page, FullscreenPage {
    override val title: Reactive<String> get() = Constant("Activate Account")

    override fun ElementWriter.CanAddTheme.render() {
        val firstName = Signal("")
        val lastName = Signal("")
        val password = Signal("")
        val passwordConfirm = Signal("")
        val enrollMfaNow = Signal(false)

        frame {
            centered.sizedBox(SizeConstraints(maxWidth = 40.rem)).scrolling.col {
                centered.h2("Activate your HeroScript account")
                centered.text("Set your name, password, and security options to get started.")

                card.col {
                    h4("Your name")
                    field("First name") { textInput { content bind firstName } }
                    field("Last name") { textInput { content bind lastName } }
                }

                card.col {
                    h4("Password")
                    field("Password") { textInput { content bind password; hint = "" } }
                    field("Confirm password") { textInput { content bind passwordConfirm; hint = "" } }
                    subtext { ::content { if (password() != passwordConfirm() && passwordConfirm().isNotEmpty()) "Passwords do not match" else "" } }
                }

                card.col {
                    h4("Two-factor authentication")
                    text("Add an extra layer of security with an authenticator app. You can also set this up later from your profile.")
                    row {
                        toggleButton {
                            checked bind enrollMfaNow
                            centered.text { ::content { if (enrollMfaNow()) "Set up later" else "Set up now" } }
                        }
                    }
                    shownWhen { enrollMfaNow() }.col {
                        text("TODO: Render TOTP QR code + recovery codes once the server exposes an enrollment endpoint backed by this invite token.")
                    }
                }

                important.buttonTheme.button {
                    centered.text("Activate account")
                    ::enabled {
                        firstName().isNotBlank()
                                && lastName().isNotBlank()
                                && password().length >= 8
                                && password() == passwordConfirm()
                    }
                    onClick {
                        // TODO: Server endpoint required —
                        //   api.userAuth.consumeInviteToken(
                        //       ConsumeInviteToken(token, firstName, lastName, password, enrollMfa)
                        //   ): String  // returns refresh token
                        // For now, route to login with a notice.
                        context.toast("Account activation backend not yet wired. Please sign in with the credentials provided in your invite email.")
                        context.pageNavigator.reset(LoginPage())
                    }
                }

                centered.button {
                    centered.text("Already have an account? Sign in")
                    onClick { context.pageNavigator.reset(LoginPage()) }
                }
            }
        }
    }
}
