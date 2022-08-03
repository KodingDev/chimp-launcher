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

package dev.koding.launcher.util.system

import java.io.File

object JavaUtil {

    val javaExecutable = getJavaPath(File(System.getProperty("java.home")))

    fun getJavaPath(root: File) = when (OS.type) {
        OS.Type.WINDOWS -> root.resolve("bin/java.exe")
        OS.Type.MAC -> root.resolve("jre.bundle")
            .let { if (it.exists()) it.resolve("Contents/Home") else root }
            .resolve("bin/java")

        OS.Type.LINUX -> root.resolve("bin/java")
        else -> throw IllegalStateException("Unsupported OS: ${OS.type}")
    }

}