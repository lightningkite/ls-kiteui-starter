package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.ModelPermissions

object ShipmentEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = { permissions(this) },
    )
    val rest = path include ModelRestEndpoints(info)

    // V1: tracking info isn't PHI (patients also get it from the carrier), and Shipment.ID
    // is an unguessable UUID — so we allow any authenticated user to read. Writes are
    // restricted to system admins (set by pharmacy webhooks in the future).
    context(server: ServerRuntime)
    suspend fun permissions(auth: AuthAccess<User>): ModelPermissions<Shipment> {
        return if (auth.userRole() < UserRole.Admin) {
            ModelPermissions(read = Condition.Always)
        } else {
            ModelPermissions.allowAll()
        }
    }
}

val Shipment.Companion.info get() = ShipmentEndpoints.info
