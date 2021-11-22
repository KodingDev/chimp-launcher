@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileConfig(
    val name: String,
    val launch: LaunchConfig,
    val resources: List<ProfileResource> = emptyList(),
    val files: Map<String, ProfileFile> = emptyMap()
)

@Serializable
data class LaunchConfig(
    val profile: String,
    val arguments: List<String> = emptyList()
)

@Serializable
sealed class ProfileResource {
    abstract val name: String
}

@Serializable
data class UrlResource(
    override val name: String,
    val url: String
) : ProfileResource()

@Serializable
data class ProfileFile(
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