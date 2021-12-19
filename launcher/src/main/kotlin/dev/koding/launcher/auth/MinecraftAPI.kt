package dev.koding.launcher.auth

import dev.koding.launcher.util.httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

private const val CLIENT_TOKEN = "chimp-launcher"

object MinecraftAPI {

    suspend fun getMinecraftProfile(token: String): MinecraftProfile =
        httpClient.get("https://api.minecraftservices.com/minecraft/profile") {
            header("Authorization", "Bearer $token")
        }

    suspend fun login(username: String, password: String): MinecraftLoginResponse =
        httpClient.post("https://authserver.mojang.com/authenticate") {
            contentType(ContentType.Application.Json)
            body = MinecraftLoginRequest(username, password, CLIENT_TOKEN)
        }

    suspend fun validate(accessToken: String): Boolean =
        runCatching {
            httpClient.post<HttpResponse>("https://authserver.mojang.com/validate") {
                contentType(ContentType.Application.Json)
                body = MinecraftValidateRequest(accessToken)
            }
        }.isSuccess

    suspend fun refresh(accessToken: String): MinecraftRefreshResponse =
        httpClient.post("https://authserver.mojang.com/refresh") {
            contentType(ContentType.Application.Json)
            body = MinecraftRefreshRequest(accessToken, CLIENT_TOKEN)
        }

    @Serializable
    data class MinecraftProfile(
        val id: String,
        val name: String
    )

    @Serializable
    data class MinecraftLoginRequest(
        val username: String,
        val password: String,
        val clientToken: String,
        val agent: Agent = Agent(),
        val requestUser: Boolean = true
    ) {
        @Serializable
        data class Agent(
            val name: String = "Minecraft",
            val version: Int = 1
        )
    }

    @Serializable
    data class MinecraftLoginResponse(
        val accessToken: String,
        val clientToken: String,
        val selectedProfile: MinecraftProfile
    )

    @Serializable
    data class MinecraftValidateRequest(
        val accessToken: String
    )

    @Serializable
    data class MinecraftRefreshRequest(
        val accessToken: String,
        val clientToken: String,
        val requestUser: Boolean = true
    )

    @Serializable
    data class MinecraftRefreshResponse(
        val accessToken: String,
        val clientToken: String,
        val selectedProfile: MinecraftProfile
    )

}