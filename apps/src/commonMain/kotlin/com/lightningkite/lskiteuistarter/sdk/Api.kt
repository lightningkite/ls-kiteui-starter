package com.lightningkite.lskiteuistarter.sdk



interface Api {
	fun withHeaderCalculator(calculator: suspend () -> List<Pair<String, String>>): Api

	val uploadEarlyEndpoint: com.lightningkite.lightningserver.files.ClientUploadEarlyEndpoints

	val appRelease: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lskiteuistarter.AppRelease, com.lightningkite.lskiteuistarter.AppRelease.ID>

	val user: com.lightningkite.lightningserver.typed.ClientModelRestEndpointsAndUpdatesWebsocket<com.lightningkite.lskiteuistarter.User, com.lightningkite.lskiteuistarter.User.ID>

	interface UserAuthApi : com.lightningkite.lightningserver.sessions.proofs.AuthClientEndpoints<com.lightningkite.lskiteuistarter.User, com.lightningkite.lskiteuistarter.User.ID>, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.Session<com.lightningkite.lskiteuistarter.User, com.lightningkite.lskiteuistarter.User.ID>, kotlin.uuid.Uuid> {

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

		interface TimeBasedOTPProof : com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.TimeBasedOTP, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.TotpSecret, kotlin.uuid.Uuid> {
		}
		val totp: TimeBasedOTPProof

		interface PasswordProof : com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.PasswordSecret, kotlin.uuid.Uuid>, com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Password {
		}
		val password: PasswordProof

		val backupCode: com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.BackupCode
	}
	val userAuth: UserAuthApi

	interface FcmTokenApi : com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lskiteuistarter.FcmToken, com.lightningkite.lskiteuistarter.FcmToken.ID> {
		/**
		 * Register Token
		 * 
		 * **Auth Requirements:** User with root access
		 * */
		suspend fun registerToken(input: com.lightningkite.lskiteuistarter.FcmToken.ID): com.lightningkite.services.database.EntryChange<com.lightningkite.lskiteuistarter.FcmToken>
		/**
		 * Test In-App Notifications
		 * 
		 * **Auth Requirements:** User with root access
		 * */
		suspend fun testInAppNotifications(id: com.lightningkite.lskiteuistarter.FcmToken.ID): kotlin.String
		/**
		 * Clear Token
		 * 
		 * **Auth Requirements:** No Requirements
		 * */
		suspend fun clearToken(id: com.lightningkite.lskiteuistarter.FcmToken.ID): kotlin.Boolean
	}
	val fcmToken: FcmTokenApi

	val organization: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lskiteuistarter.Organization, com.lightningkite.lskiteuistarter.Organization.ID>

	val membership: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lskiteuistarter.Membership, com.lightningkite.lskiteuistarter.Membership.ID>

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
