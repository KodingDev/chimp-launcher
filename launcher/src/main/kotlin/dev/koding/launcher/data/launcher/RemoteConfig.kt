package dev.koding.launcher.data.launcher

import dev.koding.launcher.util.URISerializer
import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class RemoteConfig(
    val profiles: List<RemoteProfile>
)

@Serializable
data class RemoteProfile(
    val name: String,
    @Serializable(URISerializer::class)
    val resource: URI,
    val description: String? = null
)