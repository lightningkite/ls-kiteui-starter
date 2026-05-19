package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.ModelPermissions

object ProductPharmacyMappingEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = { permissions(this) },
    )
    val rest = path include ModelRestEndpoints(info)

    context(server: ServerRuntime)
    suspend fun permissions(auth: AuthAccess<User>): ModelPermissions<ProductPharmacyMapping> {
        return if (auth.userRole() < UserRole.Admin) {
            ModelPermissions(read = Condition.Always)
        } else {
            ModelPermissions.allowAll()
        }
    }
}

val ProductPharmacyMapping.Companion.info get() = ProductPharmacyMappingEndpoints.info
