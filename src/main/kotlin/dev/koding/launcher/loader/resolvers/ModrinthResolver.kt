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
                path = target
            )
        )
    }
}