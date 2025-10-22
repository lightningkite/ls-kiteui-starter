import com.lightningkite.deployhelpers.useGitBasedVersion
import com.lightningkite.deployhelpers.useLocalDependencies

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.graalVmNative) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.vite) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.lkGradleHelpers)
    }
}

allprojects {
    useLocalDependencies()

    repositories {
        group = "com.lightningkite.lskiteuistarter"
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
}
