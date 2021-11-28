@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.launcher

import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.loader.Resource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

@Serializable
data class ProfileConfig(
    val name: String,
    val launch: Launch,
    val resources: List<Resource> = emptyList(),
    @Serializable(FileMapDeserializer::class)
    val files: Map<String, File> = emptyMap()
) {
    @Serializable
    data class Launch(
        val profile: String,
        val arguments: LauncherManifest.Arguments = LauncherManifest.Arguments()
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