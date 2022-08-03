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