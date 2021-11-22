@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.data.launcher

import dev.koding.launcher.Launcher
import dev.koding.launcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class LocalConfig(
    val config: String
) {
    companion object {
        fun load() = json.decodeFromStream<LocalConfig>(
            Launcher::class.java.getResourceAsStream("/config.json") ?: error("File not found")
        )
    }
}