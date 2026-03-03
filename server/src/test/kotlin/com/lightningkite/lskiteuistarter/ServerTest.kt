// by Claude — CRUD and permission-boundary tests for all endpoints
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.ForbiddenException
import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.testAuth
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lightningserver.typed.test
import com.lightningkite.lskiteuistarter.data.InventoryItemEndpoints
import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.data.OrganizationEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.Modification
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.insertOne
import com.lightningkite.services.database.modification
import com.lightningkite.toEmailAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerTest {

    @Test
    fun organizationCrud(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create admin user
            val admin = UserEndpoints.info.table().insertOne(
                User(
                    email = "admin@test.com".toEmailAddress(),
                    name = "Admin",
                    role = UserRole.Admin,
                )
            )!!

            // Create org via direct table insert (admin has system-level access)
            val org = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            )!!

            // Verify admin can read org
            val fetched = OrganizationEndpoints.rest.detail.test(org._id, UserAuth.testAuth(admin), Unit)
            assertEquals("Test Org", fetched.name)
        }
    }

    @Test
    fun membershipCrud(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create admin user and org
            val admin = UserEndpoints.info.table().insertOne(
                User(
                    email = "admin@test.com".toEmailAddress(),
                    name = "Admin",
                    role = UserRole.Admin,
                )
            )!!
            val org = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            )!!

            // Create a regular user
            val member = UserEndpoints.info.table().insertOne(
                User(
                    email = "member@test.com".toEmailAddress(),
                    name = "Member",
                    role = UserRole.User,
                )
            )!!

            // Create membership
            val membership = MembershipEndpoints.info.table().insertOne(
                Membership(
                    organization = org._id,
                    user = member._id,
                    role = MemberRole.Member,
                )
            )!!

            assertNotNull(membership)
            assertEquals(org._id, membership.organization)
            assertEquals(member._id, membership.user)
            assertEquals(MemberRole.Member, membership.role)
        }
    }

    @Test
    fun organizationPermissions(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Set up: admin, regular user, org, and membership
            val admin = UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Admin", role = UserRole.Admin)
            )!!
            val regularUser = UserEndpoints.info.table().insertOne(
                User(email = "regular@test.com".toEmailAddress(), name = "Regular", role = UserRole.User)
            )!!
            val orgMember = UserEndpoints.info.table().insertOne(
                User(email = "member@test.com".toEmailAddress(), name = "Member", role = UserRole.User)
            )!!

            val org = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Private Org")
            )!!

            // Add orgMember as a Member (not admin) of the org
            MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = orgMember._id, role = MemberRole.Member)
            )

            val adminAuth = UserAuth.testAuth(admin)
            val regularAuth = UserAuth.testAuth(regularUser)
            val memberAuth = UserAuth.testAuth(orgMember)

            // Non-member can't read the org (returns NotFoundException since read filter hides it)
            assertFailsWith<NotFoundException> {
                OrganizationEndpoints.rest.detail.test(org._id, regularAuth, Unit)
            }

            // Member CAN read the org
            val fetched = OrganizationEndpoints.rest.detail.test(org._id, memberAuth, Unit)
            assertEquals("Private Org", fetched.name)

            // Member (non-admin) can't update the org
            assertFailsWith<NotFoundException> {
                OrganizationEndpoints.rest.modify.test(
                    org._id, memberAuth,
                    modification<Organization> { it.name assign "Renamed" }
                )
            }

            // System admin CAN update the org
            val updated = OrganizationEndpoints.rest.modify.test(
                org._id, adminAuth,
                modification<Organization> { it.name assign "Renamed Org" }
            )
            assertEquals("Renamed Org", updated.name)
        }
    }

    @Test
    fun membershipPermissions(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val admin = UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Admin", role = UserRole.Admin)
            )!!
            val orgAdmin = UserEndpoints.info.table().insertOne(
                User(email = "orgadmin@test.com".toEmailAddress(), name = "OrgAdmin", role = UserRole.User)
            )!!
            val regularMember = UserEndpoints.info.table().insertOne(
                User(email = "member@test.com".toEmailAddress(), name = "Member", role = UserRole.User)
            )!!
            val outsider = UserEndpoints.info.table().insertOne(
                User(email = "outsider@test.com".toEmailAddress(), name = "Outsider", role = UserRole.User)
            )!!

            val org = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            )!!

            // Set up: orgAdmin is Admin of the org, regularMember is a Member
            MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = orgAdmin._id, role = MemberRole.Admin)
            )
            MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = regularMember._id, role = MemberRole.Member)
            )

            val orgAdminAuth = UserAuth.testAuth(orgAdmin)
            val memberAuth = UserAuth.testAuth(regularMember)
            val outsiderAuth = UserAuth.testAuth(outsider)

            // Org admin CAN create a new membership
            val newMembership = MembershipEndpoints.rest.insert.test(
                orgAdminAuth,
                Membership(organization = org._id, user = outsider._id, role = MemberRole.Member)
            )
            assertNotNull(newMembership)

            // Regular member can NOT create memberships (not org admin)
            assertFailsWith<ForbiddenException> {
                MembershipEndpoints.rest.insert.test(
                    memberAuth,
                    Membership(organization = org._id, user = admin._id, role = MemberRole.Member)
                )
            }

            // Outsider (not in org) can NOT see memberships in the org
            // First remove the membership we just created for outsider so they're truly outside
            MembershipEndpoints.info.table().deleteOne(condition<Membership> { it._id eq newMembership._id })
            val outsiderResults = MembershipEndpoints.rest.list.test(
                outsiderAuth,
                Query<Membership>()
            )
            assertTrue(outsiderResults.isEmpty(), "Outsider should not see any memberships")
        }
    }

    @Test
    fun userRoleEscalation(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val regularUser = UserEndpoints.info.table().insertOne(
                User(email = "user@test.com".toEmailAddress(), name = "User", role = UserRole.User)
            )!!

            val userAuth = UserAuth.testAuth(regularUser)

            // User can read their own record
            val self = UserEndpoints.rest.detail.test(regularUser._id, userAuth, Unit)
            assertEquals("User", self.name)

            // User can modify their own name
            val updated = UserEndpoints.rest.modify.test(
                regularUser._id, userAuth,
                modification<User> { it.name assign "Updated Name" }
            )
            assertEquals("Updated Name", updated.name)

            // User CANNOT escalate their own role — role.requires(admin)
            // The updateRestrictions narrow the query condition so the record doesn't match,
            // resulting in NotFoundException (the record "disappears" when attempting forbidden modifications).
            assertFailsWith<NotFoundException> {
                UserEndpoints.rest.modify.test(
                    regularUser._id, userAuth,
                    modification<User> { it.role assign UserRole.Admin }
                )
            }
        }
    }

    // by Claude — Inventory item CRUD test
    @Test
    fun inventoryItemCrud(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create user, org, membership
            val user = UserEndpoints.info.table().insertOne(
                User(email = "member@test.com".toEmailAddress(), name = "Member", role = UserRole.User)
            )!!
            val org = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            )!!
            MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = user._id, role = MemberRole.Member)
            )

            // Insert item via table
            val item = InventoryItemEndpoints.info.table().insertOne(
                InventoryItem(organization = org._id, name = "Laptop", category = ItemCategory.Electronics, quantity = 10)
            )!!

            // Member can read item via endpoint
            val memberAuth = UserAuth.testAuth(user)
            val fetched = InventoryItemEndpoints.rest.detail.test(item._id, memberAuth, Unit)
            assertEquals("Laptop", fetched.name)
            assertEquals(ItemCategory.Electronics, fetched.category)
            assertEquals(10, fetched.quantity)
        }
    }

    // by Claude — Inventory item org-scoping test
    @Test
    fun inventoryItemOrgScoping(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create two users and two orgs
            val userA = UserEndpoints.info.table().insertOne(
                User(email = "usera@test.com".toEmailAddress(), name = "User A", role = UserRole.User)
            )!!
            val userB = UserEndpoints.info.table().insertOne(
                User(email = "userb@test.com".toEmailAddress(), name = "User B", role = UserRole.User)
            )!!
            val orgA = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Org A")
            )!!
            val orgB = OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Org B")
            )!!

            // User A is member of Org A, User B is member of Org B
            MembershipEndpoints.info.table().insertOne(
                Membership(organization = orgA._id, user = userA._id, role = MemberRole.Member)
            )
            MembershipEndpoints.info.table().insertOne(
                Membership(organization = orgB._id, user = userB._id, role = MemberRole.Member)
            )

            // Insert item in Org A
            InventoryItemEndpoints.info.table().insertOne(
                InventoryItem(organization = orgA._id, name = "Widget", quantity = 5)
            )

            // User B should get empty list (org-scoped filtering)
            val userBAuth = UserAuth.testAuth(userB)
            val results = InventoryItemEndpoints.rest.list.test(userBAuth, Query<InventoryItem>())
            assertTrue(results.isEmpty(), "User B should not see items from Org A")

            // User A should see the item
            val userAAuth = UserAuth.testAuth(userA)
            val resultsA = InventoryItemEndpoints.rest.list.test(userAAuth, Query<InventoryItem>())
            assertEquals(1, resultsA.size, "User A should see 1 item from their org")
            assertEquals("Widget", resultsA.first().name)
        }
    }

    @Test
    fun authFlow(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create users with different roles
            val adminUser = UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Admin", role = UserRole.Admin)
            )!!
            val regularUser = UserEndpoints.info.table().insertOne(
                User(email = "user@test.com".toEmailAddress(), name = "User", role = UserRole.User)
            )!!

            // Verify testAuth produces correct authentication
            val adminAuth = UserAuth.testAuth(adminUser)
            val userAuth = UserAuth.testAuth(regularUser)

            // Admin can list users they have permission to see
            val adminResults = UserEndpoints.rest.list.test(adminAuth, Query<User>())
            assertTrue(adminResults.isNotEmpty(), "Admin should see at least one user")
            assertTrue(adminResults.any { it._id == adminUser._id }, "Admin should see themselves")
            assertTrue(adminResults.any { it._id == regularUser._id }, "Admin should see the regular user")

            // Regular user can only see their own record
            val userResults = UserEndpoints.rest.list.test(userAuth, Query<User>())
            assertEquals(1, userResults.size, "Regular user should only see themselves")
            assertEquals(regularUser._id, userResults.first()._id)
        }
    }
}
