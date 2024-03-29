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

@file:Suppress("UnstableApiUsage")

package dev.koding.launcher.util.system

import dev.koding.launcher.arguments
import dev.koding.launcher.launcherHome
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

object MacUtil {
    private val logger = KotlinLogging.logger {}

    fun runWorkaround(vararg args: String) {
        if (OS.type != OS.Type.MAC || System.getProperty("chimp.patchy") != null) return

        // On macOS, some mods may require the microphone permission to operator
        // properly. We are able to hack around this by creating a small script
        // to restart the launcher if we are not running behind a terminal.

        // The full command which was run to start the launcher
        // It should be escaped properly
        val commandLine = "${JavaUtil.javaExecutable.absolutePath} " +
                ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ") { escape(it) } +
                " -Dchimp.patchy=true -cp ${escape(System.getProperty("java.class.path"))} " +
                "${System.getProperty("chimp.mainClass", "dev.koding.launcher.LauncherKt")} " +
                "${arguments.joinToString(" ")} ${args.joinToString(" ") { escape(it) }}"

        logger.debug { "Running workaround for macOS using command line: $commandLine" }
        val workaroundFile = launcherHome.resolve("run-workaround.sh")
        workaroundFile.writeText(
            """
            #!/bin/bash
            echo "Restarting the launcher"
            $commandLine
            """.trimIndent()
        )
        workaroundFile.setExecutable(true)

        ProcessBuilder(
            "osascript",
            "-e",
            "tell app \"Terminal\" to do script \"${workaroundFile.absolutePath}\""
        ).inheritIO().start()
        exitProcess(0)
    }

    private fun escape(str: String) = StringEscapeUtils.escapeXSI(str)
}