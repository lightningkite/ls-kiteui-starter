package com.heroscript.views.patients

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
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalKiteUi::class)
@Routable("patients")
class PatientListPage : PageWithParent {
    override val title: Reactive<String> get() = Constant("Patients")
    override var parentPage: Page? = null

    @QueryParameter
    val search = Signal("")

    @QueryParameter
    val unverifiedOnly = Signal(false)

    val data = remember {
        val session = currentSession() ?: return@remember null
        val clinicId = activeClinic() ?: return@remember null
        val q = search().trim()
        val nameSearch = q.takeIf { it.isNotBlank() }?.let { needle ->
            condition<Patient> { it.firstName.contains(needle, ignoreCase = true) } or
                condition<Patient> { it.lastName.contains(needle, ignoreCase = true) }
        }
        session.patients.query(
            Query(
                condition = Condition.And(
                    listOfNotNull(
                        condition<Patient> { it.clinic eq clinicId },
                        nameSearch,
                        unverifiedOnly().takeIf { it }?.let {
                            condition<Patient> { it.shippingAddress.verifiedAt eq null }
                        },
                    )
                )
            )
        )
    }

    override fun ElementWriter.CanAddTheme.render() {
        col {
            shownWhen { activeClinic() == null }.card.col {
                h3("No active clinic")
                text("Patients are scoped to a clinic. Accept a clinic membership invite to view patients.")
            }

            expanding.shownWhen { activeClinic() != null }.col {
                row {
                    expanding.fieldTheme.row {
                        expanding.textInput {
                            hint = "Search name"
                            content bind search
                        }
                        icon(Icon.search, "search")
                    }
                    card.row {
                        centered.checkbox { checked bind unverifiedOnly }
                        centered.text("Unverified only")
                    }
                    card.button {
                        icon(Icon.add, "Add patient")
                        onClick {
                            context.pageNavigator.navigate(PatientDetailPage(newPatientId(), startInEditMode = true))
                        }
                    }
                }

                val items = remember { data()?.invoke() ?: emptyList() }

                expanding.lazyColumn(
                    items = items,
                    id = { it._id },
                    loadMore = {
                        val d = data() ?: return@lazyColumn
                        d.limit = d().size + 20
                        delay(3.seconds)
                    },
                    render = { patient ->
                        card.link {
                            ::to {
                                val id = patient()._id
                                { PatientDetailPage(id) }
                            }
                            col {
                                row {
                                    expanding.h4 { ::content { patient().displayName } }
                                    subtext {
                                        ::content {
                                            if (patient().shippingAddress.verifiedAt == null) "Unverified" else "Verified"
                                        }
                                    }
                                }
                                subtext {
                                    ::content { "${patient().gender.label()} · DOB ${patient().dateOfBirth}" }
                                }
                                row {
                                    shownWhen { patient().smsConsent != null }.subtext("SMS")
                                    shownWhen { patient().emailConsent != null }.subtext("Email")
                                }
                            }
                        }
                    }
                )

                shownWhen { items().isEmpty() }.padded.col {
                    centered.text("No patients match the current filters.")
                }
            }
        }
    }
}

internal fun Gender.label(): String = when (this) {
    Gender.M -> "Male"
    Gender.F -> "Female"
    Gender.A -> "Another"
    Gender.U -> "Unknown"
}

internal fun newPatientId(): Patient.ID = Patient.ID(kotlin.uuid.Uuid.random())

internal fun today(): LocalDate =
    kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
