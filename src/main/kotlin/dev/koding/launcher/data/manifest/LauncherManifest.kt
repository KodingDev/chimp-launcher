@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.manifest

import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

@Serializable
data class LauncherManifest(
    val arguments: LaunchArguments,
    val id: String,
    val libraries: List<Library>,
    val mainClass: String,
    val type: String,
    val logging: LaunchLogging? = null,
    val javaVersion: LaunchJavaVersion? = null,
    val assets: String? = null,
    val assetIndex: Asset? = null,
    val downloads: LaunchDownloads? = null,
    val inheritsFrom: String? = null
) {
    companion object {
        fun load(resourceManager: ResourceManager, name: String): LauncherManifest {
            val resource = resourceManager[name]?.file?.inputStream() ?: error("Invalid resource")
            val manifest = json.decodeFromStream<LauncherManifest>(resource)

            if (manifest.inheritsFrom != null) {
                val sub = resourceManager.getManifest("profile:${manifest.inheritsFrom}")
                return manifest.copy(
                    arguments = manifest.arguments + sub.arguments,
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
}

@Serializable
data class LaunchLogging(
    val client: LoggingConfig
)

@Serializable
data class LoggingConfig(
    val argument: String,
    val file: Asset,
    val type: String
)

// TODO: Add minor version, etc
@Serializable
data class LaunchJavaVersion(
    val component: String,
    val majorVersion: Int
)

@Serializable
data class Library(
    val name: String,
    val url: String? = null,
    val downloads: LibraryDownloads? = null,
    val rules: List<Rule> = emptyList()
) {
    val asset: Asset?
        get() = downloads?.artifact ?: url?.let { url ->
            val path = name.split(":").let { "${it[0].replace(".", "/")}/${it[1]}/${it[2]}/${it[1]}-${it[2]}.jar" }
            Asset("$url$path", path = path)
        }

    val assets = listOfNotNull(
        asset,
        downloads?.classifiers?.macosNatives,
        downloads?.classifiers?.windowsNatives,
        downloads?.classifiers?.linuxNatives
    )
}

@Serializable
data class LibraryDownloads(
    val artifact: Asset,
    val classifiers: LibraryClassifiers? = null,
    val natives: LibraryNatives? = null
)

@Serializable
data class LibraryNatives(
    val windows: String? = null,
    val linux: String? = null,
    val macos: String? = null
)

@Serializable
data class LibraryClassifiers(
    @SerialName("natives-linux") val linuxNatives: Asset? = null,
    @SerialName("natives-macos") val macosNatives: Asset? = null,
    @SerialName("natives-windows") val windowsNatives: Asset? = null
)

// Server stuff isn't needed, nor mappings
@Serializable
data class LaunchDownloads(
    val client: Asset
)

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
data class LaunchArguments(
    @Serializable(with = LaunchArgumentListDeserializer::class)
    val game: List<LaunchArgument> = emptyList(),
    @Serializable(with = LaunchArgumentListDeserializer::class)
    val jvm: List<LaunchArgument> = emptyList()
)

@Serializable
data class LaunchArgument(
    val value: String,
    val rules: List<Rule> = emptyList()
)

@Serializable
enum class RuleAction {
    @SerialName("allow")
    ALLOW,

    @SerialName("disallow")
    DISALLOW
}

// TODO: Add more rules
@Serializable
data class Rule(
    val action: RuleAction,
    val os: OS? = null,
    val features: Features? = null
) {
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
    JsonTransformingSerializer<List<LaunchArgument>>(ListSerializer(LaunchArgument.serializer())) {
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