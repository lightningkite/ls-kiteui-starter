import java.util.*

// The Android app shell for :apps. AGP 9 doesn't allow com.android.application in a KMP module, so the
// code (including MainActivity) and resources stay in :apps and this module only packages it.
plugins {
    alias(libs.plugins.androidApp)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://jitpack.io")
}

android {
    // Must differ from :apps's namespace; applicationId is what identifies the installed app.
    namespace = "com.lightningkite.lskiteuistarter.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lightningkite.lskiteuistarter"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "0.0.1"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.add("com/lightningkite/lightningserver/lightningdb.txt")
        resources.excludes.add("com/lightningkite/lightningserver/lightningdb-log.txt")
        resources.excludes.add("com/lightningkite/lightningserver/*-dependencies.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    val props = project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
        Properties().apply { load(stream) }
    }
    if (props != null && props.getProperty("signingKeystore") != null) {
        signingConfigs {
            this.create("release") {
                storeFile = project.rootProject.file(props.getProperty("signingKeystore"))
                storePassword = props.getProperty("signingPassword")
                keyAlias = props.getProperty("signingAlias")
                keyPassword = props.getProperty("signingAliasPassword")
            }
        }
        buildTypes {
            this.getByName("release") {
                this.isMinifyEnabled = false
                this.proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
                this.signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":apps"))
    coreLibraryDesugaring(libs.desugarJdkLibs)
}


val localProperties = project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
    Properties().apply { load(stream) }
}


tasks.register<Exec>("publishAndroid") {
    group = "deploy"
    description = "Build signed AAB then upload to Play Store internal track via Fastlane"
    dependsOn("bundleRelease")  // Gradle builds + signs; Fastlane only uploads
    workingDir = rootProject.projectDir
    environment("PLAY_STORE_JSON_KEY_PATH", localProperties?.getProperty("playStoreJsonKeyPath") ?: "")
    commandLine("bundle", "exec", "fastlane", "android", "internal")
}

tasks.register<Exec>("promoteAndroid") {
    group = "deploy"
    description = "Promote the current Play Store internal track build to production via Fastlane"
    workingDir = rootProject.projectDir
    environment("PLAY_STORE_JSON_KEY_PATH", localProperties?.getProperty("playStoreJsonKeyPath") ?: "")
    commandLine("bundle", "exec", "fastlane", "android", "promote_to_production")
}
