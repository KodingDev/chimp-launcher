package dev.koding.launcher.util

import kotlinx.serialization.json.Json


val json = Json { ignoreUnknownKeys = true }

fun String.replaceParams(vararg params: Pair<String, Any>) =
    params.fold(this) { acc, (key, value) -> acc.replace("\${${key}}", value.toString()) }