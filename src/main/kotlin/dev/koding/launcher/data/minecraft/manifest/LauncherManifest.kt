@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.minecraft.manifest

import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

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
        fun load(resourceManager: ResourceManager, name: String): LauncherManifest {
            val resource = resourceManager[name]?.file ?: error("Invalid resource")
            val manifest = resource.json<LauncherManifest>()

            if (manifest.inheritsFrom != null) {
                val sub = resourceManager.getManifest("profile:${manifest.inheritsFrom}")
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
        val natives: Map<String, String> = emptyMap()
    ) {
        @Serializable
        data class Downloads(
            val artifact: Asset? = null,
            val classifiers: Map<String, Asset> = emptyMap()
        )
    }

    @Serializable
    data class Logging(
        val client: Config
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
        )
    }

    @Serializable
    data class JavaVersion(
        val component: String,
        val majorVersion: Int
    )

    @Serializable
    data class Downloads(
        val client: Asset
    )
}

@Serializable
data class Asset(
    val url: String,
    val size: Long? = null,
    val id: String? = null,
    val sha1: String? = null,
    val totalSize: Long? = null,
    val path: String? = null
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