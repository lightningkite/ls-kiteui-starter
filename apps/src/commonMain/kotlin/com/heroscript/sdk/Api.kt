package com.heroscript.sdk



interface Api {
	fun withHeaderCalculator(calculator: suspend () -> List<Pair<String, String>>): Api

	val uploadEarlyEndpoint: com.lightningkite.lightningserver.files.ClientUploadEarlyEndpoints

	val appRelease: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.AppRelease, com.heroscript.AppRelease.ID>

	val user: com.lightningkite.lightningserver.typed.ClientModelRestEndpointsAndUpdatesWebsocket<com.heroscript.User, com.heroscript.User.ID>

	interface UserAuthApi : com.lightningkite.lightningserver.sessions.proofs.AuthClientEndpoints<com.heroscript.User, com.heroscript.User.ID>, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.Session<com.heroscript.User, com.heroscript.User.ID>, kotlin.uuid.Uuid> {

		interface EmailApi : com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Email {
			/**
			 * Verify New Email
			 * 
			 * Sends a verification passcode to a new email.
			 * 
			 * **Auth Requirements:** User with root access
			 * */
			suspend fun verifyNewEmail(input: com.lightningkite.services.data.EmailAddress): kotlin.String
		}
		val email: EmailApi

		interface TimeBasedOTPProof : com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.TotpSecret, kotlin.uuid.Uuid>, com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.TimeBasedOTP {
		}
		val totp: TimeBasedOTPProof

		interface PasswordProof : com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Password, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.PasswordSecret, kotlin.uuid.Uuid> {
		}
		val password: PasswordProof

		val backupCode: com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.BackupCode
	}
	val userAuth: UserAuthApi

	interface FcmTokenApi : com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.FcmToken, com.heroscript.FcmToken.ID> {
		/**
		 * Register Token
		 * 
		 * **Auth Requirements:** User with root access
		 * */
		suspend fun registerToken(input: com.heroscript.FcmToken.ID): com.lightningkite.services.database.EntryChange<com.heroscript.FcmToken>
		/**
		 * Test In-App Notifications
		 * 
		 * **Auth Requirements:** User with root access
		 * */
		suspend fun testInAppNotifications(id: com.heroscript.FcmToken.ID): kotlin.String
		/**
		 * Clear Token
		 * 
		 * **Auth Requirements:** No Requirements
		 * */
		suspend fun clearToken(id: com.heroscript.FcmToken.ID): kotlin.Boolean
	}
	val fcmToken: FcmTokenApi

	val clinic: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.Clinic, com.heroscript.Clinic.ID>

	val clinicMembership: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.ClinicMembership, com.heroscript.ClinicMembership.ID>

	val patient: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.Patient, com.heroscript.Patient.ID>

	val pharmacy: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.Pharmacy, com.heroscript.Pharmacy.ID>

	val product: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.Product, com.heroscript.Product.ID>

	val productPharmacyMapping: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.ProductPharmacyMapping, com.heroscript.ProductPharmacyMapping.ID>

	val prescription: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.Prescription, com.heroscript.Prescription.ID>

	val prescriptionOrder: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.PrescriptionOrder, com.heroscript.PrescriptionOrder.ID>

	val pharmacyOrder: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.PharmacyOrder, com.heroscript.PharmacyOrder.ID>

	val shipment: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.Shipment, com.heroscript.Shipment.ID>

	val clinicInvoice: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.heroscript.ClinicInvoice, com.heroscript.ClinicInvoice.ID>

	interface MetaApi {
		/**
		 * Get Server Health
		 * 
		 * Gets the current status of the server
		 * 
		 * **Auth Requirements:** No Requirements
		 * */
		suspend fun getServerHealth(): com.lightningkite.lightningserver.typed.ServerHealth
		/**
		 * Bulk Request
		 * 
		 * Performs multiple requests at once, returning the results in the same order.
		 * 
		 * **Auth Requirements:** No Requirements
		 * */
		suspend fun bulkRequest(input: Map<String, com.lightningkite.lightningserver.typed.BulkRequest>): Map<String, com.lightningkite.lightningserver.typed.BulkResponse>
	}
	val meta: MetaApi
}
