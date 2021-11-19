@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.assets

import dev.koding.launcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import java.io.File

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject>
) {
    companion object {
        fun load(file: File) = json.decodeFromStream<AssetIndex>(file.inputStream())
    }
}

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
)