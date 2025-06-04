import com.lightningkite.deployhelpers.publishing
import com.lightningkite.deployhelpers.useGitBasedVersion
import com.lightningkite.deployhelpers.useLocalDependencies

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.versionCatalogUpdate)
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

//    useGitBasedVersion()
    useLocalDependencies()
//    publishing()

    repositories {
        group = "com.lightningkite.template"
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
}
