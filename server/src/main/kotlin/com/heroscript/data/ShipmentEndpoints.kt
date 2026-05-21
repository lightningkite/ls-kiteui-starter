package com.heroscript.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.typed.*
import com.heroscript.*
import com.heroscript.UserAuth.ClinicMembershipsCache.clinicIds
import com.heroscript.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object ShipmentEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myClinics = auth.clinicIds()

            val systemAdmin = condition<Shipment>(isSystemAdmin)
            // Shipment.clinics is denormalized from any PrescriptionOrder.shipment pointing here,
            // maintained by the PrescriptionOrder change listener. A clinic member may read a
            // shipment iff their clinic is one that referenced it.
            val anyMyClinic = condition<Shipment> { it.clinics.any { c -> c inside myClinics } }

            ModelPermissions(
                read = systemAdmin or anyMyClinic,
                update = systemAdmin,
                create = systemAdmin,
                delete = systemAdmin,
            )
        },
    )
    val rest = path include ModelRestEndpoints(info)
}

val Shipment.Companion.info get() = ShipmentEndpoints.info
