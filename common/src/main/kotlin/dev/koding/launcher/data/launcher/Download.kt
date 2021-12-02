@file:OptIn(DelicateCoroutinesApi::class)

package dev.koding.launcher.data.launcher

import dev.koding.launcher.util.download
import dev.koding.launcher.util.sha1
import dev.koding.launcher.util.sha256
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File
import java.net.URL

val logger = KotlinLogging.logger {}

typealias ProgressHandler = (name: String?, progress: Double?) -> Unit

@Serializable
data class Download(
    val url: String,
    val path: String = URL(url).let { "${it.host}/${it.path}" },
    val integrity: Integrity = Integrity(),
    val volatile: Boolean = false
) {
    @Serializable
    data class Integrity(
        val size: Long? = null, val sha1: String? = null, val sha256: String? = null
    ) {
        fun verify(file: File) {
            if (size != null && size != file.length()) error("File size mismatch. Expected $size != ${file.length()}")
            if (sha1 != null && sha1 != file.sha1) error("SHA1 mismatch. Expected $sha1 != ${file.sha1}")
            if (sha256 != null && sha256 != file.sha256) error("SHA256 mismatch. Expected $sha256 != ${file.sha256}")
        }

        fun isValid(file: File) = runCatching { verify(file) }.isSuccess
    }
}

fun Download.download(root: File, strict: Boolean = false): File {
    val url = URL(this.url)
    val destination = if (strict) root else root.resolve(path)

    // Check if the file already exists
    if (destination.exists() && integrity.isValid(destination) && !volatile) {
        logger.debug { "File already exists, skipping: ${destination.absolutePath}" }
        return destination
    }

    // Download the file
    logger.info { "Downloading file: $url" }
    url.download(destination)

    // Verify integrity
    integrity.verify(destination)
    logger.debug { "Downloaded file: ${destination.absolutePath}" }
    return destination
}

suspend fun Collection<Download>.download(
    root: File, progressHandler: ProgressHandler = { _, _ -> }, threads: Int = 5
) {
    var i = 0
    val context = newFixedThreadPoolContext(threads, "Download")
    map {
        CoroutineScope(context).async {
            it.download(root)
            progressHandler(null, ++i / size.toDouble())
        }
    }.joinAll()
}