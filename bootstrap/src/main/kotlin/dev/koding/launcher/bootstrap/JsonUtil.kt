@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.bootstrap

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.net.URL

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

@Suppress("unused")
inline fun <reified T> T.toJson(): String = json.encodeToString(this)
inline fun <reified T> String.json(): T = json.decodeFromString(this)
inline fun <reified T> File.json(): T = readText().json()

inline fun <reified T> String.fromUrl(): T = json.decodeFromStream(URL(this@fromUrl).openConnection().apply {
    setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
    )
}.getInputStream())