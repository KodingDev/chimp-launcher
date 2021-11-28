package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.modrinth.ModrinthAPI
import dev.koding.launcher.launch.ResourcesDirectory
import dev.koding.launcher.loader.*
import mu.KotlinLogging

object ModrinthResolver : ResourceResolver {
    private val logger = KotlinLogging.logger {}

    override suspend fun resolve(manager: ResourceManager, resource: ResourceLocation): LoadedResource<*>? {
        if (!resource.namespace.equals("modrinth", true)) return null

        val mod = resource.path[0]
        val versionName = resource.path.getOrNull(1) ?: "latest"

        // For speed's sake we will assume that the file is the correct mod
        // if it already exists
        val target = "modrinth/${mod}/${versionName}/${mod}-${versionName}.jar"
        val targetFile = manager.config[ResourcesDirectory]?.resolve(target)
            ?: error("No resources directory configured")
        if (targetFile.exists()) return manager.load(FileResource(resource.toString(), targetFile.absolutePath))

        logger.debug { "Searching for modrinth resource $resource" }
        val result = ModrinthAPI.search(mod)
        if (result.hits.isEmpty()) return null

        logger.debug { "Fetching versions for modrinth resource $resource" }
        val hit = result.hits.sortedBy {
            if (it.slug.equals(mod, true)) return@sortedBy 0
            if (it.slug.replace("-", "").equals(mod, true)) return@sortedBy 5
            if (it.modId.equals(mod, true)) return@sortedBy 5
            10
        }.firstOrNull() ?: return null

        val versions = ModrinthAPI.getVersions(hit.modId)
        if (versions.isEmpty()) return null

        val version = (if (resource.path.size == 1) versions.first()
        else versions.find { it.versionNumber == resource.path[1] }) ?: return null

        val file = version.files.firstOrNull() ?: return run {
            logger.debug { "No version found for modrinth resource $resource" }
            null
        }

        return manager.load(
            UrlResource(
                resource.toString(),
                file.url,
                integrity = UrlResource.Integrity(file.hashes.sha1, file.hashes.sha256),
                path = target
            )
        )
    }
}