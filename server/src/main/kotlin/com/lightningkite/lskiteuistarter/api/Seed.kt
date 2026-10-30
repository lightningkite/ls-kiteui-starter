// Populates a local database with sample data for development.
// Run via: ./gradlew :server:run --args="seed"
// Only works when general.debug = true in settings.json.
package com.lightningkite.lskiteuistarter.api

import com.lightningkite.lightningserver.InternalLightningServerApi
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.fixtures.*
import com.lightningkite.lightningserver.runtime.Execution
import com.lightningkite.lightningserver.runtime.Invocation
import com.lightningkite.lightningserver.runtime.execute
import com.lightningkite.lskiteuistarter.api.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.api.data.OrganizationEndpoints
import com.lightningkite.lskiteuistarter.api.data.UserEndpoints
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.services.data.toEmailAddress
import kotlinx.coroutines.runBlocking
import kotlin.uuid.Uuid

data class SeedConfig(override val seed: Long = 1, override val scale: Double = 1.0) : TestFixtureConfig

object SeedData : ServerBuilder() {
    val fixtures = FixtureRegistry<SeedConfig>()

    // Seed ids must have a zero top half (as randomUuid() makes) so deleteSeedData() can find them.
    val admin = User(
        _id = User.ID(Uuid.fromLongs(0L, 100L)),
        email = "admin@example.com".toEmailAddress(),
        name = "Alice Admin",
        role = UserRole.Admin,
    )

    val users = fixtures.seed(UserEndpoints.info) {
        listOf(admin) + List((config.scale * 20).toInt()) {
            User(_id = User.ID(randomUuid()), email = randomEmailAddress(), name = randomStringFullName())
        }
    }

    val organizations = fixtures.seed(OrganizationEndpoints.info) {
        List((config.scale * 3).toInt()) {
            Organization(_id = Organization.ID(randomUuid()), name = randomStringName().replaceFirstChar { it.uppercase() } + " Corp")
        }
    }

    val memberships = fixtures.seed(MembershipEndpoints.info) {
        val orgs = organizations()
        val ownerships = orgs.map {
            Membership(_id = Membership.ID(randomUuid()), organization = it._id, user = admin._id, role = MemberRole.Owner)
        }
        val members = users().filter { it != admin }.map {
            Membership(
                _id = Membership.ID(randomUuid()),
                organization = orgs.random()._id,
                user = it._id,
                role = listOf(MemberRole.Member, MemberRole.Member, MemberRole.Admin).random(),
            )
        }
        ownerships + members
    }
}

fun seed() = withEngine {
    // Execution.Direct is the framework's sanctioned (but internal) way to run a script outside any request or task.
    @OptIn(InternalLightningServerApi::class)
    runBlocking { execute("seed", Invocation.Unrequested(Execution.Direct(Execution.ID.generate()))) {
        // Fixtures insert fixed ids, so clear the previous run first.  Hand-made rows survive.
        deleteSeedData()
        SeedData.fixtures.execute(SeedConfig())

        val (_, token) = UserAuth.session.createSession(SeedData.admin._id)
        println()
        println("=== Seed Complete ===")
        println("Admin token: '$token'")
        println("Use this token to authenticate API requests during development.")
    } }
}
