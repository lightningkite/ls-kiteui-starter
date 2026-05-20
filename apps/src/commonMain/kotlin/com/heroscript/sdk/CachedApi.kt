package com.heroscript.sdk

import com.lightningkite.lightningserver.db.*
import kotlinx.serialization.builtins.*

open class CachedApi(val uncached: Api) {
	open val appReleases = ModelCache(uncached.appRelease, com.heroscript.AppRelease.serializer())
	open val users = ModelCache(uncached.user, com.heroscript.User.serializer())
	open val sessions = ModelCache(uncached.userAuth, com.lightningkite.lightningserver.sessions.Session.serializer(com.heroscript.User.serializer(), com.heroscript.User.ID.serializer()))
	open val totpSecrets = ModelCache(uncached.userAuth.totp, com.lightningkite.lightningserver.sessions.TotpSecret.serializer())
	open val passwordSecrets = ModelCache(uncached.userAuth.password, com.lightningkite.lightningserver.sessions.PasswordSecret.serializer())
	open val fcmTokens = ModelCache(uncached.fcmToken, com.heroscript.FcmToken.serializer())
	open val clinics = ModelCache(uncached.clinic, com.heroscript.Clinic.serializer())
	open val clinicMemberships = ModelCache(uncached.clinicMembership, com.heroscript.ClinicMembership.serializer())
	open val patients = ModelCache(uncached.patient, com.heroscript.Patient.serializer())
	open val pharmacies = ModelCache(uncached.pharmacy, com.heroscript.Pharmacy.serializer())
	open val products = ModelCache(uncached.product, com.heroscript.Product.serializer())
	open val productPharmacyMappings = ModelCache(uncached.productPharmacyMapping, com.heroscript.ProductPharmacyMapping.serializer())
	open val prescriptions = ModelCache(uncached.prescription, com.heroscript.Prescription.serializer())
	open val prescriptionOrders = ModelCache(uncached.prescriptionOrder, com.heroscript.PrescriptionOrder.serializer())
	open val pharmacyOrders = ModelCache(uncached.pharmacyOrder, com.heroscript.PharmacyOrder.serializer())
	open val shipments = ModelCache(uncached.shipment, com.heroscript.Shipment.serializer())
	open val clinicInvoices = ModelCache(uncached.clinicInvoice, com.heroscript.ClinicInvoice.serializer())
}
