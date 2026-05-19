// Populates a local database with sample data for development.
// Run via: ./gradlew :server:serve --args="seed"
// Only works when general.debug = true in settings.json.
package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.definition.generalSettings
import com.lightningkite.lskiteuistarter.data.ClinicEndpoints
import com.lightningkite.lskiteuistarter.data.ClinicMembershipEndpoints
import com.lightningkite.lskiteuistarter.data.PatientEndpoints
import com.lightningkite.lskiteuistarter.data.PharmacyEndpoints
import com.lightningkite.lskiteuistarter.data.PrescriptionEndpoints
import com.lightningkite.lskiteuistarter.data.ProductEndpoints
import com.lightningkite.lskiteuistarter.data.ProductPharmacyMappingEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.insertOne
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

fun seed() = engine {
    if (!generalSettings().debug) {
        println("ERROR: Seed command requires general.debug = true in settings.json")
        return@engine
    }

    runBlocking {
        println("Seeding database with sample data...")

        val usersTable = UserEndpoints.info.table()
        val clinicsTable = ClinicEndpoints.info.table()
        val membershipsTable = ClinicMembershipEndpoints.info.table()
        val pharmaciesTable = PharmacyEndpoints.info.table()
        val productsTable = ProductEndpoints.info.table()
        val mappingsTable = ProductPharmacyMappingEndpoints.info.table()
        val patientsTable = PatientEndpoints.info.table()
        val prescriptionsTable = PrescriptionEndpoints.info.table()

        // Admin user (system-level, no clinic membership)
        val admin = usersTable.insertOne(
            User(
                _id = User.ID(Uuid.fromLongs(0L, 100L)),
                email = "admin@example.com".toEmailAddress(),
                firstName = "Alice",
                lastName = "Admin",
                role = UserRole.Admin,
            )
        ) ?: run {
            println("Admin user already exists, skipping seed.")
            return@runBlocking
        }
        println("  Created admin user: ${admin.email} (${admin._id})")

        // Prescriber user (DEA licensing left null — requires real upload)
        val prescriber = usersTable.insertOne(
            User(
                _id = User.ID(Uuid.fromLongs(0L, 101L)),
                email = "prescriber@example.com".toEmailAddress(),
                firstName = "Pat",
                lastName = "Prescriber",
                role = UserRole.User,
            )
        )!!
        println("  Created prescriber user: ${prescriber.email} (${prescriber._id})")

        // Medical assistant user
        val ma = usersTable.insertOne(
            User(
                _id = User.ID(Uuid.fromLongs(0L, 102L)),
                email = "ma@example.com".toEmailAddress(),
                firstName = "Morgan",
                lastName = "Assistant",
                role = UserRole.User,
            )
        )!!
        println("  Created medical assistant: ${ma.email} (${ma._id})")

        // Clinic
        val clinicAddress = VerifiedAddress(
            address = Address(
                recipient = "Sample Clinic",
                line1 = "100 Main St",
                city = "Knoxville",
                state = "TN",
                zip = "37902",
            ),
            verifiedAt = now(),
            verificationProvider = "seed",
        )
        val clinic = clinicsTable.insertOne(
            Clinic(
                _id = Clinic.ID(Uuid.fromLongs(0L, 200L)),
                name = "Sample Clinic",
                primaryAddress = clinicAddress,
                billingContactEmail = "billing@example.com".toEmailAddress(),
                billingContactName = "Billy Biller",
                stripePaymentId = "pm_seed_placeholder",
                stripePaymentType = PaymentType.Card,
            )
        )!!
        println("  Created clinic: ${clinic.name} (${clinic._id})")

        // Memberships — accepted so they're active
        membershipsTable.insertOne(
            ClinicMembership(
                _id = ClinicMembership.ID(Uuid.fromLongs(0L, 300L)),
                clinic = clinic._id,
                user = prescriber._id,
                role = ClinicRole.Prescriber,
                acceptedAt = now(),
            )
        )
        membershipsTable.insertOne(
            ClinicMembership(
                _id = ClinicMembership.ID(Uuid.fromLongs(0L, 301L)),
                clinic = clinic._id,
                user = ma._id,
                role = ClinicRole.MedicalAssistant,
                acceptedAt = now(),
            )
        )
        println("  Created 2 active clinic memberships (Prescriber, MedicalAssistant)")

        // Pharmacies — one of each adapter type to exercise both adapters
        val lifeFilePharmacy = pharmaciesTable.insertOne(
            Pharmacy(
                _id = Pharmacy.ID(Uuid.fromLongs(0L, 400L)),
                name = "LifeFile Compounding",
                adapterType = PharmacyAdapterType.LifeFile,
                credentialsSecretRef = "secrets/pharmacy/lifefile-sandbox",
                contactEmail = "support@lifefile.example".toEmailAddress(),
                states = setOf(Pharmacy.StateInfo(state = "TN")),
            )
        )!!
        println("  Created pharmacy: ${lifeFilePharmacy.name} (${lifeFilePharmacy._id})")

        val empowerPharmacy = pharmaciesTable.insertOne(
            Pharmacy(
                _id = Pharmacy.ID(Uuid.fromLongs(0L, 401L)),
                name = "Empower Compounding",
                adapterType = PharmacyAdapterType.Empower,
                credentialsSecretRef = "secrets/pharmacy/empower-sandbox",
                contactEmail = "support@empower.example".toEmailAddress(),
                states = setOf(Pharmacy.StateInfo(state = "TN")),
            )
        )!!
        println("  Created pharmacy: ${empowerPharmacy.name} (${empowerPharmacy._id})")

        // Product (with embedded forms) + pharmacy mapping
        val product = productsTable.insertOne(
            Product(
                _id = Product.ID(Uuid.fromLongs(0L, 500L)),
                name = "Semaglutide",
                description = "Sample non-controlled compound for development.",
                forms = setOf(
                    Product.Form(
                        form = Product.FormType.InjectableVial,
                        strengthUnit = "mg/mL",
                        quantityUnit = "mL",
                    )
                ),
                controlled = false,
            )
        )!!
        println("  Created product: ${product.name} with ${product.forms.size} form(s) (${product._id})")

        mappingsTable.insertOne(
            ProductPharmacyMapping(
                _id = ProductPharmacyMapping.ID(Uuid.fromLongs(0L, 700L)),
                pharmacy = lifeFilePharmacy._id,
                product = product._id,
                form = Product.FormType.InjectableVial,
                strength = 2.5,
                quantity = 5.0,
                pharmacySku = "LF-SEMA-2.5MG-5ML",
                price = 12500,
                leadTimeDays = 3,
            )
        )
        println("  Created product-pharmacy mapping for ${product.name} @ ${lifeFilePharmacy.name}")

        // Patient
        val patientAddress = VerifiedAddress(
            address = Address(
                recipient = "Sam Sample",
                line1 = "200 Patient Ln",
                city = "Knoxville",
                state = "TN",
                zip = "37902",
            ),
            verifiedAt = now(),
            verificationProvider = "seed",
        )
        val patient = patientsTable.insertOne(
            Patient(
                _id = Patient.ID(Uuid.fromLongs(0L, 800L)),
                clinic = clinic._id,
                firstName = "Sam",
                lastName = "Sample",
                gender = Gender.U,
                dateOfBirth = LocalDate(1990, 1, 1),
                shippingAddress = patientAddress,
                createdBy = ma._id,
            )
        )!!
        println("  Created patient: ${patient.displayName} (${patient._id})")

        // Draft prescription (no clinician review yet — that's the submission gate)
        val prescription = prescriptionsTable.insertOne(
            Prescription(
                _id = Prescription.ID(Uuid.fromLongs(0L, 900L)),
                clinic = clinic._id,
                patient = patient._id,
                product = product._id,
                prescribedBy = prescriber._id,
                form = Product.FormType.InjectableVial,
                strength = 2.5,
                instructions = "Inject 0.25 mg subcutaneously once weekly.",
            )
        )!!
        println("  Created draft prescription: ${prescription._id}")

        // Print admin token for easy testing
        val (_, token) = UserAuth.session.createSession(admin._id)
        println()
        println("=== Seed Complete ===")
        println("Admin token: '$token'")
        println("Use this token to authenticate API requests during development.")
    }
}
