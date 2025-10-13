package com.lightningkite.template.sdk



interface Api {
	fun withHeaderCalculator(calculator: suspend () -> List<Pair<String, String>>): Api
	/**
	 * Example Endpoint
	 * 
	 * **Auth Requirements:** No Requirements
	 * */
	suspend fun exampleEndpoint(): kotlin.Int
	/**
	 * Example Endpoint
	 * 
	 * **Auth Requirements:** User with scope *
	 * */
	suspend fun exampleEndpoint(input: kotlin.Int): kotlin.Int

	val user: com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.template.User, kotlin.uuid.Uuid>

	interface UserAuthApi : com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.Session<com.lightningkite.template.User, kotlin.uuid.Uuid>, kotlin.uuid.Uuid>, com.lightningkite.lightningserver.sessions.proofs.AuthClientEndpoints<com.lightningkite.template.User, kotlin.uuid.Uuid> {

		interface EmailApi : com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Email {
			/**
			 * Verify New Email
			 * 
			 * Sends a verification passcode to a new email.
			 * 
			 * **Auth Requirements:** User with scope *
			 * */
			suspend fun verifyNewEmail(input: com.lightningkite.EmailAddress): kotlin.String
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

	interface FcmTokenApi : com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.template.FcmToken, kotlin.String> {
		/**
		 * Register Token
		 * 
		 * **Auth Requirements:** User with scope *
		 * */
		suspend fun registerToken(input: kotlin.String): com.lightningkite.services.database.EntryChange<com.lightningkite.template.FcmToken>
		/**
		 * Test In-App Notifications
		 * 
		 * **Auth Requirements:** User with scope *
		 * */
		suspend fun testInAppNotifications(id: kotlin.String): kotlin.String
		/**
		 * Clear Token
		 * 
		 * **Auth Requirements:** No Requirements
		 * */
		suspend fun clearToken(id: kotlin.String): kotlin.Boolean
	}
	val fcmToken: FcmTokenApi

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
