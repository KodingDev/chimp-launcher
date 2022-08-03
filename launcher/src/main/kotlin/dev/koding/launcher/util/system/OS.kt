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


object OS {
    enum class Type(val names: Array<String>) {
        WINDOWS(arrayOf("windows")),
        LINUX(arrayOf("linux")),
        MAC(arrayOf("osx", "macos")),
        UNKNOWN(emptyArray())
    }

    val classifiers = Type.values().flatMap { it.names.toList() }
        .map { "natives-$it" }

    val type: Type
        get() {
            val osName = System.getProperty("os.name")
            return when {
                osName.contains("Windows", true) -> Type.WINDOWS
                osName.contains("Mac", true) -> Type.MAC
                osName.contains("Linux", true) -> Type.LINUX
                else -> Type.UNKNOWN
            }
        }

    val name = when {
        System.getProperty("os.name").startsWith("Windows") -> "windows"
        System.getProperty("os.name").startsWith("Mac") -> "osx"
        System.getProperty("os.name").startsWith("Linux") -> "linux"
        else -> "unknown"
    }

    val version: String = System.getProperty("os.version")
    val arch: String = System.getProperty("os.arch")
}