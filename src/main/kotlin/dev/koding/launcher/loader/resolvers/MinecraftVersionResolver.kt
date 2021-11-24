package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.minecraft.version.VersionManifest
import dev.koding.launcher.data.minecraft.version.get
import dev.koding.launcher.loader.ResourceLocation
import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.loader.ResourceResolver
import dev.koding.launcher.loader.UrlResource

object MinecraftVersionResolver : ResourceResolver {
    private lateinit var versions: VersionManifest

    override suspend fun resolve(manager: ResourceManager, path: ResourceLocation): ResourceManager.LoadedResource? {
        if (!this::versions.isInitialized) versions = VersionManifest.fetch()
        if (!path.namespace.equals("profile", true)) return null
        return versions[path.path]?.let { manager.load(UrlResource(path.toString(), it.url)) }
    }
}