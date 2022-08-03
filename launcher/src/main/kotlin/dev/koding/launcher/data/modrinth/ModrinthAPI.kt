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

package dev.koding.launcher.data.modrinth

import dev.koding.launcher.util.httpClient
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MODRINTH_API_URL = "https://api.modrinth.com/api/v1"

object ModrinthAPI {

    suspend fun search(query: String, limit: Int = 5) = httpClient.get<SearchResponse>("$MODRINTH_API_URL/mod") {
        parameter("query", query)
        parameter("limit", limit)
    }

    suspend fun getVersions(modId: String) =
        httpClient.get<List<Version>>("$MODRINTH_API_URL/mod/${modId.split("-").last()}/version")

    @Serializable
    data class Version(
        val id: String,
        @SerialName("mod_id") val modId: String,
        @SerialName("version_number") val versionNumber: String,
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
                val sha1: String,
                val sha256: String? = null
            )
        }
    }

    @Serializable
    data class SearchResponse(
        val hits: List<ModResult>
    )

    @Serializable
    data class ModResult(
        @SerialName("mod_id") val modId: String,
        val slug: String
    )

}