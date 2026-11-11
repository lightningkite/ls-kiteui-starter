import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "com.lightningkite.lskiteuistarter.shared"
        compileSdk = 37
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
        val commonMain = getByName("commonMain") {
            dependencies {
                api(project.dependencies.platform(lk.stack.bom))
                api(lk.lightningserver.core.shared)
                api(lk.lightningserver.typed.shared)
                api(lk.lightningserver.sessions.shared)
                api(lk.lightningserver.files.shared)
                api(lk.lightningserver.media.shared)
            }
            kotlin {
                srcDir(file("build/generated/ksp/common/commonMain/kotlin"))
            }
        }
        val commonTest = getByName("commonTest") {
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
    coreLibraryDesugaring(libs.desugarJdkLibs)
    configurations.filter { it.name.startsWith("ksp") && it.name != "ksp" }.forEach {
        add(it.name, lk.services.database.processor)
    }
}

// commonMain compiles the database processor's shared output (written by kspCommonMainKotlinMetadata alone) as plain
// source, so every other KSP run and compile must wait for it.
tasks.matching { (it.name.startsWith("ksp") || it.name.startsWith("compile")) && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach { dependsOn("kspCommonMainKotlinMetadata") }
