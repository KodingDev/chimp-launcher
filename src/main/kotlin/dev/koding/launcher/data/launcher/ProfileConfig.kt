@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.launcher

import dev.koding.launcher.loader.Resource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileConfig(
    val name: String,
    val launch: Launch,
    val resources: List<Resource> = emptyList(),
    val files: Map<String, File> = emptyMap()
) {
    @Serializable
    data class Launch(
        val profile: String,
        val arguments: List<String> = emptyList()
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