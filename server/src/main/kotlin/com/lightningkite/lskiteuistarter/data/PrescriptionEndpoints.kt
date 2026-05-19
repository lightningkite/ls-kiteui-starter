package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.ClinicMembershipsCache.clinicAdminIds
import com.lightningkite.lskiteuistarter.UserAuth.ClinicMembershipsCache.clinicIds
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object PrescriptionEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myClinics = auth.clinicIds()
            val myAdminClinics = auth.clinicAdminIds()

            val systemAdmin = condition<Prescription>(isSystemAdmin)
            val inMyClinic = condition<Prescription> { it.clinic inside myClinics }
            val inMyAdminClinic = condition<Prescription> { it.clinic inside myAdminClinics }

            ModelPermissions(
                create = systemAdmin or inMyClinic,
                read = systemAdmin or inMyClinic,
                update = systemAdmin or inMyClinic,
                updateRestrictions = updateRestrictions {
                    it.clinic.cannotBeModified()
                    it.patient.cannotBeModified()
                    it.prescribedBy.cannotBeModified()
                    it.createdAt.cannotBeModified()
                },
                delete = systemAdmin or inMyAdminClinic,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}

val Prescription.Companion.info get() = PrescriptionEndpoints.info
