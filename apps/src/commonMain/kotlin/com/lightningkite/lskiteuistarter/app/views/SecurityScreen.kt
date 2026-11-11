package com.lightningkite.lskiteuistarter.app.views

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.auth.SecurityComponent
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lightningserver.auth.AuthEndpoints
import com.lightningkite.lightningserver.auth.accessToken
import com.lightningkite.lskiteuistarter.app.sdk.currentSession
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("/security")
class SecurityPage : Page {
    override val title: Reactive<String> get() = Constant("Security")

    override fun ElementWriter.CanAddTheme.render() {
        // The delay matters; see HomePage.
        reactive {
            if (currentSession() == null)
                launch { delay(50); context.pageNavigator.reset(LandingPage()) }
        }

        val security = remember {
            val session = currentSession() ?: return@remember null
            val api = session.api
            SecurityComponent(
                endpoints = AuthEndpoints(
                    subjects = mapOf("User" to api.userAuth),
                    emailProof = api.userAuth.email,
                    oneTimePasswordProof = api.userAuth.totp,
                    backupCodeProof = api.userAuth.backupCode,
                    passwordProof = api.userAuth.password,
                    webAuthNProof = api.userAuth.webAuthN,
                ),
                subjectId = session.userId.toString(),
                elevate = { token ->
                    val accessToken = api.userAuth.accessToken(token)
                    val auth = api.withMetadata { set("Authorization", accessToken()) }.userAuth
                    SecurityComponent.Elevated(
                        password = auth.password,
                        totp = SecurityComponent.TotpEndpoints(auth.totp),
                        webAuthN = SecurityComponent.WebAuthNEndpoints(auth.webAuthN),
                        backupCode = auth.backupCode,
                    )
                },
            )
        }

        scrolling.col {
            centered.sizeConstraints(maxWidth = 40.rem).col {
                h1("Security")
                card.frame {
                    reactive {
                        clearChildren()
                        security()?.render(this@frame)
                    }
                }
            }
        }
    }
}
