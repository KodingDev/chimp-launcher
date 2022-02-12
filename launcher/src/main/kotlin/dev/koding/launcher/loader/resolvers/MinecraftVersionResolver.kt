package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.minecraft.version.VersionManifest
import dev.koding.launcher.data.minecraft.version.get
import dev.koding.launcher.loader.LoadedResource
import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.loader.ResourceResolver
import dev.koding.launcher.loader.describe
import dev.koding.launcher.util.pathComponents
import java.net.URI

object MinecraftVersionResolver : ResourceResolver {
    private lateinit var versions: VersionManifest

    override fun matches(uri: URI) = uri.scheme.equals("content", true) &&
            uri.host.equals("net.minecraft", true)

    override suspend fun resolve(manager: ResourceManager, resource: URI): LoadedResource? {
        if (!this::versions.isInitialized) versions = VersionManifest.fetch()
        val version = resource.pathComponents.first()
        return versions[version]?.let {
            manager.load(
                URI(it.url).describe {
                    path = "minecraft/versions/$version.json"
                }
            )
        }
    }
}