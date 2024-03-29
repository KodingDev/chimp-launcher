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

import dev.koding.launcher.frame.LauncherFrame
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import java.io.Serializable

fun configureLogging() {
    if (System.getProperty("debug.log") != null) setLogLevel(Level.DEBUG)
}

fun setLogLevel(level: Level) = Configurator.setLevel(LogManager.getRootLogger().name, level)

@Suppress("unused")
@Plugin(name = "LogDisplay", category = "Core", elementType = "appender", printObject = true)
class LogUtil(
    name: String,
    filter: Filter?,
    layout: Layout<out Serializable>,
    ignoreExceptions: Boolean
) : AbstractAppender(name, filter, layout, ignoreExceptions, emptyArray()) {
    companion object {
        @PluginFactory
        @JvmStatic
        fun createAppender(
            @PluginAttribute("name") name: String?,
            @PluginElement("Layout") layout: Layout<out Serializable?>?,
            @PluginElement("Filter") filter: Filter?
        ): LogUtil? {
            if (name == null) {
                LOGGER.error("No name provided for MyCustomAppenderImpl")
                return null
            }
            return LogUtil(name, filter, layout ?: PatternLayout.createDefaultLayout(), true)
        }
    }

    override fun append(event: LogEvent) {
        LauncherFrame.main?.log(layout.toByteArray(event).toString(Charsets.UTF_8))
    }
}