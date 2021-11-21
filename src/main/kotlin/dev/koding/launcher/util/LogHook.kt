package dev.koding.launcher.util

import dev.koding.launcher.LauncherFrame
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import java.io.Serializable


@Suppress("unused")
@Plugin(name = "LogDisplay", category = "Core", elementType = "appender", printObject = true)
class LogHook(
    name: String?,
    filter: Filter?,
    layout: Layout<out Serializable>?,
    ignoreExceptions: Boolean
) : AbstractAppender(name, filter, layout, ignoreExceptions, emptyArray()) {
    companion object {
        @PluginFactory
        @JvmStatic
        fun createAppender(
            @PluginAttribute("name") name: String?,
            @PluginElement("Layout") layout: Layout<out Serializable?>?,
            @PluginElement("Filter") filter: Filter?
        ): LogHook? {
            if (name == null) {
                LOGGER.error("No name provided for MyCustomAppenderImpl")
                return null
            }
            return LogHook(name, filter, layout ?: PatternLayout.createDefaultLayout(), true)
        }
    }

    override fun append(event: LogEvent) {
        if (!event.level.isLessSpecificThan(Level.INFO)) return
        LauncherFrame.log(layout.toByteArray(event).toString(Charsets.UTF_8))
    }
}