package dev.koding.launcher

import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.frame
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

object LauncherFrame {
    var frame: JFrame? = null

    private val log by lazy { JTextArea().apply { isEditable = false } }
    private val progress by lazy { JProgressBar() }
    private val status by lazy { JLabel("Starting game...").apply { alignmentX = Component.CENTER_ALIGNMENT } }

    fun create() {
        frame = frame(size = 600 to 400) {
            content {
                layout = BorderLayout()

                JScrollPane(log) + BorderLayout.CENTER
                panel {
                    padding = 10

                    +status
                    +verticalSpace(10)
                    +progress
                } + BorderLayout.SOUTH
            }
        }
    }

    fun cleanup() {
        frame ?: return
        frame!!.isVisible = false
        frame!!.dispose()
    }

    fun update(status: String? = null, progress: Int? = null) {
        frame ?: return
        progress?.let { this.progress.value = it }
        status?.let { this.status.text = it }
    }

    fun log(message: String) {
        frame ?: return
        log.append(message)
        log.caretPosition = log.document.length
    }
}