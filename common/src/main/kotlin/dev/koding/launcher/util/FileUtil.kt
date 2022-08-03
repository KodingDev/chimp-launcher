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

package dev.koding.launcher.util

import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile

val File.sha1: String
    get() {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(readBytes())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

val File.sha256: String
    get() {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(readBytes())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

fun URL.download(file: File) {
    file.parentFile.mkdirs()
    openConnection().apply {
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
        )
    }.getInputStream().use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
}

fun File.extractZip(destination: File, eliminateRoot: Boolean = false) {
    destination.mkdirs()
    ZipFile(this).use { zip ->
        val root = zip.entries().nextElement().name.substringBefore('/')
        val commonRoot = zip.entries().asSequence().all { it.name.startsWith(root) }

        zip.entries().asSequence().forEach { entry ->
            val name = if (commonRoot && eliminateRoot) entry.name.substringAfter(root) else entry.name
            val file = File(destination, name)

            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile.mkdirs()
                zip.getInputStream(entry).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}