package dev.koding.launcher.data.launcher

import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfig(
    val profiles: List<RemoteProfile>
)

@Serializable
data class RemoteProfile(
    val name: String,
    val url: String,
    val description: String? = null
)