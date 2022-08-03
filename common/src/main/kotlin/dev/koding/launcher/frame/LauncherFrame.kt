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

package dev.koding.launcher.frame

import dev.koding.launcher.util.ui.applySwingTheme
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.frame
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class LauncherFrame(showLog: Boolean = true, size: Pair<Int, Int> = 600 to 400) {
    companion object {
        var main: LauncherFrame? = null
            private set

        fun init() {
            if (main != null) return
            main = LauncherFrame()
        }
    }

    val frame: JFrame

    private val log by lazy { JTextArea().apply { isEditable = false } }
    private val progress by lazy { JProgressBar() }
    private val status by lazy { JLabel("Starting game...").apply { alignmentX = Component.CENTER_ALIGNMENT } }

    init {
        applySwingTheme()
        frame = frame(size = size) {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

            content {
                layout = BorderLayout()

                if (showLog) JScrollPane(log) + BorderLayout.CENTER
                panel {
                    padding = 10

                    +status
                    +verticalSpace(10)
                    +progress
                } + (if (showLog) BorderLayout.SOUTH else BorderLayout.CENTER)
            }
        }
    }

    fun dispose() {
        frame.isVisible = false
        frame.dispose()
    }

    fun update(status: String? = null, progress: Int? = null) {
        progress?.let { this.progress.value = it }
        status?.let { this.status.text = it }
    }

    fun log(message: String) {
        log.append(message)
        log.caretPosition = log.document.length
    }
}