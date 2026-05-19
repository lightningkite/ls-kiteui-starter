// by Claude — JVM-only module for load testing against a live server.
// Run: ./gradlew :load-tests:test -Dloadtest.url=http://localhost:8080
// Skip if loadtest.url is not set (safe to include in CI without a live server).
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":server"))
    implementation(project(":shared"))
    implementation(libs.lightningServer.core)
    implementation(libs.lightningServer.typed)
    implementation(libs.lightningServer.sessions)
    implementation(libs.lightningServer.load.test)
    implementation(libs.services.database)

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
