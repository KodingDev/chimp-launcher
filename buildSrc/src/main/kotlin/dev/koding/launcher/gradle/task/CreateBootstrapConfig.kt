package dev.koding.launcher.gradle.task

import dev.koding.launcher.bootstrap.Manifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.PreResolvedResolvableArtifact
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.net.URL

abstract class CreateBootstrapConfig : DefaultTask() {
    @OutputFile
    val outputFile = project.objects.fileProperty().convention(project.layout.buildDirectory.file("config.json"))

    @TaskAction
    fun createBootstrapConfig() {
        val dependencies = project.configurations["runtimeClasspath"].resolvedConfiguration.resolvedArtifacts
            .filterIsInstance<PreResolvedResolvableArtifact>()
            .filter { it.moduleVersion.id.group != "dev.koding.launcher" }
            .map {
                val ver = it.moduleVersion.id
                Manifest.Dependency(
                    "${ver.group}:${ver.name}:${ver.version}",
                    getRepository(ver.group, ver.name, ver.version)
                )
            }

        val manifest = Manifest("dev.koding.launcher.LauncherKt", dependencies)
        outputFile.get().asFile.writeText(Json {
            prettyPrint = true
            encodeDefaults = false
        }.encodeToString(manifest))
    }

    private fun getRepository(group: String, name: String, version: String) =
        project.repositories.filterIsInstance<MavenArtifactRepository>()
            .find {
                val url = "${it.url}${group.replace('.', '/')}/$name/$version/$name-$version.pom"
                println(url)
                runCatching { URL(url).openStream() != null }.isSuccess
            }?.url?.toString()
}