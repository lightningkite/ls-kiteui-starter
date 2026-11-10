import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.ksp)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "com.lightningkite.lskiteuistarter.shared"
        compileSdk = 36
        minSdk = 26
        enableCoreLibraryDesugaring = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm()
    js {
        browser()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.lightningServer.core.shared)
                api(libs.lightningServer.typed.shared)
                api(libs.lightningServer.sessions.shared)
                api(libs.lightningServer.files.shared)
                api(libs.lightningServer.media.shared)
            }
            kotlin {
                srcDir(file("build/generated/ksp/common/commonMain/kotlin"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    configurations.filter { it.name.startsWith("ksp") && it.name != "ksp" }.forEach {
        add(it.name, libs.services.database.processor)
    }
    coreLibraryDesugaring(libs.desugarJdkLibs)
}

// commonMain compiles the database processor's shared output (written by kspCommonMainKotlinMetadata alone) as plain
// source, so every other KSP run and compile must wait for it.
tasks.matching { (it.name.startsWith("ksp") || it.name.startsWith("compile")) && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach { dependsOn("kspCommonMainKotlinMetadata") }
