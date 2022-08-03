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

import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import java.io.File

class Config {

    private val config = hashMapOf<String, ConfigValue<*>>()

    operator fun plusAssign(config: Config) {
        config.config.forEach { (key, value) -> this.config[key] = value }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: ConfigValue<T>) = config[key.name]?.value as? T

    operator fun <T> set(key: ConfigValue<T>, value: T?) {
        if (value == null) config.remove(key.name) else config[key.name] = ConfigValue(key.name, value)
    }

}

open class ConfigValue<T>(
    var name: String,
    var value: T? = null
)

object LauncherDirectory : ConfigValue<File>("launcherDirectory")
object GameDirectory : ConfigValue<File>("gameDirectory")
object AuthDirectory : ConfigValue<File>("authDirectory")
object AssetsDirectory : ConfigValue<File>("assetsDirectory")
object JavaDirectory : ConfigValue<File>("javaDirectory")
object LibraryDirectory : ConfigValue<File>("libraryDirectory")
object ResourcesDirectory : ConfigValue<File>("resourcesDirectory")
object StartJarPath : ConfigValue<File>("startJarPath")
object ExtraArgs : ConfigValue<LauncherManifest.Arguments>("extraArgs")
object StartupHandler : ConfigValue<(MinecraftLauncher) -> Unit>("startupHandler")