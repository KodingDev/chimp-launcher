package dev.koding.launcher.data.launcher

import dev.koding.launcher.loader.Resource
import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfig(
    val profiles: List<RemoteProfile>
)

@Serializable
data class RemoteProfile(
    val name: String,
    val resource: Resource,
    val description: String? = null
)