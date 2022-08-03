/*
 *    Copyright 2022 Koding Dev
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.launcher.Download
import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.launch.ResourcesDirectory
import dev.koding.launcher.loader.LoadedResource
import dev.koding.launcher.loader.ResourceDescriptor
import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.loader.ResourceResolver
import io.ktor.http.*
import java.io.File
import java.net.URI

object DefaultResourceResolver : ResourceResolver {
    override fun matches(uri: URI) = false

    override suspend fun resolve(manager: ResourceManager, resource: URI): LoadedResource {
        val target = manager.config[ResourcesDirectory] ?: error("No resources directory specified")
        val descriptor = ResourceDescriptor(resource)

        if (resource.scheme == "file") return LoadedResource(resource, File(resource))

        val uri = URLBuilder().apply {
            takeFrom(resource)
            parameters.names().filter { it.startsWith("chimp.") }
                .forEach { parameters.remove(it) }
        }.build().toURI()

        return LoadedResource(
            resource,
            Download(
                uri,
                path = descriptor.path,
                integrity = Download.Integrity(sha1 = descriptor.sha1, sha256 = descriptor.sha256),
                settings = Download.Settings(volatile = descriptor.volatile, log = descriptor.log)
            ).download(target)
        )
    }
}