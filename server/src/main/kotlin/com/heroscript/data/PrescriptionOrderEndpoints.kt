package com.heroscript.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.heroscript.*
import com.heroscript.UserAuth.ClinicMembershipsCache.clinicAdminIds
import com.heroscript.UserAuth.ClinicMembershipsCache.clinicIds
import com.heroscript.UserAuth.ClinicMembershipsCache.prescriberClinicIds
import com.heroscript.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object PrescriptionOrderEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myClinics = auth.clinicIds()
            val myAdminClinics = auth.clinicAdminIds()
            val myPrescriberClinics = auth.prescriberClinicIds()

            val systemAdmin = condition<PrescriptionOrder>(isSystemAdmin)
            val inMyClinic = condition<PrescriptionOrder> { it.clinic inside myClinics }
            val inMyAdminClinic = condition<PrescriptionOrder> { it.clinic inside myAdminClinics }
            val inMyPrescriberClinic = condition<PrescriptionOrder> { it.clinic inside myPrescriberClinics }

            ModelPermissions(
                create = systemAdmin or inMyClinic,
                read = systemAdmin or inMyClinic,
                update = systemAdmin or inMyClinic,
                updateRestrictions = updateRestrictions {
                    it.prescription.cannotBeModified()
                    it.pharmacy.cannotBeModified()
                    it.clinic.cannotBeModified()
                    it.patient.cannotBeModified()
                    it.product.cannotBeModified()
                    it.form.cannotBeModified()
                    it.strength.cannotBeModified()
                    it.instructions.cannotBeModified()
                    it.prescribedBy.cannotBeModified()
                    it.createdBy.cannotBeModified()
                    it.createdAt.cannotBeModified()
                    // Only a Prescriber+ in this clinic (or system admin) can submit a clinician review.
                    it.clinicianReview.requires(systemAdmin or inMyPrescriberClinic)
                    // Fulfillment is set by the pharmacy webhook path; clinics never write it.
                    it.fulfilled.requires(systemAdmin)
                    // Shipment is set by the pharmacy webhook path; clinics never write it.
                    it.shipment.requires(systemAdmin)
                    // Destination, quantity, and willLastDays are editable by any clinic member.
                    // UI must disable editing once `clinicianReview` is set; Lightning's
                    // updateRestrictions can't express "depends on existing row state" for V1.
                    it.destination.requires(systemAdmin or inMyClinic)
                    it.quantity.requires(systemAdmin or inMyClinic)
                    it.willLastDays.requires(systemAdmin or inMyClinic)
                    // Workflow assignment + consent affirmation + cancellation: any clinic member.
                    it.assignedTo.requires(systemAdmin or inMyClinic)
                    it.consentAffirmedAt.requires(systemAdmin or inMyClinic)
                    it.cancellation.requires(systemAdmin or inMyClinic)
                },
                delete = systemAdmin or inMyAdminClinic,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}

val PrescriptionOrder.Companion.info get() = PrescriptionOrderEndpoints.info
