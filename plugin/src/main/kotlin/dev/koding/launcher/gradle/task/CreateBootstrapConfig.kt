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

@file:OptIn(DelicateCoroutinesApi::class)

package dev.koding.launcher.gradle.task

import dev.koding.launcher.gradle.util.json
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.PreResolvedResolvableArtifact
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.net.URL

abstract class CreateBootstrapConfig : DefaultTask() {

    @OutputFile
    val outputFile =
        project.objects.fileProperty().convention(project.layout.buildDirectory.file("bootstrapConfig/config.json"))

    @TaskAction
    fun createBootstrapConfig() {
        val context = newFixedThreadPoolContext(10, "Download")
        val dependencies = runBlocking {
            project.configurations["runtimeClasspath"].resolvedConfiguration.resolvedArtifacts
                .filterIsInstance<PreResolvedResolvableArtifact>()
                .filter { it.moduleVersion.id.group != "dev.koding.launcher" }
                .map {
                    val ver = it.moduleVersion.id
                    CoroutineScope(context).async {
                        BootstrapManifest.Dependency(
                            "${ver.group}:${ver.name}:${ver.version}",
                            getRepository(ver.group, ver.name, ver.version)
                        )
                    }
                }
                .awaitAll()
        }

        val manifest = BootstrapManifest("dev.koding.launcher.LauncherKt", dependencies)
        outputFile.get().asFile.writeText(json.encodeToString(manifest))
    }

    private fun getRepository(group: String, name: String, version: String): String? {
        logger.info("Searching for repository for $group:$name:$version")
        return project.repositories.filterIsInstance<MavenArtifactRepository>()
            .find {
                val url = "${it.url}${group.replace('.', '/')}/$name/$version/$name-$version.pom"
                logger.info("[${it.url.host}] Requesting $url")
                runCatching { URL(url).openStream() != null }.isSuccess
            }?.let {
                logger.info("[${it.url.host}] Found repository for $group:$name:$version")
                it.url.toString()
            }
    }

}

@Serializable
data class BootstrapManifest(
    val main: String,
    val dependencies: List<Dependency>,
    val arguments: List<String> = emptyList(),
) {
    @Serializable
    data class Dependency(
        val artifact: String? = null,
        val repo: String? = null
    )
}