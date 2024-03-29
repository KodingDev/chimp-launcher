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

package dev.koding.launcher.data.minecraft.manifest

import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.util.URISerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import java.net.URI

@Serializable
data class LauncherManifest(
    val id: String,
    val libraries: List<Library>,
    val mainClass: String,
    val type: String,
    val minecraftArguments: String? = null,
    val arguments: Arguments? = null,
    val logging: Logging? = null,
    val javaVersion: JavaVersion? = null,
    val assets: String? = null,
    val assetIndex: Asset? = null,
    val downloads: Downloads? = null,
    val inheritsFrom: String? = null
) {
    companion object {
        suspend fun load(resourceManager: ResourceManager, resource: URI): LauncherManifest {
            val manifest = resourceManager.load(resource)?.json<LauncherManifest>()
                ?: error("Invalid resource")
            if (manifest.inheritsFrom != null) {
                val sub = resourceManager.load(URI("content://net.minecraft/${manifest.inheritsFrom}"))
                    ?.json<LauncherManifest>() ?: error("Invalid launcher manifest")
                return manifest.copy(
                    arguments = (manifest.arguments ?: Arguments()) + (sub.arguments ?: Arguments()),
                    downloads = sub.downloads ?: manifest.downloads,
                    libraries = manifest.libraries + sub.libraries,
                    assetIndex = sub.assetIndex ?: manifest.assetIndex,
                    assets = sub.assets ?: manifest.assets,
                    javaVersion = sub.javaVersion ?: manifest.javaVersion,
                    logging = sub.logging ?: manifest.logging
                )
            }

            return manifest
        }
    }

    @Serializable
    data class Library(
        val name: String,
        val url: String? = null,
        val downloads: Downloads? = null,
        val rules: List<Rule> = emptyList(),
        val natives: Map<String, String> = emptyMap(),
        val native: Boolean = false,
    ) {
        @Serializable
        data class Downloads(
            val artifact: Asset? = null,
            val classifiers: Map<String, Asset> = emptyMap()
        )
    }

    @Serializable
    data class Logging(
        val client: Config? = null,
    ) {
        @Serializable
        data class Config(
            val argument: String,
            val file: Asset,
            val type: String
        )
    }

    @Serializable
    data class Arguments(
        @Serializable(with = LaunchArgumentListDeserializer::class)
        val game: List<Argument> = emptyList(),
        @Serializable(with = LaunchArgumentListDeserializer::class)
        val jvm: List<Argument> = emptyList()
    ) {
        @Serializable
        data class Argument(
            val value: String,
            val rules: List<Rule> = emptyList()
        ) {
            override fun toString() = value
        }
    }

    @Serializable
    data class JavaVersion(
        val component: String,
        val majorVersion: Int,
        val asset: Asset? = null
    )

    @Serializable
    data class Downloads(
        val client: Asset? = null
    )
}

@Serializable
data class Asset(
    @Serializable(URISerializer::class)
    val url: URI? = null,
    val size: Long? = null,
    val sha1: String? = null,
    val path: String? = null,
)

@Serializable
data class Rule(
    val action: Action,
    val os: OS? = null,
    val features: Features? = null
) {
    @Serializable
    enum class Action {
        @SerialName("allow")
        ALLOW,

        @SerialName("disallow")
        DISALLOW
    }

    @Serializable
    data class OS(
        val name: String? = null,
        val version: String? = null,
        val arch: String? = null
    )

    @Serializable
    data class Features(
        @SerialName("is_demo_user") val isDemoUser: Boolean? = null,
        @SerialName("has_custom_resolution") val hasCustomResolution: Boolean? = null
    )
}

object LaunchArgumentListDeserializer :
    JsonTransformingSerializer<List<LauncherManifest.Arguments.Argument>>(ListSerializer(LauncherManifest.Arguments.Argument.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) return element
        return JsonArray(element.flatMap {
            when (it) {
                is JsonPrimitive -> listOf(buildJsonObject { put("value", it.content) })
                else -> {
                    val value = it.jsonObject.getValue("value")
                    if (value !is JsonArray) return@flatMap listOf(it)

                    return@flatMap value.map { argument ->
                        buildJsonObject {
                            put("value", argument)
                            it.jsonObject["rules"]?.let { rules -> put("rules", rules) }
                        }
                    }
                }
            }
        })
    }
}