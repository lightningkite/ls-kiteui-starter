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
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ExpirationWindow(val label: String, val days: Int) {
    Sixty("60 days", 60),
    Thirty("30 days", 30),
    Seven("7 days", 7),
}

/**
 * Urgency sort: already-expired first, then expiring-soon (sooner first), then pending verification.
 */
private enum class UrgencyBucket { Expired, ExpiringSoon, Pending }

private data class QueueItem(
    val user: User,
    val bucket: UrgencyBucket,
    val daysUntilExpiration: Long,
    val deaPending: Boolean,
    val statePending: Boolean,
)

@OptIn(InternalKiteUi::class)
@Routable("ops/dea-queue")
class DeaQueuePage : PageWithParent {
    override val title: Reactive<String> get() = Constant("DEA Queue")
    override var parentPage: Page? = null

    @QueryParameter
    val showDeaPending = Signal(true)

    @QueryParameter
    val showStatePending = Signal(true)

    @QueryParameter
    val showExpiring = Signal(true)

    @QueryParameter
    val expirationWindow = Signal(ExpirationWindow.Sixty)

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    // Server-narrows on prescriber != null. The `any stateLicenses[].review == null` predicate
    // is a nested-collection condition that the path DSL doesn't express cleanly; we fetch all
    // prescribers and filter client-side. Acceptable while the prescriber population is small.
    // TODO: push state-license-pending filter server-side once dataset growth makes this impractical.
    private val prescribers = remember {
        val session = currentSession() ?: return@remember null
        session.users.query(
            Query(condition<User> { it.prescriber neq null })
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { !isOps() }.padded.col {
                centered.text("This view is restricted to HeroScript Ops.")
            }

            expanding.shownWhen { isOps() }.col {
                filterRow()

                val items = remember {
                    val raw = prescribers()?.invoke() ?: return@remember emptyList()
                    val nowInstant = Clock.System.now()
                    val window = expirationWindow().days.days
                    val deaOn = showDeaPending()
                    val stateOn = showStatePending()
                    val expOn = showExpiring()

                    raw.mapNotNull { user ->
                        val p = user.prescriber ?: return@mapNotNull null
                        val deaPending = p.deaReview == null
                        val statePending = p.stateLicenses.any { it.review == null }
                        val expiresIn = p.deaExpiration - nowInstant
                        val expired = p.deaExpiration < nowInstant
                        val expiringSoon = !expired && expiresIn <= window

                        val matchDea = deaOn && deaPending
                        val matchState = stateOn && statePending
                        val matchExp = expOn && (expired || expiringSoon)
                        if (!(matchDea || matchState || matchExp)) return@mapNotNull null

                        val bucket = when {
                            expired -> UrgencyBucket.Expired
                            expiringSoon -> UrgencyBucket.ExpiringSoon
                            else -> UrgencyBucket.Pending
                        }
                        val daysUntil = expiresIn.inWholeDays
                        QueueItem(user, bucket, daysUntil, deaPending, statePending)
                    }.sortedWith(
                        compareBy({ it.bucket.ordinal }, { it.daysUntilExpiration }),
                    )
                }

                col {
                    reactive {
                        clearChildren()
                        val list = items()
                        if (list.isEmpty()) {
                            padded.col { centered.text("No verifications pending in this window.") }
                        } else {
                            list.forEach { item ->
                                card.link {
                                    to = { UserDetailPage(item.user._id) }
                                    queueRow(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            card.row {
                centered.checkbox { checked bind showDeaPending }
                centered.text("DEA pending")
            }
            card.row {
                centered.checkbox { checked bind showStatePending }
                centered.text("State license pending")
            }
        }
        row {
            card.row {
                centered.checkbox { checked bind showExpiring }
                centered.text("Expiring within")
            }
            card.col {
                subtext("Window")
                select {
                    bind(
                        edits = expirationWindow,
                        data = Constant(ExpirationWindow.entries.toList()),
                        render = { it.label },
                    )
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.queueRow(item: QueueItem) = col {
        val user = item.user
        val p = user.prescriber!!

        row {
            expanding.h4(user.displayName.trim().ifBlank { user.email.raw })
            subtext(item.bucket.label())
        }
        subtext(user.email.raw)

        val clinicNames = rememberSuspending {
            val s = currentSession() ?: return@rememberSuspending ""
            val memberships = s.clinicMemberships.query(
                Query(condition<ClinicMembership> {
                    (it.user eq user._id) and (it.acceptedAt neq null) and (it.deactivatedAt eq null)
                })
            )()
            val names = memberships.mapNotNull { m -> s.clinics[m.clinic]()?.name }
            val joined = names.joinToString(", ")
            if (joined.length > 80) joined.take(77) + "..." else joined
        }
        subtext { ::content { clinicNames().ifBlank { "No active clinic memberships" } } }

        row {
            subtext("DEA ${p.deaNumber}")
            subtext(expirationLabel(item.daysUntilExpiration))
        }

        row {
            shownWhen { item.deaPending }.subtext("DEA pending verification")
            shownWhen { item.statePending }.subtext("State license pending verification")
        }

        row {
            // License image is a ServerFile location; full inline preview is deferred.
            // TODO: render thumbnail inline once a reusable image preview component exists.
            subtext("License image: ${p.deaLicenseImage.location}")
        }

        row {
            // User.updatedAt is the closest proxy for "submitted at"; the prescriber subfield
            // doesn't have its own timestamp.
            // TODO: add a dedicated prescriberSubmittedAt field if reviewer SLA reporting needs it.
            subtext("Last updated ${user.updatedAt}")
        }

        row {
            atEnd.important.button {
                text("Open verification")
                // TODO: add ?focus=dea param to UserDetailPage so the DEA card is pre-scrolled / pre-opened.
                onClick { context.pageNavigator.navigate(UserDetailPage(user._id)) }
            }
        }
    }

    private fun UrgencyBucket.label(): String = when (this) {
        UrgencyBucket.Expired -> "Expired"
        UrgencyBucket.ExpiringSoon -> "Expiring soon"
        UrgencyBucket.Pending -> "Pending verification"
    }

    private fun expirationLabel(daysUntil: Long): String = when {
        daysUntil < 0 -> "expired ${-daysUntil} days ago"
        daysUntil == 0L -> "expires today"
        else -> "expires in $daysUntil days"
    }
}
