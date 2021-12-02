@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.launcher

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class LocalConfig(
    val config: String,
    val profile: String? = null
)