@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.minecraft.assets

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class AssetIndex(
    val objects: Map<String, Object>
) {
    @Serializable
    data class Object(
        val hash: String,
        val size: Long
    )
}