package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.minecraft.version.VersionManifest
import dev.koding.launcher.data.minecraft.version.get
import dev.koding.launcher.loader.*

object MinecraftVersionResolver : ResourceResolver {
    private lateinit var versions: VersionManifest

    override suspend fun resolve(manager: ResourceManager, resource: ResourceLocation): LoadedResource<*>? {
        if (!this::versions.isInitialized) versions = VersionManifest.fetch()
        if (!resource.namespace.equals("profile", true)) return null
        val version = resource.path.first()
        return versions[version]?.let {
            manager.load(
                UrlResource(
                    resource.toString(),
                    it.url,
                    path = "minecraft/versions/$version.json"
                )
            )
        }
    }
}