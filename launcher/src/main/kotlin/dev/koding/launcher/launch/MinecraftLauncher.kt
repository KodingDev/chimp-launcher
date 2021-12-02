package dev.koding.launcher.launch

import dev.koding.launcher.data.launcher.ProgressHandler
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import mu.KotlinLogging

class MinecraftLauncher(val manifest: LauncherManifest) {

    private val logger = KotlinLogging.logger {}
    private val data = hashMapOf<LaunchStage<*>, Any>()

    var progressHandler: ProgressHandler = { _, _ -> }
    val config = Config()

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun run(stage: LaunchStage<*>): Any? {
        logger.info { "Running stage: ${stage.javaClass.simpleName}" }
        val result = stage.run(this)
        result?.let { data[stage] = it }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(stage: LaunchStage<*>): T? {
        (data[stage] as? T)?.let { return it }
        if (stage !in data) return run(stage) as? T
        return null
    }

}

interface LaunchStage<T> {
    suspend fun run(launcher: MinecraftLauncher): T?
}