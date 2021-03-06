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