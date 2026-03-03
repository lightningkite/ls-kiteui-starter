// by Claude — CRUD endpoints for InventoryItem with org-scoped permissions
package com.lightningkite.lskiteuistarter.data

import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.UserAuth.MembershipsCache.memberships
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.services.database.*

object InventoryItemEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val isSystemAdmin = auth.userRole() >= UserRole.Admin
            val myMemberships = auth.memberships()
            val myOrgIds = myMemberships.map { it.organization }.toSet()
            val adminOrgIds = myMemberships.filter { it.role >= MemberRole.Admin }.map { it.organization }.toSet()

            val systemAdmin = condition<InventoryItem>(isSystemAdmin)
            val inMyOrg = condition<InventoryItem> { it.organization inside myOrgIds }
            val inAdminOrg = condition<InventoryItem> { it.organization inside adminOrgIds }

            ModelPermissions(
                create = systemAdmin or inAdminOrg,
                read = systemAdmin or inMyOrg,
                update = systemAdmin or inAdminOrg,
                updateRestrictions = updateRestrictions {
                    it.organization.cannotBeModified()
                    it.createdAt.cannotBeModified()
                },
                delete = systemAdmin or inAdminOrg,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
}
