
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
    implementation(libs.csvDurable)
    implementation(lk.lightningserver.core.asProvider())
    implementation(lk.lightningserver.typed.asProvider())
    implementation(lk.lightningserver.files.asProvider())
    implementation(lk.lightningserver.media.asProvider())
    implementation(lk.lightningserver.sessions.asProvider())
    implementation(lk.lightningserver.sessions.email)
    implementation(lk.lightningserver.sessions.sms)
    implementation(lk.lightningserver.sessions.totp)
    implementation(lk.lightningserver.sessions.webauthn)
    implementation(lk.lightningserver.fixtures)
    implementation(lk.lightningserver.engine.aws.serverless)
    implementation(lk.lightningserver.secret.source.aws)
    implementation(lk.lightningserver.engine.netty)
    implementation(lk.lightningserver.engine.ktor)
    implementation(lk.lightningserver.server.client.utils)
    implementation(lk.services.database.asProvider())
    implementation(lk.services.database.jsonfile)
    implementation(lk.services.database.mongodb)
    implementation(lk.services.notifications.fcm)
    implementation(lk.services.email.javasmtp)
    implementation(lk.services.files.s3)

    ksp(lk.services.database.processor)

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
tasks.register("dev", JavaExec::class.java) {
    group = "application"
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.lightningkite.lskiteuistarter.MainKt")
    args("dev")
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
