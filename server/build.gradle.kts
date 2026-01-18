
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    application
}


group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"


application {
    mainClass.set("com.lightningkite.lskiteuistarter.MainKt")
}

dependencies {

    implementation(project(":shared"))
    implementation(libs.kotliner.cli)
    implementation(libs.csvDurable)
    implementation(libs.lightningServer.core)
    implementation(libs.lightningServer.typed)
    implementation(libs.lightningServer.files)
    implementation(libs.lightningServer.media)
    implementation(libs.lightningServer.sessions)
    implementation(libs.lightningServer.sessions.email)
    implementation(libs.lightningServer.sessions.sms)
    implementation(libs.lightningServer.engine.aws)
    implementation(libs.lightningServer.secretSource.aws)
    implementation(libs.lightningServer.engine.netty)
    implementation(libs.lightningServer.engine.ktor)
    implementation(libs.lightningServer.client.serverUtils)
    implementation(libs.services.database)
    implementation(libs.services.database.jsonfile)
    implementation(libs.services.database.mongodb)
    implementation(libs.services.notifications.firebase)
    implementation(libs.services.email.javasmtp)
    implementation(libs.services.files.s3)
    implementation(libs.kotliner.cli)

    ksp(libs.services.database.processor)

    api(libs.kotliner.cli)
    testImplementation(kotlin("test"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.getByName<Zip>("distZip"){
    archiveFileName.set("server.zip")
}
tasks.create("generateSdk", JavaExec::class.java) {
    group = "deploy"
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.lightningkite.lskiteuistarter.MainKt")
    args("sdk")
    workingDir(project.rootDir)
}
tasks.create("serve", JavaExec::class.java) {
    group = "application"
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.lightningkite.lskiteuistarter.MainKt")
    args("serve")
    workingDir(project.rootDir)
}
tasks.create("lambda", Copy::class.java) {
    group = "deploy"
    this.destinationDir = project.buildDir.resolve("dist/lambda")
    val jarTask = tasks.getByName("jar")
    dependsOn(jarTask)
    val output = jarTask.outputs.files.find { it.extension == "jar" }!!
    from(zipTree(output))
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}
