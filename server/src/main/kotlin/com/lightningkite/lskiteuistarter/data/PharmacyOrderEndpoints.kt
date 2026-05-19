package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.ClinicMembershipsCache.clinicIds
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object PharmacyOrderEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myClinics = auth.clinicIds()

            val systemAdmin = condition<PharmacyOrder>(isSystemAdmin)
            val inMyClinic = condition<PharmacyOrder> { it.clinic inside myClinics }

            ModelPermissions(
                create = systemAdmin,
                read = systemAdmin or inMyClinic,
                update = systemAdmin,
                delete = systemAdmin,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}

val PharmacyOrder.Companion.info get() = PharmacyOrderEndpoints.info
