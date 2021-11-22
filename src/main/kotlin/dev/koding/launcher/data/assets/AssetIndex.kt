@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.assets

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject>
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
)