package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.MembershipsCache.memberships
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object OrganizationEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myMemberships = auth.memberships()
            val myOrgIds = myMemberships.map { it.organization }.toSet()
            val adminOrgIds = myMemberships.filter { it.role >= MemberRole.Admin }.map { it.organization }.toSet()

            val systemAdmin = condition<Organization>(isSystemAdmin)
            val isMember = condition<Organization> { it._id inside myOrgIds }
            val isOrgAdmin = condition<Organization> { it._id inside adminOrgIds }

            ModelPermissions(
                create = systemAdmin,
                read = systemAdmin or isMember,
                update = systemAdmin or isOrgAdmin,
                delete = systemAdmin,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}
