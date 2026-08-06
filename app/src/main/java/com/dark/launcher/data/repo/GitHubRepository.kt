package com.dark.launcher.data.repo

import com.dark.launcher.data.model.GitStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GhResponse(val data: GhData)

@Serializable
private data class GhData(val viewer: GhViewer)

@Serializable
private data class GhViewer(
    val repositories: GhRepos,
    val contributionsCollection: GhContributions
)

@Serializable
private data class GhRepos(val totalCount: Int, val nodes: List<GhRepo> = emptyList())

@Serializable
private data class GhRepo(val stargazerCount: Int = 0)

@Serializable
private data class GhContributions(val contributionCalendar: GhCalendar)

@Serializable
private data class GhCalendar(val totalContributions: Int, val weeks: List<GhWeek> = emptyList())

@Serializable
private data class GhWeek(val contributionDays: List<GhDay> = emptyList())

@Serializable
private data class GhDay(val date: String, val contributionCount: Int)

@Singleton
class GitHubRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchStats(token: String): GitStats = withContext(Dispatchers.IO) {
        val query = """
            query {
              viewer {
                repositories(first: 100, ownerAffiliations: OWNER, isFork: false) {
                  totalCount
                  nodes { stargazerCount }
                }
                contributionsCollection {
                  contributionCalendar {
                    totalContributions
                    weeks {
                      contributionDays { date contributionCount }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val payload = kotlinx.serialization.json.buildJsonObject {
            put("query", kotlinx.serialization.json.JsonPrimitive(query))
        }.toString()

        val request = Request.Builder()
            .url("https://api.github.com/graphql")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IllegalStateException("GitHub API ${response.code}")
            }
            parse(body)
        }
    }

    private fun parse(body: String): GitStats {
        val resp = json.decodeFromString<GhResponse>(body)
        val viewer = resp.data.viewer

        val repoCount = viewer.repositories.totalCount
        val stars = viewer.repositories.nodes.sumOf { it.stargazerCount }
        val commits = viewer.contributionsCollection.contributionCalendar.totalContributions

        val monthMap = mutableMapOf<String, Int>()
        viewer.contributionsCollection.contributionCalendar.weeks.forEach { week ->
            week.contributionDays.forEach { day ->
                if (day.date.length >= 7) {
                    val month = day.date.substring(0, 7)
                    monthMap[month] = (monthMap[month] ?: 0) + day.contributionCount
                }
            }
        }
        val bestMonth = monthMap.maxByOrNull { it.value }?.key ?: "N/A"

        return GitStats(
            repos = repoCount,
            stars = stars,
            commits = commits,
            bestMonth = bestMonth,
            lastSync = System.currentTimeMillis()
        )
    }
}
