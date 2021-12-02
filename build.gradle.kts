// fuck it.
// crab in the code. 🦀

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    java
}

group = "dev.koding"
version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.github.johnrengelman.shadow")

    version = rootProject.version

    repositories {
        mavenCentral()
    }

    dependencies {
        api(kotlin("stdlib"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + arrayOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    tasks["build"].dependsOn("shadowJar")
}