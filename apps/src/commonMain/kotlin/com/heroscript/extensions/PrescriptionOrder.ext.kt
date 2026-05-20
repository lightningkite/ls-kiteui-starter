package com.heroscript.extensions

import com.heroscript.PharmacyOrder
import com.heroscript.PrescriptionOrder
import com.heroscript.Shipment

enum class DerivedStatus(val label: String) {
    PendingSubmission("Pending submission"),
    Submitted("Submitted"),
    Accepted("Accepted"),
    InProcess("In Process"),
    Shipped("Shipped"),
    Cancelled("Cancelled"),
    Rejected("Rejected"),
}

/**
 * Status derivation per ui.md "Order Detail" → "Status timeline derivation":
 *  - Cancelled  : order.cancellation != null
 *  - Rejected   : order.fulfilled.reject != null OR pharmacyOrder.totalRejection != null
 *  - Shipped    : shipment != null && shipment.shippedAt != null
 *  - In Process : Accepted AND (shipment == null OR shipment.shippedAt == null)
 *  - Accepted   : pharmacyOrder.accepted != null AND order.fulfilled.reject == null
 *  - Submitted  : order.clinicianReview != null && order.clinicianReview.approved
 *  - else       : PendingSubmission
 *
 * Accepted is the predecessor of InProcess. In a single-label representation, once accepted-but-not-shipped
 * we render "In Process" — Accepted as a discrete label only shows in the timeline alongside the current
 * state.
 */
fun derivedStatus(
    order: PrescriptionOrder,
    pharmacyOrder: PharmacyOrder?,
    shipment: Shipment?,
): DerivedStatus {
    if (order.cancellation != null) return DerivedStatus.Cancelled
    val reject = order.fulfilled?.reject ?: pharmacyOrder?.totalRejection
    if (reject != null) return DerivedStatus.Rejected
    if (shipment != null && shipment.shippedAt != null) return DerivedStatus.Shipped
    val accepted = pharmacyOrder?.accepted != null && order.fulfilled?.reject == null
    if (accepted) return DerivedStatus.InProcess
    val review = order.clinicianReview
    if (review != null && review.approved) return DerivedStatus.Submitted
    return DerivedStatus.PendingSubmission
}
