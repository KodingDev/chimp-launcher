package dev.koding.launcher.gradle.util

import kotlinx.serialization.json.Json

val json = Json {
    prettyPrint = true
    encodeDefaults = false
}