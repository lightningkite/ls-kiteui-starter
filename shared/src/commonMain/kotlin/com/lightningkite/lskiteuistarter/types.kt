package com.lightningkite.lskiteuistarter

import kotlinx.serialization.Serializable

@Serializable
enum class AppPlatform {
    iOS,
    Android,
    Web,
    Desktop,
    ;

    companion object
}