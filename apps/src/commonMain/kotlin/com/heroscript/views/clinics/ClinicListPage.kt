package com.heroscript.views.clinics

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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(InternalKiteUi::class)
@Routable("ops/clinics")
class ClinicListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Clinics")
    override var parentPage: Page? = null

    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val activeOnly = Signal(true)

    @QueryParameter
    val stateFilter = Signal("")

    private val isOps = remember {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    val data = remember {
        val session = currentSession() ?: return@remember null
        val q = search().trim()
        val nameSearch = q.takeIf { it.isNotBlank() }?.let { needle ->
            condition<Clinic> { it.name.contains(needle, ignoreCase = true) }
        }
        val stateRaw = stateFilter().trim().uppercase().take(2)
        val stateCondition = stateRaw.takeIf { it.length == 2 }?.let {
            condition<Clinic> { it.primaryAddress.address.state eq stateRaw }
        }
        val activeFilter = activeOnly().takeIf { it }?.let {
            condition<Clinic> { it.deactivatedAt eq null }
        }
        session.clinics.query(
            Query(
                condition = Condition.And(
                    listOfNotNull(nameSearch, stateCondition, activeFilter)
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

                val items = remember { data()?.invoke() ?: emptyList() }

                expanding.lazyColumn(
                    items = items,
                    id = { it._id },
                    loadMore = {
                        val d = data() ?: return@lazyColumn
                        d.limit = d().size + 20
                        delay(3.seconds)
                    },
                    render = { clinic ->
                        card.link {
                            ::to {
                                val id = clinic()._id
                                { ClinicDetailPage(id) }
                            }
                            clinicRow(clinic)
                        }
                    },
                )

                shownWhen { items().isEmpty() }.padded.col {
                    centered.text("No clinics match the current filters.")
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.filterRow() = col {
        row {
            expanding.fieldTheme.row {
                expanding.textInput {
                    hint = "Search name"
                    content bind search
                }
                icon(Icon.search, "search")
            }
            card.row {
                centered.checkbox { checked bind activeOnly }
                centered.text("Active only")
            }
            card.button {
                icon(Icon.add, "Add clinic")
                onClick {
                    context.pageNavigator.navigate(
                        ClinicDetailPage(Clinic.ID(Uuid.random()), startInEditMode = true)
                    )
                }
            }
        }
        row {
            card.col {
                subtext("State")
                textInput {
                    hint = "e.g. CA"
                    content bind stateFilter
                }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.clinicRow(clinic: Reactive<Clinic>) = col {
        val memberCount = rememberSuspending {
            val session = currentSession() ?: return@rememberSuspending 0
            val cid = clinic()._id
            session.clinicMemberships.skipCache.count(
                condition<ClinicMembership> {
                    (it.clinic eq cid) and (it.acceptedAt neq null) and (it.deactivatedAt eq null)
                }
            )
        }

        row {
            expanding.h4 { ::content { clinic().name.ifBlank { "(unnamed clinic)" } } }
            subtext {
                ::content { if (clinic().isActive) "Active" else "Deactivated" }
            }
        }
        subtext {
            ::content {
                val a = clinic().primaryAddress.address
                if (a.city.isBlank() && a.state.isBlank()) "—" else "${a.city}, ${a.state}"
            }
        }
        row {
            subtext { ::content { clinic().billingContactName.ifBlank { "(no billing contact)" } } }
            subtext { ::content { "${memberCount()} members" } }
        }
    }
}
