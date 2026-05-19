// Populates a local database with sample data for development.
// Run via: ./gradlew :server:serve --args="seed"
// Only works when general.debug = true in settings.json.
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.definition.generalSettings
import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.data.OrganizationEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.insertOne
import kotlinx.coroutines.runBlocking
import kotlin.uuid.Uuid

fun seed() = engine {
    if (!generalSettings().debug) {
        println("ERROR: Seed command requires general.debug = true in settings.json")
        return@engine
    }

    runBlocking {
        println("Seeding database with sample data...")

        val usersTable = UserEndpoints.info.table()
        val orgsTable = OrganizationEndpoints.info.table()
        val membershipsTable = MembershipEndpoints.info.table()

        // Admin user
        val admin = usersTable.insertOne(
            User(
                _id = User.ID(Uuid.fromLongs(0L, 100L)),
                email = "admin@example.com".toEmailAddress(),
                name = "Alice Admin",
                role = UserRole.Admin,
            )
        ) ?: run {
            println("Admin user already exists, skipping seed.")
            return@runBlocking
        }
        println("  Created admin user: ${admin.email} (${admin._id})")

        // Regular users
        val user1 = usersTable.insertOne(
            User(
                _id = User.ID(Uuid.fromLongs(0L, 101L)),
                email = "bob@example.com".toEmailAddress(),
                name = "Bob Builder",
                role = UserRole.User,
            )
        )!!
        println("  Created user: ${user1.email} (${user1._id})")

        val user2 = usersTable.insertOne(
            User(
                _id = User.ID(Uuid.fromLongs(0L, 102L)),
                email = "carol@example.com".toEmailAddress(),
                name = "Carol Contractor",
                role = UserRole.User,
            )
        )!!
        println("  Created user: ${user2.email} (${user2._id})")

        // Organizations
        val org1 = orgsTable.insertOne(
            Organization(
                _id = Organization.ID(Uuid.fromLongs(0L, 200L)),
                name = "Acme Corp",
            )
        )!!
        println("  Created org: ${org1.name} (${org1._id})")

        val org2 = orgsTable.insertOne(
            Organization(
                _id = Organization.ID(Uuid.fromLongs(0L, 201L)),
                name = "Widget Labs",
            )
        )!!
        println("  Created org: ${org2.name} (${org2._id})")

        // Memberships
        membershipsTable.insertOne(
            Membership(organization = org1._id, user = admin._id, role = MemberRole.Owner)
        )
        membershipsTable.insertOne(
            Membership(organization = org1._id, user = user1._id, role = MemberRole.Admin)
        )
        membershipsTable.insertOne(
            Membership(organization = org1._id, user = user2._id, role = MemberRole.Member)
        )
        membershipsTable.insertOne(
            Membership(organization = org2._id, user = admin._id, role = MemberRole.Admin)
        )
        membershipsTable.insertOne(
            Membership(organization = org2._id, user = user2._id, role = MemberRole.Member)
        )
        println("  Created 5 memberships across 2 organizations")

        // Print admin token for easy testing
        val (_, token) = UserAuth.session.createSession(admin._id)
        println()
        println("=== Seed Complete ===")
        println("Admin token: '$token'")
        println("Use this token to authenticate API requests during development.")
    }
}
