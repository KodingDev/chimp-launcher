// fuck it.
// crab in the code. 🦀

import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    java
}

group = "dev.koding"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")

    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")

    implementation("io.ktor:ktor-client-java:1.6.5")
    implementation("io.ktor:ktor-client-serialization:1.6.5")
    implementation("io.ktor:ktor-client-logging:1.6.5")
    implementation("io.ktor:ktor-server-netty:1.6.5")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + arrayOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<Jar> {
    manifest.attributes(
        "Main-Class" to "dev.koding.launcher.LauncherKt"
    )
}