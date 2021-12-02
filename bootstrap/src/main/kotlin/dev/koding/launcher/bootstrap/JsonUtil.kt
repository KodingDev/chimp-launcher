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
inline fun <reified T> String.fromUrl(): T = json.decodeFromStream(URL(this@fromUrl).openStream())