package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.media.ServerFileWithMetadata
import com.lightningkite.services.data.*
import com.lightningkite.services.database.HasId
import com.lightningkite.services.database.TypedId
import com.lightningkite.services.files.ServerFile
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/* -------------------------------------------------------------------------- */
/*  Type aliases                                                              */
/* -------------------------------------------------------------------------- */

typealias Cents = Int
typealias UsState = String        // 2-letter postal code
typealias DeaNumber = String

/* -------------------------------------------------------------------------- */
/*  App release (mobile/web version gating)                                   */
/* -------------------------------------------------------------------------- */

@GenerateDataClassPaths
@Serializable
data class AppRelease(
    override val _id: ID = ID(Uuid.random()),
    val version: String,
    val platform: AppPlatform,
    val releaseDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val requiredUpdate: Boolean,
) : HasId<AppRelease.ID> {
    @Serializable
    @JvmInline
    @References(AppRelease::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

/* -------------------------------------------------------------------------- */
/*  Address + verification (embedded value objects)                           */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class Address(
    val recipient: String,
    val line1: String,
    val line2: String? = null,
    val city: String,
    val state: UsState,
    val zip: String,
    val country: String = "US",
) {
    companion object {
        val EMPTY = Address(recipient = "", line1 = "", city = "", state = "", zip = "")
    }
}

/** USPS / Smarty / Lob verification result, embedded on Patient and snapshotted on Order.shipTo. */
@Serializable
@GenerateDataClassPaths
data class VerifiedAddress(
    val address: Address,
    val verifiedAt: Instant? = null,
    val verificationProvider: String? = null,
)

/* -------------------------------------------------------------------------- */
/*  Clinic, memberships, billing                                              */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class Clinic(
    override val _id: ID = ID(Uuid.random()),
    val name: String,
    val logo: ServerFileWithMetadata? = null,
    val primaryAddress: VerifiedAddress,
    val additionalShippingAddresses: List<VerifiedAddress> = emptyList(),
    @Index val billingContactEmail: EmailAddress,
    val billingContactName: String,
    val stripePaymentId: String,
    val stripePaymentType: PaymentType,
    val createdAt: Instant = now(),
    val deactivatedAt: Instant? = null,
) : HasId<Clinic.ID> {
    val isActive: Boolean get() = deactivatedAt == null

    @Serializable
    @JvmInline
    @References(Clinic::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
enum class PaymentType { Card, ACH }

@Serializable
enum class ClinicRole {
    /** Provisioned by HeroScript Ops; one per clinic. Manages users, addresses, billing. */
    ClinicAdmin,
    /** DEA-licensed clinician. Submits orders (ID.me step-up). */
    Prescriber,
    /** Drafts orders on behalf of a Prescriber. Cannot submit. */
    MedicalAssistant,
}

/**
 * User ↔ Clinic many-to-many. A Prescriber can hold memberships in multiple clinics
 * simultaneously and selects active clinic context at order time.
 */
@Serializable
@GenerateDataClassPaths
data class ClinicMembership(
    override val _id: ID = ID(Uuid.random()),
    @Index val clinic: Clinic.ID,
    @Index val user: User.ID,
    val role: ClinicRole,
    val invitedAt: Instant = now(),
    val invitedBy: User.ID? = null,
    val acceptedAt: Instant? = null,
    val deactivatedAt: Instant? = null,
) : HasId<ClinicMembership.ID> {
    val isActive: Boolean get() = acceptedAt != null && deactivatedAt == null

    @Serializable
    @JvmInline
    @References(ClinicMembership::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

/* -------------------------------------------------------------------------- */
/*  User + embedded prescriber licensing                                      */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class User(
    override val _id: ID = ID(Uuid.random()),
    @Index(IndexUniqueness.Unique) val email: EmailAddress,
    val firstName: String,
    val lastName: String,
    val phoneNumber: PhoneNumber? = null,
    /** 1:1 with a User who holds Prescriber membership in any clinic. Persists across clinics. */
    val prescriber: PrescriberLicensing? = null,
    val role: UserRole = UserRole.User,
    val mfaEnrolledAt: Instant? = null,
    val lastLoginAt: Instant? = null,
    val createdAt: Instant = now(),
    val updatedAt: Instant = now(),
    val deactivatedAt: Instant? = null,
) : HasId<User.ID> {
    val displayName: String get() = "$firstName $lastName"
    val isActive: Boolean get() = deactivatedAt == null

    @Serializable
    @JvmInline
    @References(User::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
enum class UserRole {
    User,
    Admin,
    Developer,
    Root
}

@Serializable
@GenerateDataClassPaths
data class PrescriberLicensing(
    val deaNumber: DeaNumber,
    @MimeType("image/*,application/pdf", maxSize = 10_000_000)
    val deaLicenseImage: ServerFile,
    val deaExpiration: Instant,
    val deaReview: Review? = null,

    val stateLicenses: Set<StateMedicalLicense> = setOf(),

    val idMeSubjectId: String? = null,
    val idMeLinkedAt: Instant? = null,
) {
    val isDeaVerified: Boolean get() = deaReview?.approved == true
    val isDeaExpired: Boolean get() = deaExpiration < now()
    val canSubmitControlledSubstance: Boolean get() = isDeaVerified && !isDeaExpired

    @Serializable
    @GenerateDataClassPaths
    data class Review(
        val byUser: User.ID? = null,
        val bySystem: String? = null, // future auto-verification
        val approved: Boolean,
        val notes: String,
        val at: Instant = now(),
    )

    @Serializable
    @GenerateDataClassPaths
    data class StateMedicalLicense(
        val state: UsState,
        val licenseNumber: String,
        val expiration: Instant,
        val review: Review? = null,
    )
}

/* -------------------------------------------------------------------------- */
/*  FCM push tokens                                                           */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class FcmToken(
    @MaxLength(160, average = 142) override val _id: ID,
    @Index val user: User.ID,
    val active: Boolean = true,
    val created: Instant = now(),
    val lastRegisteredAt: Instant = created,
    val userAgent: String? = null,
) : HasId<FcmToken.ID> {
    @Serializable
    @JvmInline
    @References(FcmToken::class)
    value class ID(override val raw: String) : TypedId<String, ID> {
        override fun toString(): String = raw
    }
}

/* -------------------------------------------------------------------------- */
/*  Patient + clinical facts                                                  */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class Patient(
    override val _id: ID = ID(Uuid.random()),
    @Index val clinic: Clinic.ID,
    val firstName: String,
    val lastName: String,
    val gender: Gender,
    val dateOfBirth: LocalDate,
    val phoneNumber: PhoneNumber? = null,
    val email: EmailAddress? = null,
    val shippingAddress: VerifiedAddress,

    /** Affirmed at intake; reaffirmed at order entry per PRD § 03 assumptions. */
    val smsConsent: Instant? = null,
    val emailConsent: Instant? = null,

    val createdBy: User.ID,
    val createdAt: Instant = now(),
    val updatedAt: Instant = now(),

    /** null = unasked at intake; empty list = asked and patient reported none. */
    val allergies: List<ClinicalEntry>? = null,
    /** null = unasked at intake; empty list = asked and patient reported none. */
    val diseases: List<ClinicalEntry>? = null,
    /** null = unasked at intake; empty list = asked and patient reported none. */
    val otherMedications: List<ClinicalEntry>? = null,

    /** Most recent submitted-order time, denormalized for the refill queue. */
    @Denormalized val lastOrderAt: Instant? = null,
) : HasId<Patient.ID> {
    val displayName: String get() = "$firstName $lastName"

    @Serializable
    @JvmInline
    @References(Patient::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
enum class Gender { M, F, A, U }

/**
 * Structured clinical fact attached to a [Patient].
 *
 * Shape mirrors LifeFile's `clinical[]` entries so an order submission can pass these through
 * with minimal translation. The container field on [Patient] (`allergies` / `diseases` /
 * `otherMedications`) determines the LifeFile `type`, so it isn't stored here.
 *
 * V1 intake is manual, so [code] is optional — [description] is what clinic staff actually
 * type and is the source of truth. The coding system is implied by the container field
 * (RxNorm for medications/allergies, SNOMED-CT or ICD-10 for diseases) per LifeFile convention.
 */
@Serializable
@GenerateDataClassPaths
data class ClinicalEntry(
    val description: String,
    val code: String? = null,
    val source: ClinicalSource? = null,
    /** Allergy entries only: observed reaction. */
    val reaction: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

/** Who reported the clinical fact. Values map directly to LifeFile's `source` enum. */
@Serializable
enum class ClinicalSource {
    Doctor,
    Patient,
    PatientAgent,
    Pharmacist,
}

/* -------------------------------------------------------------------------- */
/*  Pharmacy                                                                  */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class Pharmacy(
    override val _id: ID = ID(Uuid.random()),
    val name: String,
    val adapterType: PharmacyAdapterType,
    /** Pointer into AWS Secrets Manager; the secret value never lives in the DB. */
    val credentialsSecretRef: String,
    val contactEmail: EmailAddress,
    val contactPhone: PhoneNumber? = null,
    val createdAt: Instant = now(),
    val deactivatedAt: Instant? = null,
    /**
     * States the pharmacy is licensed to ship *to*. Pharmacy eligibility for an order
     * is filtered against the order's ship-to destination state
     * (`PrescriptionOrder.destination.address.state`), NOT the patient's residence —
     * these can differ when an order ships to a clinic in a different state than where
     * the patient lives.
     */
    val states: Set<StateInfo> = setOf(),
) : HasId<Pharmacy.ID> {
    val isActive: Boolean get() = deactivatedAt == null

    @Serializable
    @GenerateDataClassPaths
    data class StateInfo(
        val state: UsState,
        val effectiveDate: LocalDate? = null,
        val expirationDate: LocalDate? = null,
        val notes: String? = null,
    )

    @Serializable
    @JvmInline
    @References(Pharmacy::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
enum class PharmacyAdapterType {
    LifeFile,
    Empower,
    Proprietary,
}

/* -------------------------------------------------------------------------- */
/*  Product catalog (product + form + pharmacy mapping)                       */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
@TextIndex(["name", "description"])
data class Product(
    override val _id: ID = ID(Uuid.random()),
    val name: String,
    val description: String = "",
    val forms: Set<Form> = setOf(), // Only one entry per form type
    /** Controlled-substance flag triggers ID.me step-up and DEA validation at submission. */
    val controlled: Boolean = false,
    val active: Boolean = true,
    val createdAt: Instant = now(),
) : HasId<Product.ID> {
    @Serializable
    @JvmInline
    @References(Product::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }

    @Serializable
    enum class FormType {
        InjectableVial,
        InjectableSyringe,
        OralTablet,
        OralCapsule,
        OralSolution,
        TopicalCream,
        TopicalGel,
        Troche,
        Other,
    }

    @Serializable
    @GenerateDataClassPaths
    data class Form(
        val form: FormType,
        val strengthUnit: String,
        val quantityUnit: String,
    )
}

@Serializable
@GenerateDataClassPaths
data class ProductPharmacyMapping(
    override val _id: ID = ID(Uuid.random()),
    @Index val pharmacy: Pharmacy.ID,
    @Index val product: Product.ID,
    val form: Product.FormType,
    val strength: Double? = null,  // null indicates customizable
    val quantity: Double? = null,  // null indicates customizable
    val pharmacySku: String,
    val price: Cents,
    val tax: Cents = 0,
    val shippingFee: Cents = 0,
    @Denormalized val total: Cents = price,
    val leadTimeDays: Int,
    val active: Boolean = true,
    val updatedAt: Instant = now(),
) : HasId<ProductPharmacyMapping.ID> {
    @Serializable
    @JvmInline
    @References(ProductPharmacyMapping::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

/* -------------------------------------------------------------------------- */
/*  Prescriptions and orders                                                  */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class Prescription(
    override val _id: ID = ID(Uuid.random()),
    @Index val clinic: Clinic.ID,
    @Index val patient: Patient.ID,
    @Index val product: Product.ID,
    @Index val prescribedBy: User.ID,
    val form: Product.FormType,
    val strength: Double,
    val instructions: String,
    val createdAt: Instant = now(),
    val endsAt: Instant? = null,
) : HasId<Prescription.ID> {
    @Serializable
    @JvmInline
    @References(Prescription::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
@GenerateDataClassPaths
data class PrescriptionOrder(
    override val _id: ID = ID(Uuid.random()),
    @Index val prescription: Prescription.ID,
    @Index val pharmacy: Pharmacy.ID,
    val destination: VerifiedAddress,
    val quantity: Double,
    val willLastDays: Int,

    @Denormalized @Index val clinic: Clinic.ID,
    @Denormalized @Index val patient: Patient.ID,
    @Denormalized @Index val product: Product.ID,
    @Denormalized val form: Product.FormType,
    @Denormalized val strength: Double,
    @Denormalized val instructions: String,
    @Denormalized val prescribedBy: User.ID,

    @Index val createdBy: User.ID,
    @Index val assignedTo: User.ID? = null,
    val consentAffirmedAt: Instant? = null,
    val createdAt: Instant = now(),
    val cancellation: Cancellation? = null,
    val clinicianReview: ClinicianReview? = null,
    val fulfilled: Fulfillment? = null,
    @Index val shipment: Shipment.ID? = null,
) : HasId<PrescriptionOrder.ID> {
    @Serializable
    @GenerateDataClassPaths
    data class Fulfillment(
        val at: Instant = now(),
        val by: PharmacyOrder.ID,
        val reject: PharmacyReject? = null,
        val accept: PharmacyAcceptLine? = null,
    )

    @Serializable
    @GenerateDataClassPaths
    data class Cancellation(
        val at: Instant = now(),
        val by: User.ID,
        val reason: String,
    )

    @Serializable
    @JvmInline
    @References(PrescriptionOrder::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
@GenerateDataClassPaths
data class ClinicianReview(
    val user: User.ID,
    val idEvent: String,
    val approved: Boolean,
    val at: Instant,
)

@Serializable
@GenerateDataClassPaths
data class PharmacyOrder(
    override val _id: ID = ID(Uuid.random()),
    @Index val clinic: Clinic.ID,
    @Index val invoice: ClinicInvoice.ID? = null,
    @Index val pharmacy: Pharmacy.ID,
    val createdAt: Instant = now(),
    val destination: Address,
    val destinationIsClinic: Boolean,
    val accepted: PharmacyAcceptBundle? = null,
    @Denormalized val totalRejection: PharmacyReject? = null,
) : HasId<PharmacyOrder.ID> {
    @Serializable
    @JvmInline
    @References(PharmacyOrder::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
@GenerateDataClassPaths
data class Shipment(
    override val _id: ID = ID(Uuid.random()),
    val carrier: String,
    val trackingNumber: String,
    val shippingUrl: String? = null, // contains both shipper and shipping identifier
    @Denormalized val shippedAt: Instant? = null,
    @Denormalized val deliveredAt: Instant? = null,
    @Denormalized val cancelledAt: Instant? = null,
): HasId<Shipment.ID> {
    @Serializable
    @JvmInline
    @References(Shipment::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

@Serializable
@GenerateDataClassPaths
data class PharmacyAcceptBundle(
    val at: Instant = now(),
    val externalId: String,
    val price: Cents,
    val shipping: Cents,
    val tax: Cents,
    val total: Cents,
)

@Serializable
@GenerateDataClassPaths
data class PharmacyAcceptLine(
    val at: Instant = now(),
    val externalId: String,
    val price: Cents? = null,
    val shipping: Cents? = null,
    val tax: Cents? = null,
    val total: Cents? = null,
)

@Serializable
@GenerateDataClassPaths
data class PharmacyReject(
    val at: Instant = now(),
    val externalId: String? = null,
    val reason: String,
)

/* -------------------------------------------------------------------------- */
/*  Billing                                                                   */
/* -------------------------------------------------------------------------- */

@Serializable
@GenerateDataClassPaths
data class ClinicInvoice(
    override val _id: ID = ID(Uuid.random()),
    @Index val clinic: Clinic.ID,
    val createdAt: Instant = now(),
    val startPeriod: Instant,
    val endPeriod: Instant,
    val paidAt: Instant? = null,
    val stripeId: String,
    val total: Cents,
) : HasId<ClinicInvoice.ID> {
    @Serializable
    @JvmInline
    @References(ClinicInvoice::class)
    value class ID(override val raw: Uuid) : TypedId<Uuid, ID> {
        override fun toString(): String = raw.toString()
    }
}

/* -------------------------------------------------------------------------- */
/*  Feature flags                                                             */
/* -------------------------------------------------------------------------- */

@Serializable
enum class FeatureFlag {
    // Add your project's feature flags here
}
