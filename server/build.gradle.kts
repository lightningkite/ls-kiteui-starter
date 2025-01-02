import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    application
}


group = "com.lightningkite.template"
version = "1.0-SNAPSHOT"


application {
    mainClass.set("com.lightningite.template.MainKt")
}

val lightningServerVersion: String by extra
val coroutines: String by extra
dependencies {

    implementation(project(":shared"))
    api("com.lightningkite.lightningserver:server-core:$lightningServerVersion")
    api("com.lightningkite.lightningserver:server-aws:$lightningServerVersion")
    api("com.lightningkite.lightningserver:server-mongo:$lightningServerVersion")
    api("com.lightningkite.lightningserver:server-ktor:$lightningServerVersion")
    api("com.lightningkite.lightningserver:server-sentry:$lightningServerVersion")
    ksp("com.lightningkite.lightningserver:processor:$lightningServerVersion")

    api("com.lightningkite:kotliner-cli:1.0.5")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.23.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.getByName<Zip>("distZip"){
    archiveFileName.set("server.zip")
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