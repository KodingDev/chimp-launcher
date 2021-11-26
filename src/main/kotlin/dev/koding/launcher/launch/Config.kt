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