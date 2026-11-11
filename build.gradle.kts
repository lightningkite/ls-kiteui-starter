
plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.graalVmNative) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
}

buildscript {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
}

allprojects {
    group = "com.lightningkite.lskiteuistarter"
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
}


// Whether modules declare iOS targets. Defaults to true only on macOS, overridable with `-PlsKiteui.ios=true|false`.
// KiteUI uses cinterop, so a KiteUI published from a non-Mac host has no iOS artifacts, and declaring iOS targets
// against it breaks even commonMain metadata compilation. Against a Mac-published KiteUI, iOS can be cross-compiled
// on any host, so forcing this on there is fine.
extra["iosEnabled"] = providers.gradleProperty("lsKiteui.ios").orNull?.toBoolean()
    ?: System.getProperty("os.name").contains("Mac", ignoreCase = true)