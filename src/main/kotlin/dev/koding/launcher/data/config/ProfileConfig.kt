@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.config

import dev.koding.launcher.Launcher
import dev.koding.launcher.util.httpClient
import dev.koding.launcher.util.json
import io.ktor.client.request.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class ProfileConfig(
    val name: String,
    val launch: LaunchConfig,
    val resources: List<ProfileResource> = emptyList(),
    val files: Map<String, ProfileFile> = emptyMap()
) {
    companion object {
        // Needs to be this way because of the Gist content type
        suspend fun fromUrl(url: String) = httpClient.get<String>(url)
            .let { json.decodeFromString<ProfileConfig>(it) }

        @Suppress("unused")
        fun fromResources(path: String) = json.decodeFromStream<ProfileConfig>(
            Launcher::class.java.getResourceAsStream(path) ?: error("File not found")
        )
    }
}

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