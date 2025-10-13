import com.lightningkite.deployhelpers.*
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    application
}


group = "com.lightningkite.template"
version = "1.0-SNAPSHOT"


application {
    mainClass.set("com.lightningite.template.MainKt")
}

dependencies {

    implementation(project(":shared"))
    api(libs.kotliner.cli)
    api(libs.comLightningKite.csvDurable)
    api(libs.comLightningKite.services.database)
    api(libs.comLightningKite.services.database.jsonfile)
    api(libs.comLightningKite.services.database.mongodb)
    api(libs.comLightningKite.services.notifications.firebase)
    api(libs.comLightningKite.lightningServer.core)
    api(libs.comLightningKite.lightningServer.typed)
    api(libs.comLightningKite.lightningServer.files)
    api(libs.comLightningKite.lightningServer.media)
    api(libs.comLightningKite.lightningServer.sessions)
    api(libs.comLightningKite.lightningServer.sessions.email)
    api(libs.comLightningKite.lightningServer.sessions.sms)
    implementation(libs.comLightningKite.lightningServer.engine.aws)
    implementation(libs.comLightningKite.services.files.s3)
    implementation(libs.comLightningKite.lightningServer.engine.netty)
    implementation(libs.comLightningKite.lightningServer.engine.ktor)
    implementation(libs.comLightningKite.lightningServerClient.serverUtils)
    implementation(libs.kotliner.cli)

    ksp(libs.comLightningKite.services.database.processor)

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
    mainClass.set("com.lightningkite.template.MainKt")
    args("sdk")
    workingDir(project.rootDir)
}
tasks.create("serve", JavaExec::class.java) {
    group = "application"
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.lightningkite.template.MainKt")
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
tasks.create("rebuildTerraform", JavaExec::class.java) {
    group = "deploy"
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.lightningkite.template.MainKt")
    args("terraform")
    workingDir(project.rootDir)
}

fun env(name: String, profile: String) {
    val mongoProfile = file("${System.getProperty("user.home")}/.mongo/profiles/$profile.env")

    if(mongoProfile.exists()) {
        tasks.create("deployServer${name}Init", Exec::class.java) {
            group = "deploy"
            this.dependsOn("lambda", "rebuildTerraform")
            this.environment("AWS_PROFILE", "$profile")
            val props = Properties()
            mongoProfile.reader().use { props.load(it) }
            props.entries.forEach {
                environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' '))
            }
            this.executable = "terraform"
            this.args("init")
            this.workingDir = file("terraform/$name")
        }
        tasks.create("deployServer${name}", Exec::class.java) {
            group = "deploy"
            this.dependsOn("deployServer${name}Init")
            this.environment("AWS_PROFILE", "$profile")
            val props = Properties()
            mongoProfile.reader().use { props.load(it) }
            props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }

            this.executable = "terraform"
            this.args("apply", "-auto-approve")
            this.workingDir = file("terraform/$name")
        }
    }
}
env("default", "default")