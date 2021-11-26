package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.modrinth.ModrinthAPI
import dev.koding.launcher.loader.*

object ModrinthResolver : ResourceResolver {
    override suspend fun resolve(manager: ResourceManager, path: ResourceLocation): LoadedResource<*>? {
        if (!path.namespace.equals("modrinth", true)) return null

        val result = ModrinthAPI.search(path.path[0])
        if (result.hits.isEmpty()) return null

        val versions = ModrinthAPI.getVersions(result.hits.first().modId)
        if (versions.isEmpty()) return null

        val version = (if (path.path.size == 1) versions.first()
        else versions.find { it.name == path.path[1] }) ?: return null

        val file = version.files.firstOrNull() ?: return null
        return manager.load(UrlResource(path.path[0], file.url, file.hashes.sha1))
    }
}