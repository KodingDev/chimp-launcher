@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.launcher

import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.util.URISerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import java.net.URI

@Serializable
data class ProfileConfig(
    val name: String,
    val launch: Launch,
    val resources: List<@Serializable(URISerializer::class) URI> = emptyList(),
    @Serializable(FileMapDeserializer::class)
    val files: Map<String, File> = emptyMap()
) {
    @Serializable
    data class Launch(
        @Serializable(URISerializer::class)
        val profile: URI,
        val arguments: LauncherManifest.Arguments = LauncherManifest.Arguments(),
        val debug: Boolean = false,
        val macOSWorkaround: Boolean = false
    )

    @Serializable
    data class File(
        val resource: String? = null,
        val action: Action = Action.COPY
    ) {
        @Serializable
        enum class Action {
            @SerialName("copy")
            COPY,

            @SerialName("delete")
            DELETE
        }
    }
}

object FileMapDeserializer : JsonTransformingSerializer<Map<String, ProfileConfig.File>>(
    MapSerializer(
        String.serializer(),
        ProfileConfig.File.serializer()
    )
) {
    override fun transformDeserialize(element: JsonElement) =
        buildJsonObject {
            for ((key, json) in element.jsonObject) {
                if (json is JsonPrimitive) {
                    putJsonObject(key) { put("resource", json.content) }
                } else {
                    put(key, json)
                }
            }
        }
}