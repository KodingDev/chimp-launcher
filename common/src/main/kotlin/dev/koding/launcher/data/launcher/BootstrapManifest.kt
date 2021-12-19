package dev.koding.launcher.data.launcher

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val config: String
)

@Serializable
data class Manifest(
    val main: String,
    val dependencies: List<Dependency>,
    val arguments: List<String> = emptyList(),
) {
    @Serializable
    data class Dependency(
        val artifact: String? = null,
        val repo: String? = null,
        val download: Download? = null
    ) {
        fun toDownload() = download ?: Download.fromMaven(
            artifact ?: error("Unknown artifact"),
            repo ?: "https://repo1.maven.org/maven2"
        )
    }
}