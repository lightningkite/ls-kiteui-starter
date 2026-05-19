// CRUD and permission-boundary tests for the new clinical model set.
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.testAuth
import com.lightningkite.lightningserver.runtime.test.test
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lightningserver.typed.test
import com.lightningkite.lskiteuistarter.data.ClinicEndpoints
import com.lightningkite.lskiteuistarter.data.ClinicMembershipEndpoints
import com.lightningkite.lskiteuistarter.data.PatientEndpoints
import com.lightningkite.lskiteuistarter.data.PrescriptionEndpoints
import com.lightningkite.lskiteuistarter.data.PrescriptionOrderEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.insertOne
import com.lightningkite.services.database.modification
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerTest {

    // ---- Helpers ----------------------------------------------------------

    private fun user(email: String, first: String, last: String = "Last", role: UserRole = UserRole.User) =
        User(email = email.toEmailAddress(), firstName = first, lastName = last, role = role)

    private fun clinic(name: String = "Test Clinic") = Clinic(
        name = name,
        primaryAddress = VerifiedAddress(
            address = Address(
                recipient = name,
                line1 = "1 Test St",
                city = "Knoxville",
                state = "TN",
                zip = "37902",
            ),
        ),
        billingContactEmail = "billing@test.com".toEmailAddress(),
        billingContactName = "Bill Biller",
        stripePaymentId = "pm_test",
        stripePaymentType = PaymentType.Card,
    )

    private fun membership(clinicId: Clinic.ID, userId: User.ID, role: ClinicRole) = ClinicMembership(
        clinic = clinicId,
        user = userId,
        role = role,
        acceptedAt = now(),
    )

    private fun patient(clinicId: Clinic.ID, creator: User.ID, first: String = "Sam") = Patient(
        clinic = clinicId,
        firstName = first,
        lastName = "Sample",
        gender = Gender.U,
        dateOfBirth = LocalDate(1990, 1, 1),
        shippingAddress = VerifiedAddress(
            address = Address(
                recipient = "$first Sample",
                line1 = "200 Patient Ln",
                city = "Knoxville",
                state = "TN",
                zip = "37902",
            ),
        ),
        createdBy = creator,
    )

    // ---- Tests ------------------------------------------------------------

    @Test
    fun clinicCrud(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val admin = UserEndpoints.info.table().insertOne(
                user("admin@test.com", "Admin", role = UserRole.Admin)
            )!!
            val c = ClinicEndpoints.info.table().insertOne(clinic("Acme Health"))!!

            val fetched = ClinicEndpoints.rest.detail.test(c._id, UserAuth.testAuth(admin), Unit)
            assertEquals("Acme Health", fetched.name)
        }
    }

    @Test
    fun clinicMembershipCrud(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val admin = UserEndpoints.info.table().insertOne(
                user("admin@test.com", "Admin", role = UserRole.Admin)
            )!!
            val member = UserEndpoints.info.table().insertOne(user("member@test.com", "Member"))!!
            val c = ClinicEndpoints.info.table().insertOne(clinic())!!

            val m = ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, member._id, ClinicRole.MedicalAssistant)
            )!!

            assertNotNull(m)
            assertEquals(c._id, m.clinic)
            assertEquals(member._id, m.user)
            assertEquals(ClinicRole.MedicalAssistant, m.role)
            assertTrue(m.isActive, "Membership with acceptedAt should be active")

            // Admin can read it via endpoint
            val fetched = ClinicMembershipEndpoints.rest.detail.test(m._id, UserAuth.testAuth(admin), Unit)
            assertEquals(member._id, fetched.user)
        }
    }

    @Test
    fun clinicPermissions(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val admin = UserEndpoints.info.table().insertOne(
                user("admin@test.com", "Admin", role = UserRole.Admin)
            )!!
            val outsider = UserEndpoints.info.table().insertOne(user("out@test.com", "Out"))!!
            val regularMember = UserEndpoints.info.table().insertOne(user("ma@test.com", "MA"))!!
            val clinicAdmin = UserEndpoints.info.table().insertOne(user("cadmin@test.com", "CAdmin"))!!

            val c = ClinicEndpoints.info.table().insertOne(clinic("Private Clinic"))!!
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, regularMember._id, ClinicRole.MedicalAssistant)
            )
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, clinicAdmin._id, ClinicRole.ClinicAdmin)
            )

            val adminAuth = UserAuth.testAuth(admin)
            val outsiderAuth = UserAuth.testAuth(outsider)
            val memberAuth = UserAuth.testAuth(regularMember)
            val clinicAdminAuth = UserAuth.testAuth(clinicAdmin)

            // Non-member can't read
            assertFailsWith<NotFoundException> {
                ClinicEndpoints.rest.detail.test(c._id, outsiderAuth, Unit)
            }

            // Member CAN read
            val fetched = ClinicEndpoints.rest.detail.test(c._id, memberAuth, Unit)
            assertEquals("Private Clinic", fetched.name)

            // Non-clinic-admin member can't update name
            assertFailsWith<NotFoundException> {
                ClinicEndpoints.rest.modify.test(
                    c._id, memberAuth,
                    modification<Clinic> { it.name assign "Renamed By Member" }
                )
            }

            // Clinic admin CAN update name
            val updatedByClinicAdmin = ClinicEndpoints.rest.modify.test(
                c._id, clinicAdminAuth,
                modification<Clinic> { it.name assign "Renamed By Clinic Admin" }
            )
            assertEquals("Renamed By Clinic Admin", updatedByClinicAdmin.name)

            // System admin CAN update name
            val updatedBySystemAdmin = ClinicEndpoints.rest.modify.test(
                c._id, adminAuth,
                modification<Clinic> { it.name assign "Renamed By System Admin" }
            )
            assertEquals("Renamed By System Admin", updatedBySystemAdmin.name)
        }
    }

    @Test
    fun patientPermissions(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val member = UserEndpoints.info.table().insertOne(user("ma@test.com", "MA"))!!
            val clinicAdmin = UserEndpoints.info.table().insertOne(user("cadmin@test.com", "CAdmin"))!!
            val outsider = UserEndpoints.info.table().insertOne(user("out@test.com", "Out"))!!

            val c = ClinicEndpoints.info.table().insertOne(clinic())!!
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, member._id, ClinicRole.MedicalAssistant)
            )
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, clinicAdmin._id, ClinicRole.ClinicAdmin)
            )

            val memberAuth = UserAuth.testAuth(member)
            val clinicAdminAuth = UserAuth.testAuth(clinicAdmin)
            val outsiderAuth = UserAuth.testAuth(outsider)

            // Member can create
            val created = PatientEndpoints.rest.insert.test(memberAuth, patient(c._id, member._id))
            assertEquals(c._id, created.clinic)

            // Member can read
            val fetched = PatientEndpoints.rest.detail.test(created._id, memberAuth, Unit)
            assertEquals(created._id, fetched._id)

            // Member can update (e.g. lastName)
            val updated = PatientEndpoints.rest.modify.test(
                created._id, memberAuth,
                modification<Patient> { it.lastName assign "Updated" }
            )
            assertEquals("Updated", updated.lastName)

            // Outsider cannot read
            assertFailsWith<NotFoundException> {
                PatientEndpoints.rest.detail.test(created._id, outsiderAuth, Unit)
            }

            // Regular member cannot delete (delete restricted to clinic admin)
            assertFailsWith<NotFoundException> {
                PatientEndpoints.rest.deleteItem.test(created._id, memberAuth, Unit)
            }

            // Clinic admin CAN delete
            PatientEndpoints.rest.deleteItem.test(created._id, clinicAdminAuth, Unit)
        }
    }

    @Test
    fun prescriptionOrderSubmissionGate(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val prescriberUser = UserEndpoints.info.table().insertOne(user("rx@test.com", "Rx"))!!
            val maUser = UserEndpoints.info.table().insertOne(user("ma@test.com", "MA"))!!

            val c = ClinicEndpoints.info.table().insertOne(clinic())!!
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, prescriberUser._id, ClinicRole.Prescriber)
            )
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, maUser._id, ClinicRole.MedicalAssistant)
            )

            val pat = PatientEndpoints.info.table().insertOne(patient(c._id, maUser._id))!!

            // Use deterministic placeholder IDs for product/pharmacy — denormalized fields on the
            // order are not validated by foreign-key checks, so this is fine.
            val productId = Product.ID(kotlin.uuid.Uuid.fromLongs(0L, 1000L))
            val pharmacyId = Pharmacy.ID(kotlin.uuid.Uuid.fromLongs(0L, 1002L))

            val rx = PrescriptionEndpoints.info.table().insertOne(
                Prescription(
                    clinic = c._id,
                    patient = pat._id,
                    product = productId,
                    prescribedBy = prescriberUser._id,
                    form = Product.FormType.InjectableVial,
                    strength = 2.5,
                    instructions = "Once weekly.",
                )
            )!!

            val order = PrescriptionOrderEndpoints.info.table().insertOne(
                PrescriptionOrder(
                    prescription = rx._id,
                    pharmacy = pharmacyId,
                    destination = pat.shippingAddress,
                    quantity = 5.0,
                    willLastDays = 35,
                    clinic = c._id,
                    patient = pat._id,
                    product = productId,
                    form = Product.FormType.InjectableVial,
                    strength = 2.5,
                    instructions = "Once weekly.",
                    prescribedBy = prescriberUser._id,
                    createdBy = maUser._id,
                )
            )!!

            val maAuth = UserAuth.testAuth(maUser)
            val prescriberAuth = UserAuth.testAuth(prescriberUser)

            val review = ClinicianReview(
                user = prescriberUser._id,
                idEvent = "idme-event-stub",
                approved = true,
                at = now(),
            )

            // MA CANNOT set clinicianReview — they're not a Prescriber.
            assertFailsWith<NotFoundException> {
                PrescriptionOrderEndpoints.rest.modify.test(
                    order._id, maAuth,
                    modification<PrescriptionOrder> { it.clinicianReview assign review }
                )
            }

            // Prescriber CAN set clinicianReview.
            val signed = PrescriptionOrderEndpoints.rest.modify.test(
                order._id, prescriberAuth,
                modification<PrescriptionOrder> { it.clinicianReview assign review }
            )
            assertNotNull(signed.clinicianReview)
            assertEquals(prescriberUser._id, signed.clinicianReview!!.user)
        }
    }

    @Test
    fun userRoleEscalation(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val regularUser = UserEndpoints.info.table().insertOne(user("u@test.com", "User"))!!
            val userAuth = UserAuth.testAuth(regularUser)

            // User can read their own record
            val self = UserEndpoints.rest.endpoints.detail.test(regularUser._id, userAuth, Unit)
            assertEquals("User", self.firstName)

            // User can modify their own firstName
            val updated = UserEndpoints.rest.endpoints.modify.test(
                regularUser._id, userAuth,
                modification<User> { it.firstName assign "Updated" }
            )
            assertEquals("Updated", updated.firstName)

            // User CANNOT escalate to Admin — role.requires(admin).
            // updateRestrictions narrow the query so the record "disappears" → NotFoundException.
            assertFailsWith<NotFoundException> {
                UserEndpoints.rest.endpoints.modify.test(
                    regularUser._id, userAuth,
                    modification<User> { it.role assign UserRole.Admin }
                )
            }
        }
    }

    @Test
    fun authFlow(): Unit = runBlocking {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val adminUser = UserEndpoints.info.table().insertOne(
                user("admin@test.com", "Admin", role = UserRole.Admin)
            )!!
            val regularUser = UserEndpoints.info.table().insertOne(user("u@test.com", "User"))!!
            val coClinicUser = UserEndpoints.info.table().insertOne(user("co@test.com", "Co"))!!
            val outsider = UserEndpoints.info.table().insertOne(user("out@test.com", "Out"))!!

            val c = ClinicEndpoints.info.table().insertOne(clinic())!!
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, regularUser._id, ClinicRole.MedicalAssistant)
            )
            ClinicMembershipEndpoints.info.table().insertOne(
                membership(c._id, coClinicUser._id, ClinicRole.MedicalAssistant)
            )

            val adminAuth = UserAuth.testAuth(adminUser)
            val userAuth = UserAuth.testAuth(regularUser)

            // Admin sees all users
            val adminResults = UserEndpoints.rest.endpoints.list.test(adminAuth, Query<User>())
            assertTrue(adminResults.any { it._id == adminUser._id })
            assertTrue(adminResults.any { it._id == regularUser._id })
            assertTrue(adminResults.any { it._id == coClinicUser._id })
            assertTrue(adminResults.any { it._id == outsider._id })

            // Regular user sees themselves and co-clinic members but NOT the outsider
            val userResults = UserEndpoints.rest.endpoints.list.test(userAuth, Query<User>())
            val ids = userResults.map { it._id }.toSet()
            assertTrue(ids.contains(regularUser._id), "Regular user should see themselves")
            assertTrue(ids.contains(coClinicUser._id), "Regular user should see co-clinic member")
            assertTrue(!ids.contains(outsider._id), "Regular user should NOT see outsider")
        }
    }
}
