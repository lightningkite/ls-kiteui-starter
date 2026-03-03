// by Claude — Mock Api implementation for UI tests, backed by in-memory data
package com.lightningkite.lskiteuistarter.testing

import com.lightningkite.EmailAddress
import com.lightningkite.lightningserver.files.ClientUploadEarlyEndpoints
import com.lightningkite.lightningserver.files.UploadInformation
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lightningserver.sessions.proofs.*
import com.lightningkite.lightningserver.typed.ClientModelRestEndpoints
import com.lightningkite.lightningserver.typed.ServerHealth
import com.lightningkite.lightningserver.typed.BulkRequest
import com.lightningkite.lightningserver.typed.BulkResponse
import com.lightningkite.lskiteuistarter.*
import com.lightningkite.lskiteuistarter.sdk.Api
import com.lightningkite.services.database.*
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MockApi(
    val testUser: User,
    memberships: List<Membership> = emptyList(),
    inventoryItems: List<InventoryItem> = emptyList(),
    organizations: List<Organization> = emptyList(),
) : Api {

    override fun withHeaderCalculator(calculator: suspend () -> List<Pair<String, String>>): Api = this

    override suspend fun exampleEndpoint(): Int = 0
    override suspend fun exampleEndpoint(input: Int): Int = input

    override val uploadEarlyEndpoint = object : ClientUploadEarlyEndpoints {
        override suspend fun uploadFileForRequest(): UploadInformation =
            throw NotImplementedError("uploadFileForRequest not mocked")
        override suspend fun verifyUploadedFile(input: String): String = input
    }

    override val appRelease = MockModelEndpoints<AppRelease, Uuid>()
    override val user = MockModelEndpoints(listOf(testUser))

    override val userAuth: Api.UserAuthApi = object : Api.UserAuthApi,
        ClientModelRestEndpoints<Session<User, Uuid>, Uuid> by MockModelEndpoints() {

        override suspend fun logIn(input: List<Proof>): IdAndAuthMethods<Uuid> =
            throw NotImplementedError("logIn not mocked")
        override suspend fun logInV2(input: LogInRequest): IdAndAuthMethods<Uuid> =
            throw NotImplementedError("logInV2 not mocked")
        override suspend fun checkProofs(input: List<Proof>): ProofsCheckResult<Uuid> =
            throw NotImplementedError("checkProofs not mocked")
        override suspend fun getTokenSimple(input: String): String = "fake-jwt-token"
        override suspend fun getSelf(): User = testUser
        override suspend fun subsession(input: SubSessionRequest): String = "fake-subsession"
        override suspend fun authRequirements(): AuthRequirements =
            AuthRequirements(options = emptyList(), strengthRequired = 0)
        override suspend fun terminateSession() {}
        override suspend fun terminateSession(sessionId: Uuid) {}

        override val email = object : Api.UserAuthApi.EmailApi {
            override val via: String get() = "email"
            override val property: String get() = "email"
            override suspend fun beginEmailOwnershipProof(input: String): String = "fake-key"
            override suspend fun proveEmailOwnership(input: FinishProof): Proof =
                throw NotImplementedError("proveEmailOwnership not mocked")
            override suspend fun verifyNewEmail(input: EmailAddress): String = "ok"
        }

        override val totp = object : Api.UserAuthApi.TimeBasedOTPProof,
            ClientModelRestEndpoints<TotpSecret, Uuid> by MockModelEndpoints() {
            override val via: String get() = "totp"
            override suspend fun proveOTP(input: IdentificationAndPassword): Proof =
                throw NotImplementedError("proveOTP not mocked")
            override suspend fun establishOneTimePassword(input: EstablishTotp): String = "otpauth://..."
            override suspend fun confirmOneTimePassword(input: String) {}
        }

        override val password = object : Api.UserAuthApi.PasswordProof,
            ClientModelRestEndpoints<PasswordSecret, Uuid> by MockModelEndpoints() {
            override val via: String get() = "password"
            override suspend fun provePasswordOwnership(input: IdentificationAndPassword): Proof =
                throw NotImplementedError("provePasswordOwnership not mocked")
            override suspend fun establishPassword(input: EstablishPassword) {}
        }

        override val backupCode = object : ProofClientEndpoints.BackupCode {
            override val via: String get() = "backupcode"
            override suspend fun proveBackupCode(input: IdentificationAndPassword): Proof =
                throw NotImplementedError("proveBackupCode not mocked")
            override suspend fun resetCodes(): List<String> = listOf("code1", "code2")
            override suspend fun clearCodes() {}
            override suspend fun established(): Boolean = false
        }
    }

    override val fcmToken: Api.FcmTokenApi = object : Api.FcmTokenApi,
        ClientModelRestEndpoints<FcmToken, String> by MockModelEndpoints() {
        override suspend fun registerToken(input: String): EntryChange<FcmToken> {
            val token = FcmToken(_id = input, user = testUser._id)
            return EntryChange(null, token)
        }
        override suspend fun testInAppNotifications(id: String): String = "sent"
        override suspend fun clearToken(id: String): Boolean = true
    }

    override val organization = MockModelEndpoints(organizations)
    override val membership = MockModelEndpoints(memberships)
    override val inventoryItem = MockModelEndpoints(inventoryItems)

    override val meta = object : Api.MetaApi {
        override suspend fun getServerHealth(): ServerHealth =
            throw NotImplementedError("getServerHealth not mocked")
        override suspend fun bulkRequest(input: Map<String, BulkRequest>): Map<String, BulkResponse> = emptyMap()
    }
}
