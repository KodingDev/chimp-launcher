package dev.koding.launcher.launch

import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import mu.KotlinLogging

class MinecraftLauncher(val manifest: LauncherManifest) {

    private val logger = KotlinLogging.logger {}
    private val data = hashMapOf<LaunchStage<*>, Any>()

    val config = Config()

    suspend fun run(stage: LaunchStage<*>): Any? {
        logger.info { "Running stage: ${stage.javaClass.simpleName}" }
        val result = stage.run(this)
        result?.let { data[stage] = it }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : LaunchResult> get(stage: LaunchStage<*>): T? {
        (data[stage] as? T)?.let { return it }
        if (stage !in data) return run(stage) as? T
        return null
    }

}

interface LaunchStage<T : LaunchResult> {
    suspend fun run(launcher: MinecraftLauncher): T?
}

interface LaunchResult
object LaunchSuccess : LaunchResult