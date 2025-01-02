
package com.lightningkite.template.sdk

import com.lightningkite.*
import com.lightningkite.kiteui.*
import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.db.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.serialization.*
import kotlinx.datetime.*
import com.lightningkite.lightningserver.typed.BulkRequest
import com.lightningkite.lightningserver.typed.BulkResponse
import kotlin.String
import com.lightningkite.lightningserver.auth.proof.FinishProof
import com.lightningkite.lightningserver.auth.proof.Proof
import com.lightningkite.lightningserver.auth.proof.IdentificationAndPassword
import com.lightningkite.UUID
import com.lightningkite.lightningserver.auth.subject.IdAndAuthMethods
import com.lightningkite.lightningserver.auth.subject.LogInRequest
import com.lightningkite.lightningserver.auth.subject.ProofsCheckResult
import com.lightningkite.lightningserver.auth.oauth.OauthTokenRequest
import com.lightningkite.lightningserver.auth.oauth.OauthResponse
import com.lightningkite.lightningserver.serverhealth.ServerHealth
import com.lightningkite.template.User
import com.lightningkite.lightningdb.ModelPermissions
import com.lightningkite.lightningdb.Query
import com.lightningkite.lightningdb.QueryPartial
import com.lightningkite.serialization.Partial
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
import com.lightningkite.lightningserver.auth.subject.SubSessionRequest
import com.lightningkite.lightningserver.auth.subject.Session
import com.lightningkite.lightningserver.auth.proof.OtpSecret
import com.lightningkite.lightningserver.auth.proof.EstablishOtp
import com.lightningkite.lightningserver.auth.proof.PasswordSecret
import com.lightningkite.lightningserver.auth.proof.EstablishPassword

open class AbstractAnonymousSession(val api: Api) {
    val user: AbstractAnonymousSessionUserApi = AbstractAnonymousSessionUserApi(api.user)
    val emailProof: AbstractAnonymousSessionEmailProofApi = AbstractAnonymousSessionEmailProofApi(api.emailProof)
    val otpSecret: AbstractAnonymousSessionOtpSecretApi = AbstractAnonymousSessionOtpSecretApi(api.otpSecret)
    val oneTimePasswordProof: AbstractAnonymousSessionOneTimePasswordProofApi = AbstractAnonymousSessionOneTimePasswordProofApi(api.oneTimePasswordProof)
    val passwordSecret: AbstractAnonymousSessionPasswordSecretApi = AbstractAnonymousSessionPasswordSecretApi(api.passwordSecret)
    val passwordProof: AbstractAnonymousSessionPasswordProofApi = AbstractAnonymousSessionPasswordProofApi(api.passwordProof)
    val userAuth: AbstractAnonymousSessionUserAuthApi = AbstractAnonymousSessionUserAuthApi(api.userAuth)
    val userSession: AbstractAnonymousSessionUserSessionApi = AbstractAnonymousSessionUserSessionApi(api.userSession)
    suspend fun bulkRequest(input: Map<String, BulkRequest>): Map<String, BulkResponse> = api.bulkRequest(input)
    open class AbstractAnonymousSessionUserApi(val api: Api.UserApi) {
    }
    open class AbstractAnonymousSessionEmailProofApi(val api: Api.EmailProofApi): EmailProofClientEndpoints {
        override suspend fun beginEmailOwnershipProof(input: String): String = api.beginEmailOwnershipProof(input)
        override suspend fun proveEmailOwnership(input: FinishProof): Proof = api.proveEmailOwnership(input)
    }
    open class AbstractAnonymousSessionOtpSecretApi(val api: Api.OtpSecretApi) {
    }
    open class AbstractAnonymousSessionOneTimePasswordProofApi(val api: Api.OneTimePasswordProofApi): OneTimePasswordProofClientEndpoints {
        override suspend fun proveOTP(input: IdentificationAndPassword): Proof = api.proveOTP(input)
    }
    open class AbstractAnonymousSessionPasswordSecretApi(val api: Api.PasswordSecretApi) {
    }
    open class AbstractAnonymousSessionPasswordProofApi(val api: Api.PasswordProofApi): PasswordProofClientEndpoints {
        override suspend fun provePasswordOwnership(input: IdentificationAndPassword): Proof = api.provePasswordOwnership(input)
    }
    open class AbstractAnonymousSessionUserAuthApi(val api: Api.UserAuthApi): UserAuthClientEndpoints<UUID> {
        override suspend fun logIn(input: List<Proof>): IdAndAuthMethods<UUID> = api.logIn(input)
        override suspend fun logInV2(input: LogInRequest): IdAndAuthMethods<UUID> = api.logInV2(input)
        override suspend fun checkProofs(input: List<Proof>): ProofsCheckResult<UUID> = api.checkProofs(input)
        override suspend fun openSession(input: String): String = api.openSession(input)
        override suspend fun getToken(input: OauthTokenRequest): OauthResponse = api.getToken(input)
        override suspend fun getTokenSimple(input: String): String = api.getTokenSimple(input)
    }
    open class AbstractAnonymousSessionUserSessionApi(val api: Api.UserSessionApi) {
    }
}

abstract class AbstractUserSession(api: Api, userToken: String, userAccessToken: suspend () -> String, masquerade: String? = null) {
    abstract val api: Api
    abstract val userToken: String
    abstract val userAccessToken: suspend () -> String
    open val masquerade: String? = null
    val user: UserSessionUserApi = UserSessionUserApi(api.user, userToken, userAccessToken, masquerade)
    val emailProof: UserSessionEmailProofApi = UserSessionEmailProofApi(api.emailProof, userToken, userAccessToken, masquerade)
    val otpSecret: UserSessionOtpSecretApi = UserSessionOtpSecretApi(api.otpSecret, userToken, userAccessToken, masquerade)
    val oneTimePasswordProof: UserSessionOneTimePasswordProofApi = UserSessionOneTimePasswordProofApi(api.oneTimePasswordProof, userToken, userAccessToken, masquerade)
    val passwordSecret: UserSessionPasswordSecretApi = UserSessionPasswordSecretApi(api.passwordSecret, userToken, userAccessToken, masquerade)
    val passwordProof: UserSessionPasswordProofApi = UserSessionPasswordProofApi(api.passwordProof, userToken, userAccessToken, masquerade)
    val userAuth: UserSessionUserAuthApi = UserSessionUserAuthApi(api.userAuth, userToken, userAccessToken, masquerade)
    val userSession: UserSessionUserSessionApi = UserSessionUserSessionApi(api.userSession, userToken, userAccessToken, masquerade)
    suspend fun getServerHealth(): ServerHealth = api.getServerHealth(userAccessToken, masquerade)
    suspend fun bulkRequest(input: Map<String, BulkRequest>): Map<String, BulkResponse> = api.bulkRequest(input)
    class UserSessionUserApi(val api: Api.UserApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?): ClientModelRestEndpoints<User, UUID> {
        override suspend fun default(): User = api.default(userAccessToken, masquerade)
        override suspend fun permissions(): ModelPermissions<User> = api.permissions(userAccessToken, masquerade)
        override suspend fun query(input: Query<User>): List<User> = api.query(input, userAccessToken, masquerade)
        override suspend fun queryPartial(input: QueryPartial<User>): List<Partial<User>> = api.queryPartial(input, userAccessToken, masquerade)
        override suspend fun detail(id: UUID): User = api.detail(id, userAccessToken, masquerade)
        override suspend fun insertBulk(input: List<User>): List<User> = api.insertBulk(input, userAccessToken, masquerade)
        override suspend fun insert(input: User): User = api.insert(input, userAccessToken, masquerade)
        override suspend fun upsert(id: UUID, input: User): User = api.upsert(id, input, userAccessToken, masquerade)
        override suspend fun bulkReplace(input: List<User>): List<User> = api.bulkReplace(input, userAccessToken, masquerade)
        override suspend fun replace(id: UUID, input: User): User = api.replace(id, input, userAccessToken, masquerade)
        override suspend fun bulkModify(input: MassModification<User>): Int = api.bulkModify(input, userAccessToken, masquerade)
        override suspend fun modifyWithDiff(id: UUID, input: Modification<User>): EntryChange<User> = api.modifyWithDiff(id, input, userAccessToken, masquerade)
        override suspend fun modify(id: UUID, input: Modification<User>): User = api.modify(id, input, userAccessToken, masquerade)
        suspend fun simplifiedModify(id: UUID, input: Partial<User>): User = api.simplifiedModify(id, input, userAccessToken, masquerade)
        override suspend fun bulkDelete(input: Condition<User>): Int = api.bulkDelete(input, userAccessToken, masquerade)
        override suspend fun delete(id: UUID): Unit = api.delete(id, userAccessToken, masquerade)
        override suspend fun count(input: Condition<User>): Int = api.count(input, userAccessToken, masquerade)
        override suspend fun groupCount(input: GroupCountQuery<User>): Map<String, Int> = api.groupCount(input, userAccessToken, masquerade)
        override suspend fun aggregate(input: AggregateQuery<User>): Double? = api.aggregate(input, userAccessToken, masquerade)
        override suspend fun groupAggregate(input: GroupAggregateQuery<User>): Map<String, Double?> = api.groupAggregate(input, userAccessToken, masquerade)
    }
    class UserSessionEmailProofApi(val api: Api.EmailProofApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?): EmailProofClientEndpoints {
        override suspend fun beginEmailOwnershipProof(input: String): String = api.beginEmailOwnershipProof(input)
        override suspend fun proveEmailOwnership(input: FinishProof): Proof = api.proveEmailOwnership(input)
    }
    class UserSessionOtpSecretApi(val api: Api.OtpSecretApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?) {
    }
    class UserSessionOneTimePasswordProofApi(val api: Api.OneTimePasswordProofApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?): OneTimePasswordProofClientEndpoints {
        override suspend fun proveOTP(input: IdentificationAndPassword): Proof = api.proveOTP(input)
    }
    class UserSessionPasswordSecretApi(val api: Api.PasswordSecretApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?) {
    }
    class UserSessionPasswordProofApi(val api: Api.PasswordProofApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?): PasswordProofClientEndpoints {
        override suspend fun provePasswordOwnership(input: IdentificationAndPassword): Proof = api.provePasswordOwnership(input)
    }
    class UserSessionUserAuthApi(val api: Api.UserAuthApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?): UserAuthClientEndpoints<UUID>, AuthenticatedUserAuthClientEndpoints<User, UUID> {
        override suspend fun logIn(input: List<Proof>): IdAndAuthMethods<UUID> = api.logIn(input)
        override suspend fun logInV2(input: LogInRequest): IdAndAuthMethods<UUID> = api.logInV2(input)
        override suspend fun checkProofs(input: List<Proof>): ProofsCheckResult<UUID> = api.checkProofs(input)
        override suspend fun openSession(input: String): String = api.openSession(input)
        override suspend fun createSubSession(input: SubSessionRequest): String = api.createSubSession(input, userAccessToken, masquerade)
        override suspend fun getToken(input: OauthTokenRequest): OauthResponse = api.getToken(input)
        override suspend fun getTokenSimple(input: String): String = api.getTokenSimple(input)
        override suspend fun getSelf(): User = api.getSelf(userAccessToken, masquerade)
        override suspend fun terminateSession(): Unit = api.terminateSession(userAccessToken, masquerade)
        override suspend fun terminateOtherSession(sessionId: UUID): Unit = api.terminateOtherSession(sessionId, userAccessToken, masquerade)
    }
    class UserSessionUserSessionApi(val api: Api.UserSessionApi,val userToken:String, val userAccessToken: suspend () -> String, val masquerade: String?): ClientModelRestEndpoints<Session<User, UUID>, UUID> {
        override suspend fun default(): Session<User, UUID> = api.default(userAccessToken, masquerade)
        override suspend fun permissions(): ModelPermissions<Session<User, UUID>> = api.permissions(userAccessToken, masquerade)
        override suspend fun query(input: Query<Session<User, UUID>>): List<Session<User, UUID>> = api.query(input, userAccessToken, masquerade)
        override suspend fun queryPartial(input: QueryPartial<Session<User, UUID>>): List<Partial<Session<User, UUID>>> = api.queryPartial(input, userAccessToken, masquerade)
        override suspend fun detail(id: UUID): Session<User, UUID> = api.detail(id, userAccessToken, masquerade)
        override suspend fun insertBulk(input: List<Session<User, UUID>>): List<Session<User, UUID>> = api.insertBulk(input, userAccessToken, masquerade)
        override suspend fun insert(input: Session<User, UUID>): Session<User, UUID> = api.insert(input, userAccessToken, masquerade)
        override suspend fun upsert(id: UUID, input: Session<User, UUID>): Session<User, UUID> = api.upsert(id, input, userAccessToken, masquerade)
        override suspend fun bulkReplace(input: List<Session<User, UUID>>): List<Session<User, UUID>> = api.bulkReplace(input, userAccessToken, masquerade)
        override suspend fun replace(id: UUID, input: Session<User, UUID>): Session<User, UUID> = api.replace(id, input, userAccessToken, masquerade)
        override suspend fun bulkModify(input: MassModification<Session<User, UUID>>): Int = api.bulkModify(input, userAccessToken, masquerade)
        override suspend fun modifyWithDiff(id: UUID, input: Modification<Session<User, UUID>>): EntryChange<Session<User, UUID>> = api.modifyWithDiff(id, input, userAccessToken, masquerade)
        override suspend fun modify(id: UUID, input: Modification<Session<User, UUID>>): Session<User, UUID> = api.modify(id, input, userAccessToken, masquerade)
        suspend fun simplifiedModify(id: UUID, input: Partial<Session<User, UUID>>): Session<User, UUID> = api.simplifiedModify(id, input, userAccessToken, masquerade)
        override suspend fun bulkDelete(input: Condition<Session<User, UUID>>): Int = api.bulkDelete(input, userAccessToken, masquerade)
        override suspend fun delete(id: UUID): Unit = api.delete(id, userAccessToken, masquerade)
        override suspend fun count(input: Condition<Session<User, UUID>>): Int = api.count(input, userAccessToken, masquerade)
        override suspend fun groupCount(input: GroupCountQuery<Session<User, UUID>>): Map<String, Int> = api.groupCount(input, userAccessToken, masquerade)
        override suspend fun aggregate(input: AggregateQuery<Session<User, UUID>>): Double? = api.aggregate(input, userAccessToken, masquerade)
        override suspend fun groupAggregate(input: GroupAggregateQuery<Session<User, UUID>>): Map<String, Double?> = api.groupAggregate(input, userAccessToken, masquerade)
    }
}

abstract class AbstractAnySession(api: Api, anyToken: String, anyAccessToken: suspend () -> String, masquerade: String? = null) {
    abstract val api: Api
    abstract val anyToken: String
    abstract val anyAccessToken: suspend () -> String
    open val masquerade: String? = null
    val user: AnySessionUserApi = AnySessionUserApi(api.user, anyToken, anyAccessToken, masquerade)
    val emailProof: AnySessionEmailProofApi = AnySessionEmailProofApi(api.emailProof, anyToken, anyAccessToken, masquerade)
    val otpSecret: AnySessionOtpSecretApi = AnySessionOtpSecretApi(api.otpSecret, anyToken, anyAccessToken, masquerade)
    val oneTimePasswordProof: AnySessionOneTimePasswordProofApi = AnySessionOneTimePasswordProofApi(api.oneTimePasswordProof, anyToken, anyAccessToken, masquerade)
    val passwordSecret: AnySessionPasswordSecretApi = AnySessionPasswordSecretApi(api.passwordSecret, anyToken, anyAccessToken, masquerade)
    val passwordProof: AnySessionPasswordProofApi = AnySessionPasswordProofApi(api.passwordProof, anyToken, anyAccessToken, masquerade)
    val userAuth: AnySessionUserAuthApi = AnySessionUserAuthApi(api.userAuth, anyToken, anyAccessToken, masquerade)
    val userSession: AnySessionUserSessionApi = AnySessionUserSessionApi(api.userSession, anyToken, anyAccessToken, masquerade)
    suspend fun bulkRequest(input: Map<String, BulkRequest>): Map<String, BulkResponse> = api.bulkRequest(input)
    class AnySessionUserApi(val api: Api.UserApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?) {
    }
    class AnySessionEmailProofApi(val api: Api.EmailProofApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?): EmailProofClientEndpoints {
        override suspend fun beginEmailOwnershipProof(input: String): String = api.beginEmailOwnershipProof(input)
        override suspend fun proveEmailOwnership(input: FinishProof): Proof = api.proveEmailOwnership(input)
    }
    class AnySessionOtpSecretApi(val api: Api.OtpSecretApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?): ClientModelRestEndpoints<OtpSecret, UUID> {
        override suspend fun default(): OtpSecret = api.default(anyAccessToken, masquerade)
        override suspend fun permissions(): ModelPermissions<OtpSecret> = api.permissions(anyAccessToken, masquerade)
        override suspend fun query(input: Query<OtpSecret>): List<OtpSecret> = api.query(input, anyAccessToken, masquerade)
        override suspend fun queryPartial(input: QueryPartial<OtpSecret>): List<Partial<OtpSecret>> = api.queryPartial(input, anyAccessToken, masquerade)
        override suspend fun detail(id: UUID): OtpSecret = api.detail(id, anyAccessToken, masquerade)
        override suspend fun insertBulk(input: List<OtpSecret>): List<OtpSecret> = api.insertBulk(input, anyAccessToken, masquerade)
        override suspend fun insert(input: OtpSecret): OtpSecret = api.insert(input, anyAccessToken, masquerade)
        override suspend fun upsert(id: UUID, input: OtpSecret): OtpSecret = api.upsert(id, input, anyAccessToken, masquerade)
        override suspend fun bulkReplace(input: List<OtpSecret>): List<OtpSecret> = api.bulkReplace(input, anyAccessToken, masquerade)
        override suspend fun replace(id: UUID, input: OtpSecret): OtpSecret = api.replace(id, input, anyAccessToken, masquerade)
        override suspend fun bulkModify(input: MassModification<OtpSecret>): Int = api.bulkModify(input, anyAccessToken, masquerade)
        override suspend fun modifyWithDiff(id: UUID, input: Modification<OtpSecret>): EntryChange<OtpSecret> = api.modifyWithDiff(id, input, anyAccessToken, masquerade)
        override suspend fun modify(id: UUID, input: Modification<OtpSecret>): OtpSecret = api.modify(id, input, anyAccessToken, masquerade)
        suspend fun simplifiedModify(id: UUID, input: Partial<OtpSecret>): OtpSecret = api.simplifiedModify(id, input, anyAccessToken, masquerade)
        override suspend fun bulkDelete(input: Condition<OtpSecret>): Int = api.bulkDelete(input, anyAccessToken, masquerade)
        override suspend fun delete(id: UUID): Unit = api.delete(id, anyAccessToken, masquerade)
        override suspend fun count(input: Condition<OtpSecret>): Int = api.count(input, anyAccessToken, masquerade)
        override suspend fun groupCount(input: GroupCountQuery<OtpSecret>): Map<String, Int> = api.groupCount(input, anyAccessToken, masquerade)
        override suspend fun aggregate(input: AggregateQuery<OtpSecret>): Double? = api.aggregate(input, anyAccessToken, masquerade)
        override suspend fun groupAggregate(input: GroupAggregateQuery<OtpSecret>): Map<String, Double?> = api.groupAggregate(input, anyAccessToken, masquerade)
    }
    class AnySessionOneTimePasswordProofApi(val api: Api.OneTimePasswordProofApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?): AuthenticatedOneTimePasswordProofClientEndpoints, OneTimePasswordProofClientEndpoints {
        override suspend fun establishOneTimePassword(input: EstablishOtp): String = api.establishOneTimePassword(input, anyAccessToken, masquerade)
        override suspend fun proveOTP(input: IdentificationAndPassword): Proof = api.proveOTP(input)
    }
    class AnySessionPasswordSecretApi(val api: Api.PasswordSecretApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?): ClientModelRestEndpoints<PasswordSecret, UUID> {
        override suspend fun default(): PasswordSecret = api.default(anyAccessToken, masquerade)
        override suspend fun permissions(): ModelPermissions<PasswordSecret> = api.permissions(anyAccessToken, masquerade)
        override suspend fun query(input: Query<PasswordSecret>): List<PasswordSecret> = api.query(input, anyAccessToken, masquerade)
        override suspend fun queryPartial(input: QueryPartial<PasswordSecret>): List<Partial<PasswordSecret>> = api.queryPartial(input, anyAccessToken, masquerade)
        override suspend fun detail(id: UUID): PasswordSecret = api.detail(id, anyAccessToken, masquerade)
        override suspend fun insertBulk(input: List<PasswordSecret>): List<PasswordSecret> = api.insertBulk(input, anyAccessToken, masquerade)
        override suspend fun insert(input: PasswordSecret): PasswordSecret = api.insert(input, anyAccessToken, masquerade)
        override suspend fun upsert(id: UUID, input: PasswordSecret): PasswordSecret = api.upsert(id, input, anyAccessToken, masquerade)
        override suspend fun bulkReplace(input: List<PasswordSecret>): List<PasswordSecret> = api.bulkReplace(input, anyAccessToken, masquerade)
        override suspend fun replace(id: UUID, input: PasswordSecret): PasswordSecret = api.replace(id, input, anyAccessToken, masquerade)
        override suspend fun bulkModify(input: MassModification<PasswordSecret>): Int = api.bulkModify(input, anyAccessToken, masquerade)
        override suspend fun modifyWithDiff(id: UUID, input: Modification<PasswordSecret>): EntryChange<PasswordSecret> = api.modifyWithDiff(id, input, anyAccessToken, masquerade)
        override suspend fun modify(id: UUID, input: Modification<PasswordSecret>): PasswordSecret = api.modify(id, input, anyAccessToken, masquerade)
        suspend fun simplifiedModify(id: UUID, input: Partial<PasswordSecret>): PasswordSecret = api.simplifiedModify(id, input, anyAccessToken, masquerade)
        override suspend fun bulkDelete(input: Condition<PasswordSecret>): Int = api.bulkDelete(input, anyAccessToken, masquerade)
        override suspend fun delete(id: UUID): Unit = api.delete(id, anyAccessToken, masquerade)
        override suspend fun count(input: Condition<PasswordSecret>): Int = api.count(input, anyAccessToken, masquerade)
        override suspend fun groupCount(input: GroupCountQuery<PasswordSecret>): Map<String, Int> = api.groupCount(input, anyAccessToken, masquerade)
        override suspend fun aggregate(input: AggregateQuery<PasswordSecret>): Double? = api.aggregate(input, anyAccessToken, masquerade)
        override suspend fun groupAggregate(input: GroupAggregateQuery<PasswordSecret>): Map<String, Double?> = api.groupAggregate(input, anyAccessToken, masquerade)
    }
    class AnySessionPasswordProofApi(val api: Api.PasswordProofApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?): AuthenticatedPasswordProofClientEndpoints, PasswordProofClientEndpoints {
        override suspend fun establishPassword(input: EstablishPassword): Unit = api.establishPassword(input, anyAccessToken, masquerade)
        override suspend fun provePasswordOwnership(input: IdentificationAndPassword): Proof = api.provePasswordOwnership(input)
    }
    class AnySessionUserAuthApi(val api: Api.UserAuthApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?): UserAuthClientEndpoints<UUID> {
        override suspend fun logIn(input: List<Proof>): IdAndAuthMethods<UUID> = api.logIn(input)
        override suspend fun logInV2(input: LogInRequest): IdAndAuthMethods<UUID> = api.logInV2(input)
        override suspend fun checkProofs(input: List<Proof>): ProofsCheckResult<UUID> = api.checkProofs(input)
        override suspend fun openSession(input: String): String = api.openSession(input)
        override suspend fun getToken(input: OauthTokenRequest): OauthResponse = api.getToken(input)
        override suspend fun getTokenSimple(input: String): String = api.getTokenSimple(input)
    }
    class AnySessionUserSessionApi(val api: Api.UserSessionApi,val anyToken:String, val anyAccessToken: suspend () -> String, val masquerade: String?) {
    }
}

