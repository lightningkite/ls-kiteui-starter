rootProject.name = "ls-kiteui-starter"

pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.google.devtools.ksp") version kspVersion
    }
}


include(":apps")
include(":server")
include(":shared")
