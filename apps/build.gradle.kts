import com.lightningkite.kiteui.KiteUiPluginExtension
import java.util.*
import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.comLightningKite.kiteuiPlugin)
    alias(libs.plugins.vite)
    id("com.google.gms.google-services")
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"


repositories {
    maven("https://jitpack.io")
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.comLightningKite.kiteui)
                api(libs.comLightningKite.csvDurable)
                api(libs.comLightningKite.lightningServer.core.shared)
                api(libs.comLightningKite.lightningServer.typed.shared)
                api(libs.comLightningKite.lightningServer.sessions.shared)
                api(libs.comLightningKite.lightningServerClient)
                api(project(":shared"))
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.firebase.messaging.ktx)
            }
        }
        val iosMain by getting {
            dependencies {
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("firebase", "10.7.1"))
            }
        }


        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    cocoapods {
        // Required properties
        // Specify the required Pod version here. Otherwise, the Gradle project version is used.
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "14.0"

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "apps"

        framework {
            baseName = "apps"
            export(project(":shared"))
            export(libs.comLightningKite.kiteui)
            export(libs.comLightningKite.lightningServerClient)
//            podfile = project.file("../example-app-ios/Podfile")
        }
    }
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

android {
    namespace = "com.lightningkite.lskiteuistarter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lightningkite.lskiteuistarter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.add("com/lightningkite/lightningserver/lightningdb.txt")
        resources.excludes.add("com/lightningkite/lightningserver/lightningdb-log.txt")
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
    coreLibraryDesugaring(libs.desugarJdkLibs)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING
    rootProject.the<YarnRootExtension>().reportNewYarnLock = true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

configure<KiteUiPluginExtension> {
    this.packageName = "com.lightningkite.lskiteuistarter"
    this.iosProjectRoot = project.file("./ios/app")
}

fun env(name: String, profile: String) {
    tasks.create("deployWeb${name}Init", Exec::class.java) {
        group = "deploy"
        this.dependsOn("viteBuild")
        this.environment("AWS_PROFILE", profile)
        val props = Properties()
        props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }
        this.executable = "terraform"
        this.args("init")
        this.workingDir = file("terraform/$name")
    }
    tasks.create("deployWeb${name}", Exec::class.java) {
        group = "deploy"
        this.dependsOn("deployWeb${name}Init")
        this.environment("AWS_PROFILE", profile)
        val props = Properties()
        props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }
        this.executable = "terraform"
        this.args("apply", "-auto-approve")
        this.workingDir = file("terraform/$name")
    }
}

env("default", "default")