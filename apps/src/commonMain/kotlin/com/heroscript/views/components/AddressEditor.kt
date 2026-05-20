package com.heroscript.views.components

import com.heroscript.Address
import com.heroscript.VerifiedAddress
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive
import kotlin.time.Clock

/**
 * Edits a [VerifiedAddress] in place. Renders inputs in edit mode and a read-only summary
 * otherwise. Verification stamps `verifiedAt` + a `verificationProvider` tag.
 *
 * TODO: Replace the "Verify address" body with a real Smarty/Lob lookup — the stub stamps
 * `verificationProvider = "manual"` so that downstream pharmacy filtering and the ship-to
 * gating on Order Entry can proceed against today's manually-keyed addresses.
 */
fun ElementWriter.CanAddTheme.addressEditor(
    value: MutableReactive<VerifiedAddress>,
    editing: Reactive<Boolean>,
) {
    col {
        shownWhen { editing() }.col {
            field("Recipient") {
                textInput {
                    keyboardHints = KeyboardHints.title
                    content bind value.lens(
                        get = { it.address.recipient },
                        modify = { old, it -> old.copy(address = old.address.copy(recipient = it)) },
                    )
                }
            }
            field("Address line 1") {
                textInput {
                    content bind value.lens(
                        get = { it.address.line1 },
                        modify = { old, it -> old.copy(address = old.address.copy(line1 = it)) },
                    )
                }
            }
            field("Address line 2") {
                textInput {
                    content bind value.lens(
                        get = { it.address.line2 ?: "" },
                        modify = { old, it ->
                            old.copy(address = old.address.copy(line2 = it.takeIf { s -> s.isNotBlank() }))
                        },
                    )
                }
            }
            field("City") {
                textInput {
                    content bind value.lens(
                        get = { it.address.city },
                        modify = { old, it -> old.copy(address = old.address.copy(city = it)) },
                    )
                }
            }
            row {
                expanding.field("State") {
                    textInput {
                        hint = "e.g. CA"
                        content bind value.lens(
                            get = { it.address.state },
                            modify = { old, it -> old.copy(address = old.address.copy(state = it.uppercase().take(2))) },
                        )
                    }
                }
                expanding.field("ZIP") {
                    textInput {
                        keyboardHints = KeyboardHints.integer
                        content bind value.lens(
                            get = { it.address.zip },
                            modify = { old, it -> old.copy(address = old.address.copy(zip = it)) },
                        )
                    }
                }
            }

            row {
                button {
                    text("Verify address")
                    onClick {
                        value.set(
                            value().copy(
                                verifiedAt = Clock.System.now(),
                                verificationProvider = "manual",
                            )
                        )
                    }
                }
                expanding.space()
                shownWhen { value().verifiedAt != null }.button {
                    text("Mark unverified")
                    onClick {
                        value.set(value().copy(verifiedAt = null, verificationProvider = null))
                    }
                }
            }
        }

        shownWhen { !editing() }.col {
            text { ::content { value().address.recipient } }
            text { ::content { value().address.line1 } }
            shownWhen { !value().address.line2.isNullOrBlank() }.text { ::content { value().address.line2 ?: "" } }
            text {
                ::content {
                    val a = value().address
                    "${a.city}, ${a.state} ${a.zip}"
                }
            }
        }

        row {
            shownWhen { value().verifiedAt != null }.text {
                ::content {
                    val provider = value().verificationProvider ?: "manual"
                    "Verified ($provider)"
                }
            }
            shownWhen { value().verifiedAt == null }.warning.text("Address not verified")
        }
    }
}

fun Address.isFilledOut(): Boolean =
    recipient.isNotBlank() && line1.isNotBlank() && city.isNotBlank() && state.length == 2 && zip.isNotBlank()
