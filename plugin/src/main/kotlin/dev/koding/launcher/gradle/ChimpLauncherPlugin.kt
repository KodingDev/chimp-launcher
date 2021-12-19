package dev.koding.launcher.gradle

import dev.koding.launcher.gradle.task.CreateBootstrapConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.task

@Suppress("unused")
class ChimpLauncherPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.task<CreateBootstrapConfig>("createBootstrapConfig")
    }

}