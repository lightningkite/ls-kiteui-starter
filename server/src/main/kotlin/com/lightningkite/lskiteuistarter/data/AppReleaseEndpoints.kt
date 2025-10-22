package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.typed.AuthAccess
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.AppRelease
import com.lightningkite.lskiteuistarter.Server
import com.lightningkite.lskiteuistarter.User
import com.lightningkite.lskiteuistarter.UserAuth
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.lskiteuistarter.UserRole
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.ModelPermissions

object AppReleaseEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = { permissions(this) },
    )
    val rest = path include ModelRestEndpoints(info)

    context(server: ServerRuntime)
    suspend fun permissions(auth: AuthAccess<User>): ModelPermissions<AppRelease> {
        return if (auth.userRole() < UserRole.Admin) {
            ModelPermissions(read = Condition.Always)
        } else {
            ModelPermissions.allowAll()
        }
    }
}