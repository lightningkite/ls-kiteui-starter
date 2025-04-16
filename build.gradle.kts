plugins {
    kotlin("jvm") version "2.0.21"
}

buildscript {
    val kotlinVersion:String by extra
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath(libs.androidBuildTools)
        classpath(libs.lkGradleHelpers)
    }
}

allprojects {
    repositories {
        group = "com.lightningkite.template"
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
    }
}
