package com.heroscript.views.users

import com.heroscript.*
import com.heroscript.sdk.currentSession
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@kotlinx.serialization.Serializable
enum class DeaStatusFilter { Any, Verified, Pending, Expired }

@OptIn(InternalKiteUi::class)
@Routable("ops/users")
class UserListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Users")
    override var parentPage: Page? = null

    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val roleFilter = Signal<UserRole?>(null)

    @QueryParameter
    val hasPrescriberOnly = Signal(false)

    @QueryParameter
    val deaStatus = Signal(DeaStatusFilter.Any)

    @QueryParameter
    val mfaOnly = Signal(false)

    @QueryParameter
    val clinicNameQuery = Signal("")

    private val isOps = remember {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    /**
     * Resolves the clinic-name filter to a set of user IDs holding an active membership in any
     * matching clinic. Empty filter → null (no clinic-membership constraint applied).
     */
    private val clinicMemberUserIds = remember {
        val session = currentSession() ?: return@remember null
        val q = clinicNameQuery().trim()
        if (q.isBlank()) return@remember null
        val matchingClinics = session.clinics.query(
            Query(condition<Clinic> { it.name.contains(q, ignoreCase = true) })
        )()
        if (matchingClinics.isEmpty()) return@remember emptySet<User.ID>()
        val clinicIds = matchingClinics.map { it._id }.toSet()
        val memberships = session.clinicMemberships.query(
            Query(condition<ClinicMembership> {
                (it.clinic inside clinicIds) and (it.acceptedAt neq null) and (it.deactivatedAt eq null)
            })
        )()
        memberships.map { it.user }.toSet()
    }

    val data = remember {
        val session = currentSession() ?: return@remember null
        val q = search().trim()
        val nameOrEmail = q.takeIf { it.isNotBlank() }?.let { needle ->
            condition<User> { it.firstName.contains(needle, ignoreCase = true) } or
                condition<User> { it.lastName.contains(needle, ignoreCase = true) } or
                condition<User> { it.email.contains(needle, ignoreCase = true) }
        }
        val role = roleFilter()?.let { r -> condition<User> { it.role eq r } }
        val prescriber = hasPrescriberOnly().takeIf { it }?.let {
            condition<User> { it.prescriber neq null }
        }
        val mfa = mfaOnly().takeIf { it }?.let {
            condition<User> { it.mfaEnrolledAt neq null }
        }
        val memberIds = clinicMemberUserIds()
        val membershipFilter = memberIds?.let { ids ->
            if (ids.isEmpty()) condition<User>(false)
            else condition<User> { it._id inside ids }
        }
        session.users.query(
            Query(
                condition = Condition.And(
                    listOfNotNull(nameOrEmail, role, prescriber, mfa, membershipFilter)
                )
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { !isOps() }.padded.col {
                centered.text("This view is restricted to HeroScript Ops.")
            }

            expanding.shownWhen { isOps() }.col {
                filterRow()

                val rawItems = remember { data()?.invoke() ?: emptyList() }

                val filtered = remember {
                    val items = rawItems()
                    when (deaStatus()) {
                        DeaStatusFilter.Any -> items
                        DeaStatusFilter.Verified -> items.filter { it.prescriber?.isDeaVerified == true && it.prescriber?.isDeaExpired == false }
                        DeaStatusFilter.Pending -> items.filter { it.prescriber != null && it.prescriber?.isDeaVerified == false && it.prescriber?.isDeaExpired == false }
                        DeaStatusFilter.Expired -> items.filter { it.prescriber?.isDeaExpired == true }
                    }
                }

                expanding.lazyColumn(
                    items = filtered,
                    id = { it._id },
                    loadMore = {
                        val d = data() ?: return@lazyColumn
                        d.limit = d().size + 20
                        delay(3.seconds)
                    },
                    render = { user ->
                        card.link {
                            ::to {
                                val id = user()._id
                                { UserDetailPage(id) }
                            }
                            userRow(user)
                        }
                    },
                )

                shownWhen { filtered().isEmpty() }.padded.col {
                    centered.text("No users match the current filters.")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            expanding.fieldTheme.row {
                expanding.textInput {
                    hint = "Search name or email"
                    content bind search
                }
                icon(Icon.search, "search")
            }
            card.button {
                icon(Icon.add, "Add user")
                onClick {
                    context.pageNavigator.navigate(
                        UserDetailPage(User.ID(Uuid.random()), startInEditMode = true)
                    )
                }
            }
        }
        row {
            card.col {
                subtext("Role")
                select {
                    bind(
                        edits = roleFilter,
                        data = Constant(listOf<UserRole?>(null) + UserRole.entries.toList()),
                        render = { it?.name ?: "Any" },
                    )
                }
            }
            card.col {
                subtext("DEA")
                select {
                    bind(
                        edits = deaStatus,
                        data = Constant(DeaStatusFilter.entries.toList()),
                        render = { it.name },
                    )
                }
            }
        }
        row {
            card.row {
                centered.checkbox { checked bind hasPrescriberOnly }
                centered.text("Prescriber")
            }
            card.row {
                centered.checkbox { checked bind mfaOnly }
                centered.text("MFA enrolled")
            }
            expanding.fieldTheme.row {
                expanding.textInput {
                    hint = "Member of clinic (name)"
                    content bind clinicNameQuery
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.userRow(user: Reactive<User>) = col {
        row {
            expanding.h4 {
                ::content {
                    val u = user()
                    u.displayName.trim().ifBlank { u.email.raw }
                }
            }
            subtext { ::content { user().role.name } }
        }
        row {
            subtext { ::content { user().email.raw } }
            shownWhen { user().prescriber != null }.subtext("Prescriber")
            shownWhen { user().prescriber?.isDeaExpired == true }.subtext("DEA expired")
            shownWhen {
                val p = user().prescriber
                p != null && p.isDeaVerified && !p.isDeaExpired
            }.subtext("DEA verified")
            shownWhen {
                val p = user().prescriber
                p != null && !p.isDeaVerified && !p.isDeaExpired
            }.subtext("DEA pending")
        }
        row {
            subtext {
                ::content {
                    user().lastLoginAt?.let { "Last login $it" } ?: "Never logged in"
                }
            }
            subtext { ::content { if (user().isActive) "Active" else "Deactivated" } }
        }
    }
}
