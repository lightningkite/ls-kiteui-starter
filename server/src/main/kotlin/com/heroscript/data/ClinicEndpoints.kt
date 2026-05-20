package com.heroscript.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.heroscript.*
import com.heroscript.UserAuth.ClinicMembershipsCache.clinicAdminIds
import com.heroscript.UserAuth.ClinicMembershipsCache.clinicIds
import com.heroscript.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object ClinicEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myClinics = auth.clinicIds()
            val myAdminClinics = auth.clinicAdminIds()

            val systemAdmin = condition<Clinic>(isSystemAdmin)
            val isMember = condition<Clinic> { it._id inside myClinics }
            val isClinicAdmin = condition<Clinic> { it._id inside myAdminClinics }

            ModelPermissions(
                create = systemAdmin,
                read = systemAdmin or isMember,
                update = systemAdmin or isClinicAdmin,
                updateRestrictions = updateRestrictions {
                    it.createdAt.cannotBeModified()
                    it.stripePaymentId.requires(systemAdmin or isClinicAdmin)
                    it.stripePaymentType.requires(systemAdmin or isClinicAdmin)
                },
                delete = systemAdmin,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}

val Clinic.Companion.info get() = ClinicEndpoints.info
