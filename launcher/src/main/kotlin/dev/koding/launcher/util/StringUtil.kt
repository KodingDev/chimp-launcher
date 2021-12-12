@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.util

import dev.koding.launcher.loader.ResourceManager
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.net.URI
import java.security.MessageDigest
import java.util.*

inline fun <reified T> readResource(name: String): T? =
    ResourceManager::class.java.getResourceAsStream(name)?.let { json.decodeFromStream(it) }

fun String.replaceParams(vararg params: Pair<String, Any>) =
    params.fold(this) { acc, (key, value) -> acc.replace("\${${key}}", value.toString()) }

fun ByteArray.toUrlBase64(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

val String.sha256: ByteArray? get() = MessageDigest.getInstance("SHA-256").digest(toByteArray())

val URI.ktor get() = Url(this)

val URI.pathComponents get() = path.removePrefix("/").split("/")