package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.NotFoundException
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.http.post
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.sessions.AuthEndpoints
import com.lightningkite.lightningserver.sessions.proofs.*
import com.lightningkite.lightningserver.sessions.proofs.extensions.constrainAttemptRate
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.typed.sdk.module
import com.lightningkite.lskiteuistarter.data.MembershipEndpoints
import com.lightningkite.lskiteuistarter.data.UserEndpoints.AppStoreTester
import com.lightningkite.lskiteuistarter.data.UserEndpoints.info
import com.lightningkite.services.data.EmailAddress
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.*
import com.lightningkite.services.email.Email
import com.lightningkite.services.email.EmailAddressWithName
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant


object UserAuth : PrincipalType<User, User.ID>, ServerBuilder() {

    // ----- Principal Fields -----

    override val subjectSerializer: KSerializer<User> = User.serializer()
    override val idSerializer: KSerializer<User.ID> = User.ID.serializer()

    context(server: ServerRuntime)
    override suspend fun fetch(id: User.ID): User = User.info.table().get(id) ?: throw NotFoundException()

    context(server: ServerRuntime)
    override suspend fun fetchByProperty(property: String, value: String): User? = when (property) {
        "email" -> User.info.table().run {
            findOne(condition { it.email eq value.toEmailAddress() })
                // This is optional. If you want signing in and signing up to be the same thing this is how you do it
                ?: insertOne(User(email = value.toEmailAddress(), name = ""))
        }

        else -> super.fetchByProperty(property, value)
    }

    override val precache: List<AuthCacheKey<User, *>> = listOf(RoleCache, MembershipsCache)

    override fun normalizePropertyValue(property: String, value: String): String = when (property) {
        "email" -> email.proof.normalize(value)

        else -> super.normalizePropertyValue(property, value)
    }

    // ----- Caching ------

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
    data class SimplifiedMembership(
        val _id: Membership.ID,
        val organization: Organization.ID,
        val role: MemberRole,
    )

    object MembershipsCache : AuthCacheKey<User, Set<SimplifiedMembership>> {
        override val id: String = "memberships"
        override val serializer: KSerializer<Set<SimplifiedMembership>> =
            SetSerializer(SimplifiedMembership.serializer())
        override val expireAfter: Duration = 5.minutes

        context(_: ServerRuntime)
        override suspend fun calculate(input: Authentication<User>): Set<SimplifiedMembership> {
            return MembershipEndpoints.info.table()
                .find(condition { (it.user eq input.id) and (it.deactivatedAt eq null) })
                .toList()
                .map { SimplifiedMembership(it._id, it.organization, it.role) }
                .toSet()
        }

        context(_: ServerRuntime)
        suspend fun Authentication<User>.memberships() = get(MembershipsCache)

        context(_: ServerRuntime)
        suspend fun AuthAccess<User>.memberships() = auth.memberships()
    }

    // ----- Proofs -----

    private val proofs = path.path("proof")

    val pins = PinHandler(Server.cache, "pins")

    val email = proofs.path("email") module EmailEndpoints(pins)
    val totp = proofs.path("totp") module TimeBasedOTPProofEndpoints(
        Server.database,
        Server.cache,
        proofExpiration = 10.minutes
    )
    val password = proofs.path("password") module PasswordProofEndpoints(
        Server.database,
        Server.cache,
        proofExpiration = 10.minutes
    )
    val backupCodes = proofs.path("backup-codes") module BackupCodeEndpoints(
        Server.database,
        Server.cache,
        proofExpiration = 10.minutes
    )

    class EmailEndpoints(val pins: PinHandler) : ServerBuilder() {
        val proof = path include EmailProofEndpoints(
            pin = pins,
            email = Server.email,
            emailTemplate = { to, pin ->
                val name = User.info.table().findOne(condition { it.email eq to })?.name
                Email(
                    subject = "Log In Code",
                    to = listOf(EmailAddressWithName(to)),
                    html = {
                        emailBase {
                            header("Log In Code")
                            paragraph(
                                buildString {
                                    if (name != null) appendLine("Hi $name,")
                                    append("Your log in code is:")
                                }
                            )
                            code(pin)
                            paragraph("If you did not request this code, you can safely ignore this email.")
                        }
                    }
                )
            },
            proofExpiration = 10.minutes,
            screenEmail = { email ->

                // Here you can prevent emails from being sent out to emails that are not associated with a user in the system
                // If you want to separate out signing in and signing up this is how you would do it.
                // If you want to do any other kind of screening, such as the domain you can do it here.
                // ScreeningResult.Reject is what you use when you want to return an explicit denial message and 400.
                /*if(User.info.table().findOne(condition { it.email eq email }) == null)
                    PinBasedProofEndpoints.ScreeningResult.Suppress
                else
                    PinBasedProofEndpoints.ScreeningResult.Allow*/

                PinBasedProofEndpoints.ScreeningResult.Allow
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
                            to = listOf(EmailAddressWithName(newEmail, self.name)),
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

    // ----- Session Configuration and Endpoints -----

    val session = path.path("session") include SessionEndpoints()

    class SessionEndpoints : AuthEndpoints<User, User.ID>(
        principal = UserAuth,
        database = Server.database,
        cache = Server.cache,
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
        override suspend fun sessionExpiration(subject: User): Instant = now() + 365.days

        context(server: ServerRuntime)
        override suspend fun sessionStaleAfter(subject: User): Duration = 7.days

        context(server: ServerRuntime)
        override suspend fun permitAuthentication(subject: User): Boolean {
            // This is where you can prevent a user from logging in because they are disabled or any other reason you want
            return super.permitAuthentication(subject)
        }

    }
}
