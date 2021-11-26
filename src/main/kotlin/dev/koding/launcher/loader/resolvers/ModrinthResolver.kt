package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.modrinth.ModrinthAPI
import dev.koding.launcher.loader.*

object ModrinthResolver : ResourceResolver {
    override suspend fun resolve(manager: ResourceManager, resource: ResourceLocation): LoadedResource<*>? {
        if (!resource.namespace.equals("modrinth", true)) return null

        val result = ModrinthAPI.search(resource.path[0])
        if (result.hits.isEmpty()) return null

        val hit = result.hits.first()
        val versions = ModrinthAPI.getVersions(hit.modId)
        if (versions.isEmpty()) return null

        val version = (if (resource.path.size == 1) versions.first()
        else versions.find { it.name == resource.path[1] }) ?: return null

        val file = version.files.firstOrNull() ?: return null
        return manager.load(
            UrlResource(
                resource.toString(),
                file.url,
                file.hashes.sha1,
                path = "modrinth/${hit.slug}/${version.name}/${file.filename}"
            )
        )
    }
}