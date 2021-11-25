package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.minecraft.version.VersionManifest
import dev.koding.launcher.data.minecraft.version.get
import dev.koding.launcher.loader.*

object MinecraftVersionResolver : ResourceResolver {
    private lateinit var versions: VersionManifest

    override suspend fun resolve(manager: ResourceManager, path: ResourceLocation): LoadedResource<*>? {
        if (!this::versions.isInitialized) versions = VersionManifest.fetch()
        if (!path.namespace.equals("profile", true)) return null
        return versions[path.path]?.let { manager.load(UrlResource(path.toString(), it.url)) }
    }
}