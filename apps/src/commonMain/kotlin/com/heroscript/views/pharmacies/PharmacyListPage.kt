package com.heroscript.views.pharmacies

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
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(InternalKiteUi::class)
@Routable("ops/pharmacies")
class PharmacyListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Pharmacies")
    override var parentPage: Page? = null

    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val adapterFilter = Signal<PharmacyAdapterType?>(null)

    @QueryParameter
    val activeOnly = Signal(true)

    @QueryParameter
    val stateFilter = Signal("")

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    val data = remember {
        val session = currentSession() ?: return@remember null
        val q = search().trim()
        val nameSearch = q.takeIf { it.isNotBlank() }?.let { needle ->
            condition<Pharmacy> { it.name.contains(needle, ignoreCase = true) }
        }
        session.pharmacies.query(
            Query(
                condition = Condition.And(
                    listOfNotNull(
                        nameSearch,
                        adapterFilter()?.let { type -> condition<Pharmacy> { it.adapterType eq type } },
                        activeOnly().takeIf { it }?.let {
                            condition<Pharmacy> { it.deactivatedAt eq null }
                        },
                    )
                )
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { !isOps() }.card.col {
                h3("Ops access required")
                text("This view is restricted to HeroScript Operations.")
            }

            expanding.shownWhen { isOps() }.col {
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
                        icon(Icon.add, "Add pharmacy")
                        onClick {
                            context.pageNavigator.navigate(
                                PharmacyDetailPage(Pharmacy.ID(Uuid.random()), startInEditMode = true)
                            )
                        }
                    }
                }
                row {
                    card.col {
                        subtext("Adapter")
                        select {
                            bind(
                                edits = adapterFilter,
                                data = Constant(listOf<PharmacyAdapterType?>(null) + PharmacyAdapterType.entries.toList()),
                                render = { it?.name ?: "Any" },
                            )
                        }
                    }
                    expanding.fieldTheme.row {
                        expanding.textInput {
                            hint = "Ships to state (e.g. UT)"
                            content bind stateFilter
                        }
                    }
                }

                val rawItems = remember { data()?.invoke() ?: emptyList() }

                val filtered = remember {
                    val items = rawItems()
                    val state = stateFilter().trim().uppercase()
                    if (state.isBlank()) items
                    else items.filter { p -> p.states.any { it.state.uppercase() == state } }
                }

                expanding.lazyColumn(
                    items = filtered,
                    id = { it._id },
                    loadMore = {
                        val d = data() ?: return@lazyColumn
                        d.limit = d().size + 20
                        delay(3.seconds)
                    },
                    render = { pharmacy ->
                        card.link {
                            ::to {
                                val id = pharmacy()._id
                                { PharmacyDetailPage(id) }
                            }
                            col {
                                row {
                                    expanding.h4 { ::content { pharmacy().name } }
                                    subtext { ::content { pharmacy().adapterType.name } }
                                }
                                row {
                                    subtext {
                                        ::content {
                                            if (pharmacy().isActive) "Active" else "Inactive"
                                        }
                                    }
                                    subtext { ::content { pharmacy().contactEmail.raw } }
                                    subtext { ::content { "${pharmacy().states.size} states" } }
                                }
                            }
                        }
                    }
                )

                shownWhen { filtered().isEmpty() }.padded.col {
                    centered.text("No pharmacies match the current filters.")
                }
            }
        }
    }
}
