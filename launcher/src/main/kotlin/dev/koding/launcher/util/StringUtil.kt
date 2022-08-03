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