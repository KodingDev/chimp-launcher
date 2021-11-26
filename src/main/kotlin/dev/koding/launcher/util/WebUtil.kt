package dev.koding.launcher.util

import dev.koding.launcher.loader.ResourceManager
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import io.ktor.response.*
import java.io.File
import java.net.URL

val httpClient = HttpClient {
    BrowserUserAgent()
    install(JsonFeature) {
        serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    if (System.getProperty("debug.ktor") != null) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}

suspend fun ApplicationCall.respondResource(resource: String) =
    respondBytes(
        ResourceManager::class.java.getResourceAsStream(resource)?.readBytes() ?: error("Invalid resource"),
        ContentType.Text.Html
    )

fun URL.download(file: File) {
    file.parentFile.mkdirs()
    file.outputStream().use {
        openConnection().apply {
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
            )
        }.getInputStream().use {
            it.copyTo(file.outputStream())
        }
    }
}