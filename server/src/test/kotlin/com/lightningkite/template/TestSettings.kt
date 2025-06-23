package com.lightningkite.template

import com.lightningkite.lightningserver.engine.UnitTestEngine
import com.lightningkite.lightningserver.engine.engine
import com.lightningkite.lightningserver.settings.GeneralServerSettings
import com.lightningkite.lightningserver.settings.Settings
import com.lightningkite.lightningserver.settings.generalSettings

object TestSettings {
    init {
        Server
        Settings.populateDefaults(mapOf(
            generalSettings.name to GeneralServerSettings(
                debug = true
            )
        ))
        engine = UnitTestEngine
    }
}

