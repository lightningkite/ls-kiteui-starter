package com.heroscript.views.catalog

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
@Routable("catalog")
class CatalogListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Catalog")
    override var parentPage: Page? = null

    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val controlledOnly = Signal(false)

    @QueryParameter
    val activeOnly = Signal(true)

    @QueryParameter
    val hasFormOnly = Signal(false)

    private val isOps = rememberSuspending {
        (currentSession()?.self?.invoke()?.role ?: UserRole.User) >= UserRole.Admin
    }

    val data = remember {
        val session = currentSession() ?: return@remember null
        val q = search().trim()
        val nameSearch = q.takeIf { it.isNotBlank() }?.let { needle ->
            condition<Product> { it.name.contains(needle, ignoreCase = true) }
        }
        session.products.query(
            Query(
                condition = Condition.And(
                    listOfNotNull(
                        nameSearch,
                        controlledOnly().takeIf { it }?.let { condition<Product> { it.controlled eq true } },
                        activeOnly().takeIf { it }?.let { condition<Product> { it.active eq true } },
                    )
                )
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            row {
                expanding.fieldTheme.row {
                    expanding.textInput {
                        hint = "Search name"
                        content bind search
                    }
                    icon(Icon.search, "search")
                }
                card.row {
                    centered.checkbox { checked bind controlledOnly }
                    centered.text("Controlled")
                }
                card.row {
                    centered.checkbox { checked bind activeOnly }
                    centered.text("Active")
                }
                card.row {
                    centered.checkbox { checked bind hasFormOnly }
                    centered.text("Has form")
                }
                shownWhen { isOps() }.card.button {
                    icon(Icon.add, "Add product")
                    onClick {
                        context.pageNavigator.navigate(
                            CatalogDetailPage(Product.ID(Uuid.random()), startInEditMode = true)
                        )
                    }
                }
            }

            val items = remember {
                val raw = data()?.invoke() ?: emptyList()
                if (hasFormOnly()) raw.filter { it.forms.isNotEmpty() } else raw
            }

            expanding.lazyColumn(
                items = items,
                id = { it._id },
                loadMore = {
                    val d = data() ?: return@lazyColumn
                    d.limit = d().size + 20
                    delay(3.seconds)
                },
                render = { product ->
                    card.link {
                        ::to {
                            val id = product()._id
                            { CatalogDetailPage(id) }
                        }
                        col {
                            row {
                                expanding.h4 { ::content { product().name } }
                                shownWhen { product().controlled }.subtext("Controlled")
                                shownWhen { !product().active }.subtext("Inactive")
                            }
                            row {
                                subtext { ::content { "${product().forms.size} forms" } }
                                subtext {
                                    val pid = remember { product()._id }
                                    val count = rememberSuspending {
                                        val session = currentSession() ?: return@rememberSuspending 0
                                        session.productPharmacyMappings.skipCache.count(
                                            condition<ProductPharmacyMapping> {
                                                (it.product eq pid()) and (it.active eq true)
                                            }
                                        )
                                    }
                                    ::content { "${count()} pharmacies" }
                                }
                            }
                        }
                    }
                }
            )

            shownWhen { items().isEmpty() }.padded.col {
                centered.text("No products match the current filters.")
            }
        }
    }
}
