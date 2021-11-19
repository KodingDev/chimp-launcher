@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.manifest

import dev.koding.launcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

@Serializable
data class LauncherManifest(
    val arguments: LaunchArguments,
    val assetIndex: Asset,
    val assets: String,
    val downloads: LaunchDownloads,
    val id: String,
    val javaVersion: LaunchJavaVersion,
    val libraries: List<Library>,
    val logging: LaunchLogging,
    val mainClass: String,
    val type: String
) {
    companion object {
        fun loadResource(resourceName: String): LauncherManifest {
            val resource = LauncherManifest::class.java.getResourceAsStream(resourceName)
            return json.decodeFromStream(resource ?: error("Resource not found"))
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
    val downloads: LibraryDownloads,
    val name: String,
    val rules: List<Rule> = emptyList()
)

@Serializable
data class LibraryDownloads(
    val artifact: Asset,
    val classifiers: LibraryClassifiers? = null,
    val natives: LibraryNatives? = null
) {
    // TODO: This could be optimized but later
    val assets = listOfNotNull(artifact, classifiers?.macosNatives, classifiers?.windowsNatives, classifiers?.linuxNatives)
}

@Serializable
data class LibraryNatives(
    val windows: String? = null,
    val linux: String? = null,
    val macos: String? = null
)

// TODO: Make this more generic to just download
// all assets
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
    val size: Long,
    val id: String? = null,
    val sha1: String? = null,
    val totalSize: Long? = null,
    val path: String? = null
)

@Serializable
data class LaunchArguments(
    @Serializable(with = LaunchArgumentListDeserializer::class)
    val game: List<LaunchArgument>,
    @Serializable(with = LaunchArgumentListDeserializer::class)
    val jvm: List<LaunchArgument>
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