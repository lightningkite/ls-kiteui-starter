// by Claude — CRUD and permission-boundary tests for all endpoints
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.ForbiddenException
import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.testAuth
import com.lightningkite.lightningserver.definition.GeneralServerSettings
import com.lightningkite.lightningserver.definition.generalSettings
import com.lightningkite.lightningserver.fixtures.deleteSeedData
import com.lightningkite.lightningserver.runtime.test.execute
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lightningserver.typed.test
import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.data.OrganizationEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.Modification
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.TextIndexQuery
import com.lightningkite.services.database.inside
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.insertOne
import com.lightningkite.services.data.toEmailAddress
import kotlinx.coroutines.flow.toList
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
            val admin = execute { UserEndpoints.info.table().insertOne(
                User(
                    email = "admin@test.com".toEmailAddress(),
                    name = "Admin",
                    role = UserRole.Admin,
                )
            ) }

            // Create org via direct table insert (admin has system-level access)
            val org = execute { OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            ) }

            // Verify admin can read org
            val fetched = OrganizationEndpoints.rest.detail.test(org._id, UserAuth.testAuth(admin), Unit)
            assertEquals("Test Org", fetched.name)
        }
    }

    @Test
    fun membershipCrud(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create admin user and org
            val admin = execute { UserEndpoints.info.table().insertOne(
                User(
                    email = "admin@test.com".toEmailAddress(),
                    name = "Admin",
                    role = UserRole.Admin,
                )
            ) }
            val org = execute { OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            ) }

            // Create a regular user
            val member = execute { UserEndpoints.info.table().insertOne(
                User(
                    email = "member@test.com".toEmailAddress(),
                    name = "Member",
                    role = UserRole.User,
                )
            ) }

            // Create membership
            val membership = execute { MembershipEndpoints.info.table().insertOne(
                Membership(
                    organization = org._id,
                    user = member._id,
                    role = MemberRole.Member,
                )
            ) }

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
            val admin = execute { UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Admin", role = UserRole.Admin)
            ) }
            val regularUser = execute { UserEndpoints.info.table().insertOne(
                User(email = "regular@test.com".toEmailAddress(), name = "Regular", role = UserRole.User)
            ) }
            val orgMember = execute { UserEndpoints.info.table().insertOne(
                User(email = "member@test.com".toEmailAddress(), name = "Member", role = UserRole.User)
            ) }

            val org = execute { OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Private Org")
            ) }

            // Add orgMember as a Member (not admin) of the org
            execute { MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = orgMember._id, role = MemberRole.Member)
            ) }

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
                    Modification<Organization> { it.name assign "Renamed" }
                )
            }

            // System admin CAN update the org
            val updated = OrganizationEndpoints.rest.modify.test(
                org._id, adminAuth,
                Modification<Organization> { it.name assign "Renamed Org" }
            )
            assertEquals("Renamed Org", updated.name)
        }
    }

    @Test
    fun membershipPermissions(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val admin = execute { UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Admin", role = UserRole.Admin)
            ) }
            val orgAdmin = execute { UserEndpoints.info.table().insertOne(
                User(email = "orgadmin@test.com".toEmailAddress(), name = "OrgAdmin", role = UserRole.User)
            ) }
            val regularMember = execute { UserEndpoints.info.table().insertOne(
                User(email = "member@test.com".toEmailAddress(), name = "Member", role = UserRole.User)
            ) }
            val outsider = execute { UserEndpoints.info.table().insertOne(
                User(email = "outsider@test.com".toEmailAddress(), name = "Outsider", role = UserRole.User)
            ) }

            val org = execute { OrganizationEndpoints.info.table().insertOne(
                Organization(name = "Test Org")
            ) }

            // Set up: orgAdmin is Admin of the org, regularMember is a Member
            execute { MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = orgAdmin._id, role = MemberRole.Admin)
            ) }
            execute { MembershipEndpoints.info.table().insertOne(
                Membership(organization = org._id, user = regularMember._id, role = MemberRole.Member)
            ) }

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
            execute { MembershipEndpoints.info.table().deleteOne(Condition<Membership> { it._id eq newMembership._id }) }
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
            val regularUser = execute { UserEndpoints.info.table().insertOne(
                User(email = "user@test.com".toEmailAddress(), name = "User", role = UserRole.User)
            ) }

            val userAuth = UserAuth.testAuth(regularUser)

            // User can read their own record
            val self = UserEndpoints.rest.endpoints.detail.test(regularUser._id, userAuth, Unit)
            assertEquals("User", self.name)

            // User can modify their own name
            val updated = UserEndpoints.rest.endpoints.modify.test(
                regularUser._id, userAuth,
                Modification<User> { it.name assign "Updated Name" }
            )
            assertEquals("Updated Name", updated.name)

            // User CANNOT escalate their own role — role.requires(admin)
            // The updateRestrictions narrow the query condition so the record doesn't match,
            // resulting in NotFoundException (the record "disappears" when attempting forbidden modifications).
            assertFailsWith<NotFoundException> {
                UserEndpoints.rest.endpoints.modify.test(
                    regularUser._id, userAuth,
                    Modification<User> { it.role assign UserRole.Admin }
                )
            }
        }
    }

    @Test
    fun authFlow(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            // Create users with different roles
            val adminUser = execute { UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Admin", role = UserRole.Admin)
            ) }
            val regularUser = execute { UserEndpoints.info.table().insertOne(
                User(email = "user@test.com".toEmailAddress(), name = "User", role = UserRole.User)
            ) }

            // Verify testAuth produces correct authentication
            val adminAuth = UserAuth.testAuth(adminUser)
            val userAuth = UserAuth.testAuth(regularUser)

            // Admin can list users they have permission to see
            val adminResults = UserEndpoints.rest.endpoints.list.test(adminAuth, Query<User>())
            assertTrue(adminResults.isNotEmpty(), "Admin should see at least one user")
            assertTrue(adminResults.any { it._id == adminUser._id }, "Admin should see themselves")
            assertTrue(adminResults.any { it._id == regularUser._id }, "Admin should see the regular user")

            // Regular user can only see their own record
            val userResults = UserEndpoints.rest.endpoints.list.test(userAuth, Query<User>())
            assertEquals(1, userResults.size, "Regular user should only see themselves")
            assertEquals(regularUser._id, userResults.first()._id)
        }
    }

    @Test
    fun userTextSearchRespectsReadPermissions(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val admin = execute { UserEndpoints.info.table().insertOne(
                User(email = "admin@test.com".toEmailAddress(), name = "Alice Admin", role = UserRole.Admin)
            ) }
            val bob = execute { UserEndpoints.info.table().insertOne(
                User(email = "bob@test.com".toEmailAddress(), name = "Bob Builder")
            ) }
            val carol = execute { UserEndpoints.info.table().insertOne(
                User(email = "carol@test.com".toEmailAddress(), name = "Carol Contractor")
            ) }

            val adminResults = UserEndpoints.rest.endpoints.textQuery.test(UserAuth.testAuth(admin), TextIndexQuery("bob"))
            assertEquals(listOf(bob._id), adminResults.map { it.model._id })

            val carolResults = UserEndpoints.rest.endpoints.textQuery.test(UserAuth.testAuth(carol), TextIndexQuery("bob"))
            assertEquals(emptyList(), carolResults, "Search must not reveal users the caller can't read")
        }
    }

    @Test
    fun seedDataCanBeResetAndReseeded(): Unit = runBlocking {
        Server.test(settings = {
            database set Database.Settings("ram")
            generalSettings set GeneralServerSettings(debug = true)
        }) {
            val handMade = execute { UserEndpoints.info.table().insertOne(
                User(email = "real@test.com".toEmailAddress(), name = "Real Person")
            ) }

            execute { SeedData.fixtures.execute(SeedConfig()) }
            val seededUsers = execute { UserEndpoints.info.table().find(Condition.Always).toList() }
            assertNotNull(seededUsers.find { it._id == SeedData.admin._id })
            assertEquals(1 + 21, seededUsers.size)
            val adminOwnerships = execute {
                MembershipEndpoints.info.table().find(Condition { it.user eq SeedData.admin._id }).toList()
            }
            assertEquals(3, adminOwnerships.size)

            execute {
                deleteSeedData()
                SeedData.fixtures.execute(SeedConfig())
            }
            val reseeded = execute { UserEndpoints.info.table().find(Condition.Always).toList() }
            assertEquals(seededUsers.map { it._id }.toSet(), reseeded.map { it._id }.toSet(), "Seeding is deterministic")
            assertNotNull(reseeded.find { it._id == handMade._id }, "Hand-made rows survive a reset")
        }
    }
}
