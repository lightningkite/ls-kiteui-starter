// by Claude — Integration tests for MembersPage: display memberships, auth guard
package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.services.database.insertOne
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MembersIntegrationTest {

    @Test
    fun authenticatedUserSeesMembersPage() = integrationTest(
        initialPage = MembersPage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("membersTitle")
        assertTextVisible("Members")
    }

    @Test
    fun membersListShowsMembershipData() = integrationTest(
        initialPage = MembersPage(),
        setup = { runtime ->
            with(runtime) {
                val (user, org) = seedTestOrg(memberRole = MemberRole.Owner)
                val user2 = createUser(email = "member2@test.com", name = "Second Member")
                MembershipEndpoints.info.table().insertOne(
                    Membership(organization = org._id, user = user2._id, role = MemberRole.Member)
                )
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("membersList")
        pollForTexts("Role: Owner", "Role: Member")
    }

    @Test
    fun membersListShowsOnlyOwnOrgMembers() = integrationTest(
        initialPage = MembersPage(),
        setup = { runtime ->
            with(runtime) {
                val orgA = seedTestOrg(email = "alice@test.com", orgName = "Org A", memberRole = MemberRole.Owner)
                seedTestOrg(email = "bob@test.com", orgName = "Org B", memberRole = MemberRole.Admin)
                loginAs(orgA.user)
            }
        },
    ) { _ ->
        waitForId("membersList")
        pollForTexts("Role: Owner")

        // Count "Role:" occurrences — server-side permission filtering should return only one membership
        val snap = snapshot()
        val roleCount = snap.split("Role:").size - 1
        assertEquals(1, roleCount, "Should only see own org's members, got $roleCount role entries")
    }

    @Test
    fun unauthenticatedUserRedirectedFromMembers() = integrationTest(
        initialPage = MembersPage(),
    ) { _ ->
        pollForTexts("Sign in to get started")
    }
}
