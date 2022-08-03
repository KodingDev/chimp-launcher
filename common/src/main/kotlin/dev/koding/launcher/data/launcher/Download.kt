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

@file:OptIn(DelicateCoroutinesApi::class)

package dev.koding.launcher.data.launcher

import dev.koding.launcher.util.URISerializer
import dev.koding.launcher.util.download
import dev.koding.launcher.util.sha1
import dev.koding.launcher.util.sha256
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File
import java.net.URI

val logger = KotlinLogging.logger {}

typealias ProgressHandler = (name: String?, progress: Double?) -> Unit

@Serializable
data class Download(
    @Serializable(with = URISerializer::class)
    val url: URI,
    val path: String = url.let { "${it.host}/${it.path}" },
    val integrity: Integrity = Integrity(),
    val settings: Settings = Settings(),
) {
    companion object {
        fun fromMaven(dependency: String, repository: String = "https://repo1.maven.org/maven2"): Download {
            val (group, artifact, version) = dependency.split(":")
            val path = "${group.replace(".", "/")}/$artifact/$version/$artifact-$version.jar"
            return Download(URI("${repository.removeSuffix("/")}/$path"), path = path)
        }
    }

    @Serializable
    data class Integrity(
        val size: Long? = null, val sha1: String? = null, val sha256: String? = null
    ) {
        fun verify(file: File) {
            if (size != null) {
                logger.trace { "Verifying size of $file to $size" }
                if (size != file.length()) {
                    error("File size mismatch. Expected $size != ${file.length()}")
                }
            }

            if (sha1 != null) {
                logger.trace { "Verifying sha1 of $file to $sha1" }
                if (sha1 != file.sha1) {
                    error("SHA1 mismatch. Expected $sha1 != ${file.sha1}")
                }
            }

            if (sha256 != null) {
                logger.trace { "Verifying sha256 of $file to $sha256" }
                if (sha256 != file.sha256) {
                    error("SHA256 mismatch. Expected $sha256 != ${file.sha256}")
                }
            }
        }

        fun isValid(file: File) = runCatching { verify(file) }.isSuccess
    }

    @Serializable
    data class Settings(
        val volatile: Boolean = false,
        val log: Boolean = true
    )
}

fun Download.download(
    root: File,
    strict: Boolean = false,
    progressHandler: ProgressHandler = { _, _ -> }
): File {
    val destination = if (strict) root else root.resolve(path)

    // Check if the file already exists
    if (destination.exists() && integrity.isValid(destination) && !settings.volatile) {
        if (settings.log) logger.debug { "File already exists, skipping: ${destination.absolutePath}" }
        progressHandler("File already exists, skipping: ${destination.absolutePath}", 1.0)
        return destination
    }

    // Download the file
    if (settings.log) logger.info { "Downloading file: $url" }
    progressHandler("Downloading file: $url", 0.0)
    url.toURL().download(destination)

    // Verify integrity
    integrity.verify(destination)
    if (settings.log) logger.debug { "Downloaded file: ${destination.absolutePath}" }
    progressHandler("Downloaded file: ${destination.absolutePath}", 1.0)
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