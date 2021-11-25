@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.util

import dev.koding.launcher.Launcher
import dev.koding.launcher.loader.FileResource
import dev.koding.launcher.loader.NamedResource
import dev.koding.launcher.loader.Resource
import dev.koding.launcher.loader.UrlResource
import io.ktor.client.request.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.File
import java.security.MessageDigest
import java.util.*


val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true

    serializersModule = SerializersModule {
        polymorphic(Resource::class) {
            subclass(NamedResource::class)
            subclass(UrlResource::class)
            subclass(FileResource::class)
        }
    }
}

inline fun <reified T> T.toJson(): String = json.encodeToString(this)
inline fun <reified T> String.json(): T = json.decodeFromString(this)
inline fun <reified T> File.json(): T = readText().json()

suspend inline fun <reified T> String.fromUrl(): T = httpClient.get<String>(this).let { json.decodeFromString(it) }
inline fun <reified T> readResource(name: String): T? =
    Launcher::class.java.getResourceAsStream(name)?.let { json.decodeFromStream(it) }

fun String.replaceParams(vararg params: Pair<String, Any>) =
    params.fold(this) { acc, (key, value) -> acc.replace("\${${key}}", value.toString()) }

fun ByteArray.toUrlBase64(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

val String.sha256: ByteArray? get() = MessageDigest.getInstance("SHA-256").digest(toByteArray())