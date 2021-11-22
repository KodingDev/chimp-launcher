package dev.koding.launcher.data.launcher

import dev.koding.launcher.util.httpClient
import dev.koding.launcher.util.json
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class RemoteConfig(
    val profiles: List<RemoteProfile>
) {
    companion object {
        // Needs to be this way because of the Gist content type
        suspend fun fromUrl(url: String) = httpClient.get<String>(url)
            .let { json.decodeFromString<RemoteConfig>(it) }
    }
}

@Serializable
data class RemoteProfile(
    val name: String,
    val url: String,
    val description: String? = null
)