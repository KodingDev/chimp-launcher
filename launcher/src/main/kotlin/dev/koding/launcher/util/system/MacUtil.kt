@file:Suppress("UnstableApiUsage")

package dev.koding.launcher.util.system

import com.google.common.escape.Escapers
import dev.koding.launcher.arguments
import dev.koding.launcher.launcherHome
import mu.KotlinLogging
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

object MacUtil {
    private val logger = KotlinLogging.logger {}

    // Escape for BASH
    private val escaper = Escapers.builder()
        .addEscape('\'', "'\"'\"'")
        .addEscape('\\', "\\\\")
        .addEscape(' ', "\\ ")
        .build()

    fun runWorkaround(vararg args: String) {
        if (OS.type != OS.Type.MAC || System.getProperty("chimp.patchy") != null) return

        // On macOS, some mods may require the microphone permission to operator
        // properly. We are able to hack around this by creating a small script
        // to restart the launcher if we are not running behind a terminal.

        // The full command which was run to start the launcher
        // It should be escaped properly
        val commandLine = "${JavaUtil.javaExecutable.absolutePath} " +
                ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ") { escaper.escape(it) } +
                " -Dchimp.patchy=true -cp ${System.getProperty("java.class.path")} " +
                "${System.getProperty("chimp.mainClass", "dev.koding.launcher.LauncherKt")} " +
                "${arguments.joinToString(" ")} ${args.joinToString(" ") { escaper.escape(it) }}"

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
}