package dev.koding.launcher.data.minecraft.version

import dev.koding.launcher.util.fromUrl
import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
    val versions: List<Version>
) {
    companion object {
        suspend fun fetch() = "https://launchermeta.mojang.com/mc/game/version_manifest.json".fromUrl<VersionManifest>()
    }

    @Serializable
    data class Version(
        val id: String,
        val url: String
    )
}