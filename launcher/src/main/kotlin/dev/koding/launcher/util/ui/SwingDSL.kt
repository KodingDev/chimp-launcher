package dev.koding.launcher.util.ui

import com.formdev.flatlaf.FlatDarkLaf
import dev.koding.launcher.LauncherFrame
import java.awt.Component
import java.awt.Dimension
import java.awt.LayoutManager
import java.awt.Taskbar
import javax.imageio.ImageIO
import javax.swing.*

private val icon = ImageIO.read(LauncherFrame::class.java.getResourceAsStream("/assets/logo.png"))
private var themeApplied = false

fun applySwingTheme() {
    if (themeApplied) return

    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", "Chimp Launcher")
    System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")

    runCatching { Taskbar.getTaskbar().iconImage = icon }
    UIManager.put("ClassLoader", LauncherFrame::class.java.classLoader)
    FlatDarkLaf.setup()

    themeApplied = true
}

@DslMarker
annotation class SwingDSL

@SwingDSL
fun frame(
    title: String = "Chimp Launcher",
    size: Pair<Int, Int>? = null,
    frame: JFrame.() -> Unit
) = JFrame(title).apply(frame)
    .apply {
        if (size != null) setSize(size.first, size.second)
        iconImage = icon
        isResizable = false
        setLocationRelativeTo(null)
        isVisible = true
    }

@SwingDSL
fun dialog(
    title: String = "Chimp Launcher",
    frame: JFrame? = LauncherFrame.frame,
    size: Pair<Int, Int>? = null,
    block: JDialog.() -> Unit
): JDialog {
    applySwingTheme()
    return JDialog(frame, title, true).apply(block).apply {
        if (size != null) setSize(size.first, size.second)
        isResizable = false
        setLocationRelativeTo(null)
        isVisible = true
    }
}

@SwingDSL
fun JFrame.content(content: ContentBuilder.() -> Unit) {
    contentPane = ContentBuilder().apply(content).panel
}

@SwingDSL
fun JDialog.content(content: ContentBuilder.() -> Unit) {
    contentPane = ContentBuilder().apply(content).panel
}

data class ContentBuilder(
    val panel: JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    },
) {
    var padding: Int
        get() = panel.border.getBorderInsets(panel).top
        set(value) {
            panel.border = BorderFactory.createEmptyBorder(value, value, value, value)
        }

    var layout: LayoutManager
        get() = panel.layout
        set(value) {
            panel.layout = value
        }

    operator fun Component.unaryPlus(): Component {
        panel.add(this)
        return this
    }

    operator fun Component.plus(constraint: String): Component {
        panel.add(this, constraint)
        return this
    }

    operator fun Component.plus(constraint: Int): Component {
        panel.add(this, constraint)
        return this
    }

    fun panel(init: ContentBuilder.() -> Unit): JPanel =
        ContentBuilder().apply(init).panel

    fun verticalSpace(height: Int): Component = Box.createVerticalStrut(height)

    fun button(text: String, action: () -> Unit): JButton =
        JButton(text).apply {
            addActionListener { action() }
        }
}

fun <T : JComponent> T.alignX(alignment: Float): T {
    alignmentX = alignment
    return this
}

fun <T : JComponent> T.sized(width: Int, height: Int): T {
    preferredSize = Dimension(width, height)
    return this
}