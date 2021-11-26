package dev.koding.launcher.data.modrinth

import dev.koding.launcher.data.minecraft.manifest.Asset
import dev.koding.launcher.util.httpClient
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MODRINTH_API_URL = "https://api.modrinth.com/api/v1"

object ModrinthAPI {

    suspend fun search(query: String) = httpClient.get<SearchResponse>("$MODRINTH_API_URL/mod") {
        parameter("query", query)
    }

    suspend fun getVersions(modId: String) =
        httpClient.get<List<Version>>("$MODRINTH_API_URL/mod/${modId.split("-").last()}/version")

    @Serializable
    data class Version(
        val id: String,
        @SerialName("mod_id") val modId: String,
        val name: String,
        val files: List<File>
    ) {
        @Serializable
        data class File(
            val filename: String,
            val url: String,
            val hashes: Hashes
        ) {
            @Serializable
            data class Hashes(
                val sha1: String
            )
        }

        val assets = files.map { Asset(it.url, sha1 = it.hashes.sha1, path = "modrinth/${modId}/${id}/${it.filename}") }
    }

    @Serializable
    data class SearchResponse(
        val hits: List<ModResult>
    )

    @Serializable
    data class ModResult(
        @SerialName("mod_id") val modId: String,
        val title: String
    )

}