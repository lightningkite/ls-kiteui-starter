package com.heroscript.views.orders

import com.heroscript.PrescriptionOrder
import com.heroscript.extensions.derivedStatus
import com.heroscript.sdk.currentSession
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember
import kotlin.time.Duration.Companion.hours
import kotlin.time.Clock

/**
 * Reason an order needs Ops attention. Computed against the order itself plus the
 * (optionally loaded) sibling PharmacyOrder.
 */
enum class OrderAlertReason(val label: String) {
    Stuck("Stuck — submitted > 1h"),
    Rejected("Rejected"),
    AddressIssue("Address verification failed"),
}

fun orderAlertReason(
    order: PrescriptionOrder,
    pharmacyOrder: com.heroscript.PharmacyOrder?,
    now: kotlin.time.Instant = Clock.System.now(),
): OrderAlertReason? {
    if (order.cancellation != null) return null
    if (order.fulfilled?.reject != null || pharmacyOrder?.totalRejection != null) return OrderAlertReason.Rejected
    if (order.destination.verifiedAt == null) return OrderAlertReason.AddressIssue
    val submitted = order.clinicianReview?.takeIf { it.approved } != null
    val accepted = pharmacyOrder?.accepted != null
    if (submitted && !accepted && now - order.createdAt > 1.hours) return OrderAlertReason.Stuck
    return null
}

/**
 * Shared row body for the PrescriptionOrder list (used by both OrdersListPage and the
 * Network Order Monitor). Renders patient · product, derived status, strength, prescriber,
 * createdAt — plus optional clinic name and alert badge for the Ops view.
 */
fun ElementWriter.CanAddTheme.orderRowContent(
    order: Reactive<PrescriptionOrder>,
    showClinic: Boolean = false,
    showAlert: Boolean = false,
) = col {
    val pharmacyOrder = remember {
        val session = currentSession() ?: return@remember null
        order().fulfilled?.by?.let { session.pharmacyOrders[it].invoke() }
    }
    val shipment = remember {
        val session = currentSession() ?: return@remember null
        order().shipment?.let { session.shipments[it].invoke() }
    }
    val patient = remember {
        val session = currentSession() ?: return@remember null
        session.patients[order().patient].invoke()
    }
    val product = remember {
        val session = currentSession() ?: return@remember null
        session.products[order().product].invoke()
    }
    val prescriber = remember {
        val session = currentSession() ?: return@remember null
        session.users[order().prescribedBy].invoke()
    }
    val clinic = remember {
        if (!showClinic) return@remember null
        val session = currentSession() ?: return@remember null
        session.clinics[order().clinic].invoke()
    }

    row {
        expanding.col {
            row {
                expanding.h4 {
                    ::content {
                        val patientName = patient()?.displayName ?: "—"
                        val productName = product()?.name ?: "—"
                        "$patientName · $productName"
                    }
                }
                subtext {
                    ::content { derivedStatus(order(), pharmacyOrder(), shipment()).label }
                }
            }
            subtext {
                ::content {
                    val o = order()
                    val form = product()?.forms?.firstOrNull { it.form == o.form }
                    val unit = form?.strengthUnit
                    if (unit != null) "${o.strength} $unit" else o.strength.toString()
                }
            }
            if (showClinic) {
                subtext { ::content { clinic()?.name ?: "—" } }
            }
            row {
                subtext { ::content { prescriber()?.displayName ?: "—" } }
                subtext { ::content { "Submitted ${order().createdAt}" } }
            }
            if (showAlert) {
                shownWhen { orderAlertReason(order(), pharmacyOrder()) != null }.subtext {
                    ::content { orderAlertReason(order(), pharmacyOrder())?.label ?: "" }
                }
            }
        }
        shownWhen { shipment()?.shippedAt != null }.col {
            centered.icon(Icon.send, "Tracking available")
        }
    }
}
