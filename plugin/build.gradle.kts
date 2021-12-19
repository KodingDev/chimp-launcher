import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("plugin.serialization") version "1.5.31"
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
}

gradlePlugin {
    plugins {
        create("chimp-launcher") {
            id = "dev.koding.chimp-launcher.gradle"
            implementationClass = "dev.koding.launcher.gradle.ChimpLauncherPlugin"
        }
    }
}

tasks.compileJava {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + arrayOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}
