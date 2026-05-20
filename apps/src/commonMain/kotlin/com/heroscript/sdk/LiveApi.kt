package com.heroscript.sdk

import com.lightningkite.lightningserver.HttpMethod
import com.lightningkite.lightningserver.typed.Fetcher
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable

class LiveApi(val fetcher: Fetcher) : Api {
	override fun withHeaderCalculator(calculator: suspend () -> List<Pair<String, String>>): LiveApi = 
		LiveApi(fetcher.withHeaderCalculator(calculator))

	override val uploadEarlyEndpoint = com.lightningkite.lightningserver.files.LiveClientUploadEarlyEndpoints(fetcher, "upload-early", )

	override val appRelease = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "app-releases", com.heroscript.AppRelease.serializer(), com.heroscript.AppRelease.ID.serializer())

	override val user = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpointsAndUpdatesWebsocket(fetcher, "users/rest", com.heroscript.User.serializer(), com.heroscript.User.ID.serializer())

	inner class LiveUserAuthApi : Api.UserAuthApi, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.Session<com.heroscript.User, com.heroscript.User.ID>, kotlin.uuid.Uuid> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "auth/session/sessions", com.lightningkite.lightningserver.sessions.Session.serializer(com.heroscript.User.serializer(), com.heroscript.User.ID.serializer()), kotlin.uuid.Uuid.serializer()), com.lightningkite.lightningserver.sessions.proofs.AuthClientEndpoints<com.heroscript.User, com.heroscript.User.ID> by com.lightningkite.lightningserver.sessions.proofs.LiveAuthClientEndpoints(fetcher, "auth/session", com.heroscript.User.serializer(), com.heroscript.User.ID.serializer()) {

		inner class LiveEmailApi : Api.UserAuthApi.EmailApi, com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Email by com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.Email(fetcher, "auth/proof/email", ) {
			override suspend fun verifyNewEmail(input: com.lightningkite.services.data.EmailAddress): kotlin.String =
				fetcher("auth/proof/email/verify-new-email", HttpMethod.POST, com.lightningkite.services.data.EmailAddress.serializer(), input, kotlin.String.serializer())
		}
		override val email = LiveEmailApi()

		inner class LiveTimeBasedOTPProof : Api.UserAuthApi.TimeBasedOTPProof, com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.TimeBasedOTP by com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.TimeBasedOTP(fetcher, "auth/proof/totp", ), com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.TotpSecret, kotlin.uuid.Uuid> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "auth/proof/totp/secrets", com.lightningkite.lightningserver.sessions.TotpSecret.serializer(), kotlin.uuid.Uuid.serializer()) {
		}
		override val totp = LiveTimeBasedOTPProof()

		inner class LivePasswordProof : Api.UserAuthApi.PasswordProof, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.PasswordSecret, kotlin.uuid.Uuid> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "auth/proof/password/secrets", com.lightningkite.lightningserver.sessions.PasswordSecret.serializer(), kotlin.uuid.Uuid.serializer()), com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Password by com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.Password(fetcher, "auth/proof/password", ) {
		}
		override val password = LivePasswordProof()

		override val backupCode = com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.BackupCode(fetcher, "auth/proof/backup-codes", )
	}
	override val userAuth = LiveUserAuthApi()

	inner class LiveFcmTokenApi : Api.FcmTokenApi, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.FcmToken, com.heroscript.FcmToken.ID> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "fcmTokens", com.heroscript.FcmToken.serializer(), com.heroscript.FcmToken.ID.serializer()) {
		override suspend fun registerToken(input: com.heroscript.FcmToken.ID): com.lightningkite.services.database.EntryChange<com.heroscript.FcmToken> =
			fetcher("fcmTokens/register", HttpMethod.POST, com.heroscript.FcmToken.ID.serializer(), input, com.lightningkite.services.database.EntryChange.serializer(com.heroscript.FcmToken.serializer()))
		override suspend fun testInAppNotifications(id: com.heroscript.FcmToken.ID): kotlin.String =
			fetcher("fcmTokens/${fetcher.url(id, com.heroscript.FcmToken.ID.serializer())}/test", HttpMethod.POST, kotlin.Unit.serializer(), kotlin.Unit, kotlin.String.serializer())
		override suspend fun clearToken(id: com.heroscript.FcmToken.ID): kotlin.Boolean =
			fetcher("fcmTokens/${fetcher.url(id, com.heroscript.FcmToken.ID.serializer())}/clear", HttpMethod.POST, kotlin.Unit.serializer(), kotlin.Unit, kotlin.Boolean.serializer())
	}
	override val fcmToken = LiveFcmTokenApi()

	override val clinic = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "clinics", com.heroscript.Clinic.serializer(), com.heroscript.Clinic.ID.serializer())

	override val clinicMembership = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "clinicMemberships", com.heroscript.ClinicMembership.serializer(), com.heroscript.ClinicMembership.ID.serializer())

	override val patient = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "patients", com.heroscript.Patient.serializer(), com.heroscript.Patient.ID.serializer())

	override val pharmacy = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "pharmacies", com.heroscript.Pharmacy.serializer(), com.heroscript.Pharmacy.ID.serializer())

	override val product = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "products", com.heroscript.Product.serializer(), com.heroscript.Product.ID.serializer())

	override val productPharmacyMapping = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "productPharmacyMappings", com.heroscript.ProductPharmacyMapping.serializer(), com.heroscript.ProductPharmacyMapping.ID.serializer())

	override val prescription = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "prescriptions", com.heroscript.Prescription.serializer(), com.heroscript.Prescription.ID.serializer())

	override val prescriptionOrder = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "prescriptionOrders", com.heroscript.PrescriptionOrder.serializer(), com.heroscript.PrescriptionOrder.ID.serializer())

	override val pharmacyOrder = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "pharmacyOrders", com.heroscript.PharmacyOrder.serializer(), com.heroscript.PharmacyOrder.ID.serializer())

	override val shipment = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "shipments", com.heroscript.Shipment.serializer(), com.heroscript.Shipment.ID.serializer())

	override val clinicInvoice = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "clinicInvoices", com.heroscript.ClinicInvoice.serializer(), com.heroscript.ClinicInvoice.ID.serializer())

	inner class LiveMetaApi : Api.MetaApi {
		override suspend fun getServerHealth(): com.lightningkite.lightningserver.typed.ServerHealth =
			fetcher("meta/health", HttpMethod.GET, kotlin.Unit.serializer(), kotlin.Unit, com.lightningkite.lightningserver.typed.ServerHealth.serializer())
		override suspend fun bulkRequest(input: Map<String, com.lightningkite.lightningserver.typed.BulkRequest>): Map<String, com.lightningkite.lightningserver.typed.BulkResponse> =
			fetcher("meta/bulk", HttpMethod.POST, MapSerializer(String.serializer(), com.lightningkite.lightningserver.typed.BulkRequest.serializer()), input, MapSerializer(String.serializer(), com.lightningkite.lightningserver.typed.BulkResponse.serializer()))
	}
	override val meta = LiveMetaApi()
}
