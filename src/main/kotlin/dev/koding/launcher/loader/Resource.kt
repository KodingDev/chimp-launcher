package dev.koding.launcher.loader

import kotlinx.serialization.Serializable

@Serializable
sealed class Resource {
    abstract val name: String
}

@Serializable
data class UrlResource(
    override val name: String,
    val url: String
) : Resource()

@Serializable
data class FileResource(
    override val name: String,
    val path: String
) : Resource()