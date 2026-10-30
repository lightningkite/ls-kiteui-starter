package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.data.OrganizationEndpoints
import com.lightningkite.services.database.insertOne
import kotlin.test.Test

class MembersTest {
    @Test
    fun searchFiltersMembersByName() = integrationTest(
        initialPage = MembersPage(),
        setup = {
            val admin = createAdmin()
            val bob = createUser(email = "bob@example.com", name = "Bob Builder")
            val carol = createUser(email = "carol@example.com", name = "Carol Contractor")
            val org = OrganizationEndpoints.info.table().insertOne(Organization(name = "Acme"))
            for (user in listOf(bob, carol))
                MembershipEndpoints.info.table().insertOne(Membership(organization = org._id, user = user._id))
            loginAs(admin)
        },
    ) {
        waitForText("User: Bob Builder")
        waitForText("User: Carol Contractor")

        setValue(findAll("TextInput").single().path, "carol")
        waitFor(description = "only Carol listed") {
            val page = snapshot()
            "User: Carol Contractor" in page && "User: Bob Builder" !in page
        }
    }
}
