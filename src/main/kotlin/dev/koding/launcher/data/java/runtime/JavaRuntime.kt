package dev.koding.launcher.data.java.runtime

import dev.koding.launcher.data.minecraft.manifest.Asset
import dev.koding.launcher.util.fromUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias JavaConfigs = Map<String, List<JavaRuntime.Data>>

@Serializable
data class JavaRuntime(
    val linux: JavaConfigs,
    @SerialName("mac-os") val macOs: JavaConfigs,
    @SerialName("windows-x64") val windows64: JavaConfigs,
    @SerialName("windows-x86") val windows32: JavaConfigs
) {
    companion object {
        suspend fun fetch() =
            "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json".fromUrl<JavaRuntime>()
    }

    @Serializable
    data class Data(
        val manifest: Asset,
        val version: Version
    ) {
        @Serializable
        data class Version(
            val name: String
        )
    }
}