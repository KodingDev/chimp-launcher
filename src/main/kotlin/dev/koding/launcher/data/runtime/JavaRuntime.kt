package dev.koding.launcher.data.runtime

import dev.koding.launcher.data.manifest.Asset
import dev.koding.launcher.util.httpClient
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias JavaConfigs = Map<String, List<JavaRuntimeData>>

@Serializable
data class JavaRuntime(
    val linux: JavaConfigs,
    @SerialName("mac-os") val macOs: JavaConfigs,
    @SerialName("windows-x64") val windows64: JavaConfigs,
    @SerialName("windows-x86") val windows32: JavaConfigs
) {
    companion object {
        suspend fun load() =
            httpClient.get<JavaRuntime>("https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json")
    }
}

@Serializable
data class JavaRuntimeData(
    val manifest: Asset,
    val version: JavaVersionData
)

@Serializable
data class JavaVersionData(
    val name: String
)