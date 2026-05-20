package com.heroscript.views.orders

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Stub ID.me step-up modal: shows a 1-second "verifying" dialog then invokes [onSuccess].
 * Single source of truth so the Submit action behaves identically wherever it is offered
 * (Order Entry and Order Detail). Replaces with a real ID.me OIDC flow when § F4 lands.
 */
fun openIdMeStub(ctx: ElementContext, onSuccess: suspend () -> Unit) {
    ctx.dialog { close ->
        col {
            h3("ID.me verification")
            text("Verifying with ID.me...")
            subtext("(stub - auto-succeeds in 1 second)")
            row {
                space()
                button {
                    text("Cancel")
                    onClick { close() }
                }
            }
        }
        AppScope.launch {
            delay(1000.milliseconds)
            close()
            onSuccess()
        }
    }
}
