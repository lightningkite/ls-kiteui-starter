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

@Serializable
enum class MemberRole {
    NoOne,
    Member,
    Admin,
    Owner,
}

@Serializable
enum class UserRole {
    User,
    Admin,
    Developer,
    Root
}