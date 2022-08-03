/*
 *    Copyright 2022 Koding Dev
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
