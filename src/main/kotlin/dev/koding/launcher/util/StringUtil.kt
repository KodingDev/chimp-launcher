package dev.koding.launcher.util

import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.*


val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

fun String.replaceParams(vararg params: Pair<String, Any>) =
    params.fold(this) { acc, (key, value) -> acc.replace("\${${key}}", value.toString()) }

fun ByteArray.toUrlBase64(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

val String.sha256: ByteArray? get() = MessageDigest.getInstance("SHA-256").digest(toByteArray())