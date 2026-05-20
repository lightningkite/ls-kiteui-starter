// Populates a local database with sample data for development.
// Run via: ./gradlew :server:serve --args="seed"
// Only works when general.debug = true in settings.json.
//
// Idempotent: re-running prints the admin token even if data is already present.
package com.heroscript

import com.lightningkite.lightningserver.definition.generalSettings
import com.heroscript.data.ClinicEndpoints
import com.heroscript.data.ClinicInvoiceEndpoints
import com.heroscript.data.ClinicMembershipEndpoints
import com.heroscript.data.PatientEndpoints
import com.heroscript.data.PharmacyEndpoints
import com.heroscript.data.PharmacyOrderEndpoints
import com.heroscript.data.PrescriptionEndpoints
import com.heroscript.data.PrescriptionOrderEndpoints
import com.heroscript.data.ProductEndpoints
import com.heroscript.data.ProductPharmacyMappingEndpoints
import com.heroscript.data.ShipmentEndpoints
import com.heroscript.data.UserEndpoints
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.get
import com.lightningkite.services.database.insertOne
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.days
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
        val prescriptionOrdersTable = PrescriptionOrderEndpoints.info.table()
        val pharmacyOrdersTable = PharmacyOrderEndpoints.info.table()
        val shipmentsTable = ShipmentEndpoints.info.table()
        val invoicesTable = ClinicInvoiceEndpoints.info.table()

        // -------- Users --------

        val adminId = User.ID(Uuid.fromLongs(0L, 100L))
        val admin = usersTable.get(adminId) ?: usersTable.insertOne(
            User(
                _id = adminId,
                email = "admin@example.com".toEmailAddress(),
                firstName = "Alice",
                lastName = "Admin",
                role = UserRole.Admin,
            )
        )!!.also { println("  Created admin user: ${it.email}") }

        val prescriberId = User.ID(Uuid.fromLongs(0L, 101L))
        val prescriber = usersTable.get(prescriberId) ?: usersTable.insertOne(
            User(
                _id = prescriberId,
                email = "prescriber@example.com".toEmailAddress(),
                firstName = "Pat",
                lastName = "Prescriber",
                role = UserRole.User,
                prescriber = PrescriberLicensing(
                    deaNumber = "BP1234563",
                    deaLicenseImage = com.lightningkite.services.files.ServerFile("placeholder.pdf"),
                    deaExpiration = now() + 365.days,
                    deaReview = PrescriberLicensing.Review(
                        byUser = adminId,
                        approved = true,
                        notes = "Seed data — auto-verified for development.",
                    ),
                    stateLicenses = setOf(
                        PrescriberLicensing.StateMedicalLicense(
                            state = "TN",
                            licenseNumber = "TN-PRES-001",
                            expiration = now() + 365.days,
                            review = PrescriberLicensing.Review(
                                byUser = adminId,
                                approved = true,
                                notes = "Seed.",
                            ),
                        ),
                    ),
                    idMeSubjectId = "seed-idme-001",
                    idMeLinkedAt = now() - 30.days,
                ),
            )
        )!!.also { println("  Created prescriber: ${it.email}") }

        val maId = User.ID(Uuid.fromLongs(0L, 102L))
        val ma = usersTable.get(maId) ?: usersTable.insertOne(
            User(
                _id = maId,
                email = "ma@example.com".toEmailAddress(),
                firstName = "Morgan",
                lastName = "Assistant",
                role = UserRole.User,
            )
        )!!.also { println("  Created medical assistant: ${it.email}") }

        val clinicAdminId = User.ID(Uuid.fromLongs(0L, 103L))
        val clinicAdmin = usersTable.get(clinicAdminId) ?: usersTable.insertOne(
            User(
                _id = clinicAdminId,
                email = "clinicadmin@example.com".toEmailAddress(),
                firstName = "Carla",
                lastName = "ClinicAdmin",
                role = UserRole.User,
            )
        )!!.also { println("  Created clinic admin: ${it.email}") }

        // -------- Clinic --------

        val clinicId = Clinic.ID(Uuid.fromLongs(0L, 200L))
        val clinicAddress = VerifiedAddress(
            address = Address(
                recipient = "Gameday Knoxville Clinic",
                line1 = "100 Main St",
                city = "Knoxville",
                state = "TN",
                zip = "37902",
            ),
            verifiedAt = now(),
            verificationProvider = "seed",
        )
        val clinic = clinicsTable.get(clinicId) ?: clinicsTable.insertOne(
            Clinic(
                _id = clinicId,
                name = "Gameday Knoxville",
                primaryAddress = clinicAddress,
                billingContactEmail = "billing@example.com".toEmailAddress(),
                billingContactName = "Billy Biller",
                stripePaymentId = "pm_seed_placeholder",
                stripePaymentType = PaymentType.Card,
            )
        )!!.also { println("  Created clinic: ${it.name}") }

        // -------- Memberships --------

        suspend fun ensureMembership(id: ClinicMembership.ID, user: User.ID, role: ClinicRole) {
            if (membershipsTable.get(id) == null) {
                membershipsTable.insertOne(
                    ClinicMembership(
                        _id = id,
                        clinic = clinic._id,
                        user = user,
                        role = role,
                        acceptedAt = now(),
                    )
                )
            }
        }
        ensureMembership(ClinicMembership.ID(Uuid.fromLongs(0L, 300L)), prescriber._id, ClinicRole.Prescriber)
        ensureMembership(ClinicMembership.ID(Uuid.fromLongs(0L, 301L)), ma._id, ClinicRole.MedicalAssistant)
        ensureMembership(ClinicMembership.ID(Uuid.fromLongs(0L, 302L)), clinicAdmin._id, ClinicRole.ClinicAdmin)
        println("  Ensured 3 clinic memberships")

        // -------- Pharmacies --------

        val lifeFileId = Pharmacy.ID(Uuid.fromLongs(0L, 400L))
        val lifeFilePharmacy = pharmaciesTable.get(lifeFileId) ?: pharmaciesTable.insertOne(
            Pharmacy(
                _id = lifeFileId,
                name = "LifeFile Compounding",
                adapterType = PharmacyAdapterType.LifeFile,
                credentialsSecretRef = "secrets/pharmacy/lifefile-sandbox",
                contactEmail = "support@lifefile.example".toEmailAddress(),
                states = setOf(Pharmacy.StateInfo(state = "TN"), Pharmacy.StateInfo(state = "KY")),
            )
        )!!.also { println("  Created pharmacy: ${it.name}") }

        val empowerId = Pharmacy.ID(Uuid.fromLongs(0L, 401L))
        val empowerPharmacy = pharmaciesTable.get(empowerId) ?: pharmaciesTable.insertOne(
            Pharmacy(
                _id = empowerId,
                name = "Empower Compounding",
                adapterType = PharmacyAdapterType.Empower,
                credentialsSecretRef = "secrets/pharmacy/empower-sandbox",
                contactEmail = "support@empower.example".toEmailAddress(),
                states = setOf(Pharmacy.StateInfo(state = "TN")),
            )
        )!!.also { println("  Created pharmacy: ${it.name}") }

        // -------- Products + mappings --------

        val semaId = Product.ID(Uuid.fromLongs(0L, 500L))
        val semaglutide = productsTable.get(semaId) ?: productsTable.insertOne(
            Product(
                _id = semaId,
                name = "Semaglutide",
                description = "GLP-1 agonist for weight management.",
                forms = setOf(
                    Product.Form(
                        form = Product.FormType.InjectableVial,
                        strengthUnit = "mg/mL",
                        quantityUnit = "mL",
                    )
                ),
                controlled = false,
            )
        )!!.also { println("  Created product: ${it.name}") }

        val testoId = Product.ID(Uuid.fromLongs(0L, 501L))
        val testosterone = productsTable.get(testoId) ?: productsTable.insertOne(
            Product(
                _id = testoId,
                name = "Testosterone Cypionate",
                description = "Controlled-substance TRT injectable.",
                forms = setOf(
                    Product.Form(
                        form = Product.FormType.InjectableVial,
                        strengthUnit = "mg/mL",
                        quantityUnit = "mL",
                    )
                ),
                controlled = true,
            )
        )!!.also { println("  Created product: ${it.name}") }

        val mapping1Id = ProductPharmacyMapping.ID(Uuid.fromLongs(0L, 700L))
        if (mappingsTable.get(mapping1Id) == null) {
            mappingsTable.insertOne(
                ProductPharmacyMapping(
                    _id = mapping1Id,
                    pharmacy = lifeFilePharmacy._id,
                    product = semaglutide._id,
                    form = Product.FormType.InjectableVial,
                    strength = 2.5,
                    quantity = 5.0,
                    pharmacySku = "LF-SEMA-2.5MG-5ML",
                    price = 12500,
                    leadTimeDays = 3,
                )
            )
        }
        val mapping2Id = ProductPharmacyMapping.ID(Uuid.fromLongs(0L, 701L))
        if (mappingsTable.get(mapping2Id) == null) {
            mappingsTable.insertOne(
                ProductPharmacyMapping(
                    _id = mapping2Id,
                    pharmacy = empowerPharmacy._id,
                    product = semaglutide._id,
                    form = Product.FormType.InjectableVial,
                    strength = 2.5,
                    quantity = 5.0,
                    pharmacySku = "EMP-SEMA-2.5-5",
                    price = 11900,
                    leadTimeDays = 4,
                )
            )
        }
        val mapping3Id = ProductPharmacyMapping.ID(Uuid.fromLongs(0L, 702L))
        if (mappingsTable.get(mapping3Id) == null) {
            mappingsTable.insertOne(
                ProductPharmacyMapping(
                    _id = mapping3Id,
                    pharmacy = lifeFilePharmacy._id,
                    product = testosterone._id,
                    form = Product.FormType.InjectableVial,
                    strength = 200.0,
                    quantity = 10.0,
                    pharmacySku = "LF-TEST-200-10",
                    price = 8800,
                    leadTimeDays = 3,
                )
            )
        }
        println("  Ensured 3 product-pharmacy mappings")

        // -------- Patients --------

        val patient1Id = Patient.ID(Uuid.fromLongs(0L, 800L))
        val patient1 = patientsTable.get(patient1Id) ?: patientsTable.insertOne(
            Patient(
                _id = patient1Id,
                clinic = clinic._id,
                firstName = "Sam",
                lastName = "Sample",
                gender = Gender.M,
                dateOfBirth = LocalDate(1985, 4, 12),
                shippingAddress = VerifiedAddress(
                    address = Address(
                        recipient = "Sam Sample",
                        line1 = "200 Patient Ln",
                        city = "Knoxville",
                        state = "TN",
                        zip = "37902",
                    ),
                    verifiedAt = now(),
                    verificationProvider = "seed",
                ),
                smsConsent = now() - 30.days,
                emailConsent = now() - 30.days,
                createdBy = ma._id,
            )
        )!!.also { println("  Created patient: ${it.displayName}") }

        val patient2Id = Patient.ID(Uuid.fromLongs(0L, 801L))
        val patient2 = patientsTable.get(patient2Id) ?: patientsTable.insertOne(
            Patient(
                _id = patient2Id,
                clinic = clinic._id,
                firstName = "Jordan",
                lastName = "Jones",
                gender = Gender.F,
                dateOfBirth = LocalDate(1978, 9, 3),
                shippingAddress = VerifiedAddress(
                    address = Address(
                        recipient = "Jordan Jones",
                        line1 = "55 Oak Ave",
                        city = "Knoxville",
                        state = "TN",
                        zip = "37901",
                    ),
                    verifiedAt = now(),
                    verificationProvider = "seed",
                ),
                smsConsent = now() - 10.days,
                createdBy = ma._id,
            )
        )!!.also { println("  Created patient: ${it.displayName}") }

        // -------- Prescriptions + Orders --------

        val rx1Id = Prescription.ID(Uuid.fromLongs(0L, 900L))
        val rx1 = prescriptionsTable.get(rx1Id) ?: prescriptionsTable.insertOne(
            Prescription(
                _id = rx1Id,
                clinic = clinic._id,
                patient = patient1._id,
                product = semaglutide._id,
                prescribedBy = prescriber._id,
                form = Product.FormType.InjectableVial,
                strength = 2.5,
                instructions = "Inject 0.25 mg subcutaneously once weekly for 4 weeks, then increase to 0.5 mg weekly.",
            )
        )!!.also { println("  Created prescription: Semaglutide for ${patient1.displayName}") }

        val rx2Id = Prescription.ID(Uuid.fromLongs(0L, 901L))
        val rx2 = prescriptionsTable.get(rx2Id) ?: prescriptionsTable.insertOne(
            Prescription(
                _id = rx2Id,
                clinic = clinic._id,
                patient = patient2._id,
                product = testosterone._id,
                prescribedBy = prescriber._id,
                form = Product.FormType.InjectableVial,
                strength = 200.0,
                instructions = "Inject 100 mg (0.5 mL) intramuscularly every 7 days.",
            )
        )!!.also { println("  Created prescription: Testosterone for ${patient2.displayName}") }

        // A submitted+shipped order so OrderDetail has something to render
        val shippedShipmentId = Shipment.ID(Uuid.fromLongs(0L, 950L))
        if (shipmentsTable.get(shippedShipmentId) == null) {
            shipmentsTable.insertOne(
                Shipment(
                    _id = shippedShipmentId,
                    carrier = "UPS",
                    trackingNumber = "1Z999AA10123456784",
                    shippingUrl = "https://www.ups.com/track?tracknum=1Z999AA10123456784",
                    shippedAt = now() - 5.days,
                )
            )
        }

        val pharmacyOrder1Id = PharmacyOrder.ID(Uuid.fromLongs(0L, 960L))
        if (pharmacyOrdersTable.get(pharmacyOrder1Id) == null) {
            pharmacyOrdersTable.insertOne(
                PharmacyOrder(
                    _id = pharmacyOrder1Id,
                    clinic = clinic._id,
                    pharmacy = lifeFilePharmacy._id,
                    destination = patient1.shippingAddress.address,
                    destinationIsClinic = false,
                    accepted = PharmacyAcceptBundle(
                        at = now() - 6.days,
                        externalId = "LF-ORDER-1001",
                        price = 12500,
                        shipping = 1000,
                        tax = 800,
                        total = 14300,
                    ),
                )
            )
        }

        val order1Id = PrescriptionOrder.ID(Uuid.fromLongs(0L, 970L))
        if (prescriptionOrdersTable.get(order1Id) == null) {
            prescriptionOrdersTable.insertOne(
                PrescriptionOrder(
                    _id = order1Id,
                    prescription = rx1._id,
                    pharmacy = lifeFilePharmacy._id,
                    destination = patient1.shippingAddress,
                    quantity = 5.0,
                    willLastDays = 28,
                    clinic = clinic._id,
                    patient = patient1._id,
                    product = semaglutide._id,
                    form = Product.FormType.InjectableVial,
                    strength = 2.5,
                    instructions = rx1.instructions,
                    prescribedBy = prescriber._id,
                    createdBy = ma._id,
                    assignedTo = prescriber._id,
                    consentAffirmedAt = now() - 7.days,
                    createdAt = now() - 7.days,
                    clinicianReview = ClinicianReview(
                        user = prescriber._id,
                        idEvent = "idme-event-seed-001",
                        approved = true,
                        at = now() - 7.days,
                    ),
                    fulfilled = PrescriptionOrder.Fulfillment(
                        at = now() - 6.days,
                        by = pharmacyOrder1Id,
                        accept = PharmacyAcceptLine(
                            at = now() - 6.days,
                            externalId = "LF-LINE-1001-A",
                            price = 12500,
                            total = 14300,
                        ),
                    ),
                    shipment = shippedShipmentId,
                )
            )
        }

        // A draft order assigned to the prescriber (no clinicianReview yet)
        val order2Id = PrescriptionOrder.ID(Uuid.fromLongs(0L, 971L))
        if (prescriptionOrdersTable.get(order2Id) == null) {
            prescriptionOrdersTable.insertOne(
                PrescriptionOrder(
                    _id = order2Id,
                    prescription = rx2._id,
                    pharmacy = lifeFilePharmacy._id,
                    destination = patient2.shippingAddress,
                    quantity = 10.0,
                    willLastDays = 28,
                    clinic = clinic._id,
                    patient = patient2._id,
                    product = testosterone._id,
                    form = Product.FormType.InjectableVial,
                    strength = 200.0,
                    instructions = rx2.instructions,
                    prescribedBy = prescriber._id,
                    createdBy = ma._id,
                    assignedTo = prescriber._id,
                    createdAt = now() - 1.days,
                )
            )
        }

        // An older order that's near refill-due (for Refill Queue)
        val order3Id = PrescriptionOrder.ID(Uuid.fromLongs(0L, 972L))
        if (prescriptionOrdersTable.get(order3Id) == null) {
            prescriptionOrdersTable.insertOne(
                PrescriptionOrder(
                    _id = order3Id,
                    prescription = rx1._id,
                    pharmacy = lifeFilePharmacy._id,
                    destination = patient1.shippingAddress,
                    quantity = 5.0,
                    willLastDays = 28,
                    clinic = clinic._id,
                    patient = patient1._id,
                    product = semaglutide._id,
                    form = Product.FormType.InjectableVial,
                    strength = 2.5,
                    instructions = rx1.instructions,
                    prescribedBy = prescriber._id,
                    createdBy = ma._id,
                    consentAffirmedAt = now() - 27.days,
                    createdAt = now() - 27.days,
                    clinicianReview = ClinicianReview(
                        user = prescriber._id,
                        idEvent = "idme-event-seed-old",
                        approved = true,
                        at = now() - 27.days,
                    ),
                )
            )
        }

        // -------- Invoice --------

        val invoiceId = ClinicInvoice.ID(Uuid.fromLongs(0L, 980L))
        if (invoicesTable.get(invoiceId) == null) {
            invoicesTable.insertOne(
                ClinicInvoice(
                    _id = invoiceId,
                    clinic = clinic._id,
                    startPeriod = now() - 30.days,
                    endPeriod = now() - 1.days,
                    stripeId = "in_seed_placeholder",
                    total = 14300,
                )
            )
        }

        // -------- Always print admin token for testing --------

        val (_, adminToken) = UserAuth.session.createSession(admin._id)
        val (_, prescriberToken) = UserAuth.session.createSession(prescriber._id)
        val (_, maToken) = UserAuth.session.createSession(ma._id)
        val (_, clinicAdminToken) = UserAuth.session.createSession(clinicAdmin._id)
        println()
        println("=== Seed Complete ===")
        println("Admin token: '$adminToken'")
        println("Prescriber token: '$prescriberToken'")
        println("MA token: '$maToken'")
        println("ClinicAdmin token: '$clinicAdminToken'")
        println()
        println("Test users:")
        println("  ${admin.email}         (Ops Admin)")
        println("  ${prescriber.email}    (Prescriber)")
        println("  ${ma.email}            (Medical Assistant)")
        println("  ${clinicAdmin.email}   (Clinic Admin)")
    }
}
