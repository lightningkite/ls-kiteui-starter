
package com.lightningkite.template.sdk

import com.lightningkite.*
import com.lightningkite.kiteui.*
import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.db.*
import kotlinx.datetime.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.auth.oauth.*
import com.lightningkite.lightningserver.auth.proof.*
import com.lightningkite.lightningserver.auth.subject.*
import com.lightningkite.serialization.*
import com.lightningkite.lightningserver.serverhealth.ServerHealth
import com.lightningkite.lightningserver.typed.BulkRequest
import com.lightningkite.lightningserver.typed.BulkResponse
import com.lightningkite.template.User
import com.lightningkite.lightningdb.ModelPermissions
import com.lightningkite.lightningdb.Query
import com.lightningkite.lightningdb.QueryPartial
import com.lightningkite.serialization.Partial
import com.lightningkite.UUID
import com.lightningkite.lightningdb.MassModification
import kotlin.Int
import com.lightningkite.lightningdb.Modification
import com.lightningkite.lightningdb.EntryChange
import com.lightningkite.lightningdb.Condition
import kotlin.Unit
import com.lightningkite.lightningdb.GroupCountQuery
import com.lightningkite.lightningdb.AggregateQuery
import kotlin.Double
import com.lightningkite.lightningdb.GroupAggregateQuery
import kotlin.String
import com.lightningkite.lightningserver.auth.proof.FinishProof
import com.lightningkite.lightningserver.auth.proof.Proof
import com.lightningkite.lightningserver.auth.proof.OtpSecret
import com.lightningkite.lightningserver.auth.proof.EstablishOtp
import com.lightningkite.lightningserver.auth.proof.IdentificationAndPassword
import com.lightningkite.lightningserver.auth.proof.PasswordSecret
import com.lightningkite.lightningserver.auth.proof.EstablishPassword
import com.lightningkite.lightningserver.auth.subject.IdAndAuthMethods
import com.lightningkite.lightningserver.auth.subject.LogInRequest
import com.lightningkite.lightningserver.auth.subject.ProofsCheckResult
import com.lightningkite.lightningserver.auth.subject.SubSessionRequest
import com.lightningkite.lightningserver.auth.oauth.OauthTokenRequest
import com.lightningkite.lightningserver.auth.oauth.OauthResponse
import com.lightningkite.lightningserver.auth.subject.Session

interface Api {
    val user: UserApi
    val emailProof: EmailProofApi
    val otpSecret: OtpSecretApi
    val oneTimePasswordProof: OneTimePasswordProofApi
    val passwordSecret: PasswordSecretApi
    val passwordProof: PasswordProofApi
    val userAuth: UserAuthApi
    val userSession: UserSessionApi
    suspend fun getServerHealth(userAccessToken: suspend () -> String, masquerade: String?): ServerHealth
    suspend fun bulkRequest(input: Map<String, BulkRequest>): Map<String, BulkResponse>
    interface UserApi {
        suspend fun default(userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun permissions(userAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<User>
        suspend fun query(input: Query<User>, userAccessToken: suspend () -> String, masquerade: String?): List<User>
        suspend fun queryPartial(input: QueryPartial<User>, userAccessToken: suspend () -> String, masquerade: String?): List<Partial<User>>
        suspend fun detail(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun insertBulk(input: List<User>, userAccessToken: suspend () -> String, masquerade: String?): List<User>
        suspend fun insert(input: User, userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun upsert(id: UUID, input: User, userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun bulkReplace(input: List<User>, userAccessToken: suspend () -> String, masquerade: String?): List<User>
        suspend fun replace(id: UUID, input: User, userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun bulkModify(input: MassModification<User>, userAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun modifyWithDiff(id: UUID, input: Modification<User>, userAccessToken: suspend () -> String, masquerade: String?): EntryChange<User>
        suspend fun modify(id: UUID, input: Modification<User>, userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun simplifiedModify(id: UUID, input: Partial<User>, userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun bulkDelete(input: Condition<User>, userAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun delete(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): Unit
        suspend fun count(input: Condition<User>, userAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun groupCount(input: GroupCountQuery<User>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Int>
        suspend fun aggregate(input: AggregateQuery<User>, userAccessToken: suspend () -> String, masquerade: String?): Double?
        suspend fun groupAggregate(input: GroupAggregateQuery<User>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?>
    }
    interface EmailProofApi {
        suspend fun beginEmailOwnershipProof(input: String): String
        suspend fun proveEmailOwnership(input: FinishProof): Proof
    }
    interface OtpSecretApi {
        suspend fun default(anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun permissions(anyAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<OtpSecret>
        suspend fun query(input: Query<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<OtpSecret>
        suspend fun queryPartial(input: QueryPartial<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<Partial<OtpSecret>>
        suspend fun detail(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun insertBulk(input: List<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<OtpSecret>
        suspend fun insert(input: OtpSecret, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun upsert(id: UUID, input: OtpSecret, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun bulkReplace(input: List<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<OtpSecret>
        suspend fun replace(id: UUID, input: OtpSecret, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun bulkModify(input: MassModification<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun modifyWithDiff(id: UUID, input: Modification<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): EntryChange<OtpSecret>
        suspend fun modify(id: UUID, input: Modification<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun simplifiedModify(id: UUID, input: Partial<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret
        suspend fun bulkDelete(input: Condition<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun delete(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): Unit
        suspend fun count(input: Condition<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun groupCount(input: GroupCountQuery<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Int>
        suspend fun aggregate(input: AggregateQuery<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Double?
        suspend fun groupAggregate(input: GroupAggregateQuery<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?>
    }
    interface OneTimePasswordProofApi {
        suspend fun establishOneTimePassword(input: EstablishOtp, anyAccessToken: suspend () -> String, masquerade: String?): String
        suspend fun proveOTP(input: IdentificationAndPassword): Proof
    }
    interface PasswordSecretApi {
        suspend fun default(anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun permissions(anyAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<PasswordSecret>
        suspend fun query(input: Query<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<PasswordSecret>
        suspend fun queryPartial(input: QueryPartial<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<Partial<PasswordSecret>>
        suspend fun detail(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun insertBulk(input: List<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<PasswordSecret>
        suspend fun insert(input: PasswordSecret, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun upsert(id: UUID, input: PasswordSecret, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun bulkReplace(input: List<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<PasswordSecret>
        suspend fun replace(id: UUID, input: PasswordSecret, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun bulkModify(input: MassModification<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun modifyWithDiff(id: UUID, input: Modification<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): EntryChange<PasswordSecret>
        suspend fun modify(id: UUID, input: Modification<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun simplifiedModify(id: UUID, input: Partial<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret
        suspend fun bulkDelete(input: Condition<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun delete(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): Unit
        suspend fun count(input: Condition<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun groupCount(input: GroupCountQuery<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Int>
        suspend fun aggregate(input: AggregateQuery<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Double?
        suspend fun groupAggregate(input: GroupAggregateQuery<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?>
    }
    interface PasswordProofApi {
        suspend fun establishPassword(input: EstablishPassword, anyAccessToken: suspend () -> String, masquerade: String?): Unit
        suspend fun provePasswordOwnership(input: IdentificationAndPassword): Proof
    }
    interface UserAuthApi {
        suspend fun logIn(input: List<Proof>): IdAndAuthMethods<UUID>
        suspend fun logInV2(input: LogInRequest): IdAndAuthMethods<UUID>
        suspend fun checkProofs(input: List<Proof>): ProofsCheckResult<UUID>
        suspend fun openSession(input: String): String
        suspend fun createSubSession(input: SubSessionRequest, userAccessToken: suspend () -> String, masquerade: String?): String
        suspend fun getToken(input: OauthTokenRequest): OauthResponse
        suspend fun getTokenSimple(input: String): String
        suspend fun getSelf(userAccessToken: suspend () -> String, masquerade: String?): User
        suspend fun terminateSession(userAccessToken: suspend () -> String, masquerade: String?): Unit
        suspend fun terminateOtherSession(sessionId: UUID, userAccessToken: suspend () -> String, masquerade: String?): Unit
    }
    interface UserSessionApi {
        suspend fun default(userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun permissions(userAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<Session<User, UUID>>
        suspend fun query(input: Query<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Session<User, UUID>>
        suspend fun queryPartial(input: QueryPartial<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Partial<Session<User, UUID>>>
        suspend fun detail(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun insertBulk(input: List<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Session<User, UUID>>
        suspend fun insert(input: Session<User, UUID>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun upsert(id: UUID, input: Session<User, UUID>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun bulkReplace(input: List<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Session<User, UUID>>
        suspend fun replace(id: UUID, input: Session<User, UUID>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun bulkModify(input: MassModification<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun modifyWithDiff(id: UUID, input: Modification<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): EntryChange<Session<User, UUID>>
        suspend fun modify(id: UUID, input: Modification<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun simplifiedModify(id: UUID, input: Partial<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID>
        suspend fun bulkDelete(input: Condition<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun delete(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): Unit
        suspend fun count(input: Condition<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Int
        suspend fun groupCount(input: GroupCountQuery<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Int>
        suspend fun aggregate(input: AggregateQuery<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Double?
        suspend fun groupAggregate(input: GroupAggregateQuery<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?>
    }
}

