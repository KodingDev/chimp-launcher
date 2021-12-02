package dev.koding.launcher.util

import kotlinx.cli.ArgType

object ListArgument : ArgType<List<String>>(true) {
    override val description = "{ String list }"

    override fun convert(value: kotlin.String, name: kotlin.String): List<kotlin.String> =
        value.split(",").map { it.trim() }
}