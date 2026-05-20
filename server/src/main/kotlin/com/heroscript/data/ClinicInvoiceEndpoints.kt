package com.heroscript.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.heroscript.*
import com.heroscript.UserAuth.ClinicMembershipsCache.clinicAdminIds
import com.heroscript.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object ClinicInvoiceEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myAdminClinics = auth.clinicAdminIds()

            val systemAdmin = condition<ClinicInvoice>(isSystemAdmin)
            val inMyAdminClinic = condition<ClinicInvoice> { it.clinic inside myAdminClinics }

            ModelPermissions(
                create = systemAdmin,
                // Only ClinicAdmin members see billing; regular clinic members do not.
                read = systemAdmin or inMyAdminClinic,
                update = systemAdmin,
                delete = systemAdmin,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}

val ClinicInvoice.Companion.info get() = ClinicInvoiceEndpoints.info
