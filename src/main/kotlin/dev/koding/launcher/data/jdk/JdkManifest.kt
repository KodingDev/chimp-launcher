@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.jdk

import dev.koding.launcher.data.manifest.Asset
import dev.koding.launcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import java.io.File

@Serializable
data class JdkManifest(
    val files: Map<String, JdkFile>
) {
    companion object {
        fun load(file: File) = json.decodeFromStream<JdkManifest>(file.inputStream())
    }
}

@Serializable
data class JdkFile(
    val type: Type,
    val executable: Boolean? = null,
    val downloads: Downloads? = null,
    val target: String? = null
) {
    @Serializable
    enum class Type {
        @SerialName("directory")
        DIRECTORY,

        @SerialName("file")
        FILE,

        @SerialName("link")
        LINK
    }

    @Serializable
    data class Downloads(
        val raw: Asset,
        val lzma: Asset? = null
    )
}