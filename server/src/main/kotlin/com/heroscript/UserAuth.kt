package com.heroscript

import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.http.post
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lightningserver.sessions.proofs.*
import com.lightningkite.lightningserver.sessions.proofs.extensions.constrainAttemptRate
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.typed.sdk.module
import com.heroscript.data.ClinicMembershipEndpoints
import com.heroscript.data.UserEndpoints
import com.heroscript.data.UserEndpoints.AppStoreTester
import com.heroscript.data.UserEndpoints.info
import com.lightningkite.services.data.EmailAddress
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.*
import com.lightningkite.services.email.Email
import com.lightningkite.services.email.EmailAddressWithName
import kotlinx.coroutines.flow.toList
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid


object UserAuth : PrincipalType<User, User.ID>, ServerBuilder() {
    // principal fields

    override val subjectSerializer: KSerializer<User> = User.serializer()
    override val idSerializer: KSerializer<User.ID> = User.ID.serializer()

    context(server: ServerRuntime)
    override suspend fun fetch(id: User.ID): User = User.info.table().get(id) ?: throw NotFoundException()

    context(server: ServerRuntime)
    override suspend fun fetchByProperty(property: String, value: String): User? = when (property) {
        "email" -> User.info.table().run {
            findOne(condition { it.email eq value.toEmailAddress() })
                ?: insertOne(User(email = value.toEmailAddress(), firstName = "", lastName = ""))
        }

        else -> super.fetchByProperty(property, value)
    }

    override val precache: List<AuthCacheKey<User, *>> = listOf(RoleCache, ClinicMembershipsCache, CoClinicUsersCache)


    // caching

    object RoleCache : AuthCacheKey<User, UserRole> {
        override val id: String = "role"
        override val serializer: KSerializer<UserRole> = kotlinx.serialization.serializer()
        override val expireAfter: Duration = 5.minutes

        context(_: ServerRuntime)
        override suspend fun calculate(input: Authentication<User>): UserRole = input.fetch().role

        context(_: ServerRuntime)
        suspend fun Authentication<User>.userRole() = get(RoleCache)
        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.userRole() = auth.userRole()
    }

    @Serializable
    data class SimplifiedClinicMembership(
        val _id: ClinicMembership.ID,
        val clinic: Clinic.ID,
        val role: ClinicRole,
    )

    object ClinicMembershipsCache : AuthCacheKey<User, Set<SimplifiedClinicMembership>> {
        override val id: String = "clinicMemberships"
        override val serializer: KSerializer<Set<SimplifiedClinicMembership>> =
            SetSerializer(SimplifiedClinicMembership.serializer())
        override val expireAfter: Duration = 5.minutes

        context(_: ServerRuntime)
        override suspend fun calculate(input: Authentication<User>): Set<SimplifiedClinicMembership> {
            return ClinicMembershipEndpoints.info.table()
                .find(condition {
                    (it.user eq input.id) and
                    (it.deactivatedAt eq null) and
                    (it.acceptedAt neq null)
                })
                .toList()
                .map { SimplifiedClinicMembership(it._id, it.clinic, it.role) }
                .toSet()
        }

        context(_: ServerRuntime)
        suspend fun Authentication<User>.clinicMemberships() = get(ClinicMembershipsCache)
        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.clinicMemberships() = auth.clinicMemberships()

        context(_: ServerRuntime)
        suspend fun Authentication<User>.clinicIds(): Set<Clinic.ID> =
            clinicMemberships().map { it.clinic }.toSet()
        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.clinicIds(): Set<Clinic.ID> = auth.clinicIds()

        context(_: ServerRuntime)
        suspend fun Authentication<User>.clinicAdminIds(): Set<Clinic.ID> =
            clinicMemberships().filter { it.role == ClinicRole.ClinicAdmin }.map { it.clinic }.toSet()
        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.clinicAdminIds(): Set<Clinic.ID> = auth.clinicAdminIds()

        // ClinicAdmin is administrative-only and not DEA-licensed by definition;
        // only ClinicRole.Prescriber may sign off on prescription submissions.
        context(_: ServerRuntime)
        suspend fun Authentication<User>.prescriberClinicIds(): Set<Clinic.ID> =
            clinicMemberships().filter { it.role == ClinicRole.Prescriber }
                .map { it.clinic }.toSet()
        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.prescriberClinicIds(): Set<Clinic.ID> = auth.prescriberClinicIds()
    }

    object CoClinicUsersCache : AuthCacheKey<User, Set<User.ID>> {
        override val id: String = "coClinicUsers"
        override val serializer: KSerializer<Set<User.ID>> = SetSerializer(User.ID.serializer())
        override val expireAfter: Duration = 5.minutes

        context(_: ServerRuntime)
        override suspend fun calculate(input: Authentication<User>): Set<User.ID> {
            val myClinics = input.get(ClinicMembershipsCache).map { it.clinic }.toSet()
            if (myClinics.isEmpty()) return emptySet()
            return ClinicMembershipEndpoints.info.table()
                .find(condition {
                    (it.clinic inside myClinics) and
                    (it.deactivatedAt eq null) and
                    (it.acceptedAt neq null)
                })
                .toList()
                .map { it.user }
                .toSet()
        }

        context(_: ServerRuntime)
        suspend fun Authentication<User>.coClinicUsers() = get(CoClinicUsersCache)
        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.coClinicUsers() = auth.coClinicUsers()
    }

    @Suppress("UNUSED_PARAMETER")
    fun hasFeature(flag: FeatureFlag): Boolean = false

    private val proofs = path.path("proof")

    val pins = PinHandler(Server.cache, "pins")

    val email = proofs.path("email") module EmailEndpoints(pins)
    val totp = proofs.path("totp") module TimeBasedOTPProofEndpoints(Server.database, Server.cache)
    val password = proofs.path("password") module PasswordProofEndpoints(Server.database, Server.cache)
    val backupCodes = proofs.path("backup-codes") module BackupCodeEndpoints(Server.database, Server.cache)
    val session = path.path("session") include SessionEndpoints()

    class EmailEndpoints(val pins: PinHandler) : ServerBuilder() {
        val proof = path include EmailProofEndpoints(
            pin = pins,
            email = Server.email,
            emailTemplate = { to, pin ->
                val user = User.info.table().findOne(condition { it.email eq to.toEmailAddress() })
                val greeting = user?.displayName?.takeIf { it.isNotBlank() }
                Email(
                    subject = "Log In Code",
                    to = listOf(EmailAddressWithName(to)),
                    html = {
                        emailBase {
                            header("Log In Code")
                            paragraph(
                                buildString {
                                    if (greeting != null) appendLine("Hi $greeting,")
                                    append("Your log in code is:")
                                }
                            )
                            code(pin)
                            paragraph("If you did not request this code, you can safely ignore this email.")
                        }
                    }
                )
            }
        )

        val verifyNewEmail = path.path("verify-new-email").post bind ApiHttpHandler(
            summary = "Verify New Email",
            description = "Sends a verification passcode to a new email.",
            auth = UserAuth.require(),
            implementation = { newEmail: EmailAddress ->
                val self = auth.fetch()

                pins.cache().constrainAttemptRate("email-pin-count-${newEmail}") {
                    val p = pins.establish(newEmail.raw)

                    Server.email().send(
                        Email(
                            subject = "New Email Verification",
                            to = listOf(EmailAddressWithName(newEmail, self.displayName)),
                            html = {
                                emailBase {
                                    header("New Email Verification")
                                    paragraph("Here is your verification passcode,")
                                    code(p.pin)
                                    paragraph("If you did not request this code, you can safely ignore this email.")
                                }
                            }
                        )
                    )

                    p.key
                }
            }
        )
    }

    class SessionEndpoints : AuthEndpoints<User, User.ID>(
        principal = UserAuth,
        database = Server.database,
    ) {
        context(server: ServerRuntime)
        override suspend fun requiredProofStrengthFor(subject: User): Int {
            // AppStoreTester
            if (subject._id == User.ID.AppStoreTester) return 10

            val methods = server.proofMethods
                .filter { it.established(UserAuth, subject) }
                .filter { it.info.via != UserAuth.backupCodes.info.via }

            return if (methods.size > 1) 20 else 10
        }

        context(server: ServerRuntime)
        override suspend fun sessionExpiration(subject: User): Instant? = null

        context(server: ServerRuntime)
        override suspend fun sessionStaleAfter(subject: User): Duration? = null

        context(_: ServerRuntime)
        suspend fun createSession(
            subjectId: User.ID,
            label: String? = null,
            expires: Instant? = null,
            stale: Instant? = null,
            scopes: Set<GrantedScope> = setOf(GrantedScope.root),
            oauthClient: String? = null,
            derivedFrom: Uuid? = null,
        ): Pair<Session<User, User.ID>, RefreshToken> {
            return newSession(
                subjectId = subjectId,
                label = label,
                expires = expires,
                stale = stale,
                scopes = scopes,
                oauthClient = oauthClient,
                derivedFrom = derivedFrom,
            )
        }
    }
}
