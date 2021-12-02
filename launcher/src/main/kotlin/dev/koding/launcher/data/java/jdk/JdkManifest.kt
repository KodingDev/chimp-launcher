@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.java.jdk

import dev.koding.launcher.data.minecraft.manifest.Asset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JdkManifest(
    val files: Map<String, File>
) {
    @Serializable
    data class File(
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
}