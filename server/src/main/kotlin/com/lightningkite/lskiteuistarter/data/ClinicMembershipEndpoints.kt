package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.ClinicMembershipsCache.clinicAdminIds
import com.lightningkite.lskiteuistarter.UserAuth.ClinicMembershipsCache.clinicMemberships
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object ClinicMembershipEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myMemberships = auth.clinicMemberships()
            val myClinics = myMemberships.map { it.clinic }.toSet()
            val myAdminClinics = auth.clinicAdminIds()

            val systemAdmin = condition<ClinicMembership>(isSystemAdmin)
            val mine = condition<ClinicMembership> { it.user eq auth.id }
            val inMyClinic = condition<ClinicMembership> { it.clinic inside myClinics }
            val inAdminClinic = condition<ClinicMembership> { it.clinic inside myAdminClinics }

            ModelPermissions(
                create = systemAdmin or inAdminClinic,
                read = systemAdmin or mine or inMyClinic,
                update = systemAdmin or inAdminClinic or mine,
                updateRestrictions = updateRestrictions {
                    it.clinic.cannotBeModified()
                    it.user.cannotBeModified()
                    it.invitedAt.cannotBeModified()
                    it.invitedBy.cannotBeModified()
                    it.role.requires(systemAdmin or inAdminClinic)
                    it.acceptedAt.requires(systemAdmin or inAdminClinic or mine)
                    it.deactivatedAt.requires(systemAdmin or inAdminClinic)
                },
                delete = systemAdmin or inAdminClinic,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}

val ClinicMembership.Companion.info get() = ClinicMembershipEndpoints.info
