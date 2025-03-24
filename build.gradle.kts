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
        classpath("com.android.tools.build:gradle:8.6.1")
        classpath("com.lightningkite:lk-gradle-helpers:1.2.1")
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
