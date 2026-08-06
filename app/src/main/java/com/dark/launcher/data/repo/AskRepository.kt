package com.dark.launcher.data.repo

import com.dark.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class FcSearchResponse(
    val success: Boolean = false,
    val data: List<FcResult> = emptyList()
)

@Serializable
private data class FcResult(
    val title: String = "",
    val url: String = "",
    val description: String = ""
)

@Serializable
private data class GroqResponse(
    val choices: List<GroqChoice> = emptyList()
)

@Serializable
private data class GroqChoice(
    val message: GroqMessage = GroqMessage()
)

@Serializable
private data class GroqMessage(
    val content: String = ""
)

@Singleton
class AskRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val jsonType = "application/json".toMediaType()

    suspend fun ask(query: String): String = withContext(Dispatchers.IO) {
        val excerpts = searchFirecrawl(query)
        groqAnswer(query, excerpts)
    }

    private fun searchFirecrawl(query: String): String {
        val key = BuildConfig.FIRECRAWL_API_KEY
        if (key.isBlank()) return ""
        val payload = buildJsonObject {
            put("query", JsonPrimitive(query))
            put("limit", JsonPrimitive(4))
        }.toString()

        val request = Request.Builder()
            .url("https://api.firecrawl.dev/v1/search")
            .addHeader("Authorization", "Bearer $key")
            .post(payload.toRequestBody(jsonType))
            .build()

        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful || body.isBlank()) return ""
            val parsed = runCatching { json.decodeFromString<FcSearchResponse>(body) }
                .getOrElse { FcSearchResponse() }
            return parsed.data.mapIndexed { i, r ->
                val title = r.title.ifBlank { "source ${i + 1}" }
                val snippet = r.description.ifBlank { r.url }.take(300)
                "[$i] $title\n$snippet"
            }.joinToString("\n\n")
        }
    }

    private fun groqAnswer(query: String, excerpts: String): String {
        val key = BuildConfig.GROQ_API_KEY
        if (key.isBlank()) {
            return "ask is offline on this build - no embedded API keys. Rebuild with secrets.properties configured."
        }

        val system = """
            You are D.A.R.K., a terse, no-nonsense assistant embedded in an Android terminal.
            Answer the user's question directly in a few clear lines. Use the web excerpts
            below when they are relevant; otherwise answer from your own knowledge and say so.
            Never fabricate citations. Keep it under 120 words.
        """.trimIndent()

        val userContent = if (excerpts.isBlank()) {
            "Question: $query"
        } else {
            "Question: $query\n\nWeb excerpts from a live search:\n$excerpts"
        }

        val payload = buildJsonObject {
            put("model", JsonPrimitive("llama-3.3-70b-versatile"))
            put("temperature", JsonPrimitive(0.4))
            put("max_tokens", JsonPrimitive(600))
            put(
                "messages",
                kotlinx.serialization.json.JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(system))
                        },
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userContent))
                        }
                    )
                )
            )
        }.toString()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .post(payload.toRequestBody(jsonType))
            .build()

        val response = client.newCall(request).execute()
        return try {
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IllegalStateException("Groq API ${response.code}")
            }
            val parsed = json.decodeFromString<GroqResponse>(body)
            val content = parsed.choices.firstOrNull()?.message?.content.orEmpty().trim()
            if (content.isEmpty()) throw IllegalStateException("empty answer")
            content
        } finally {
            response.close()
        }
    }
}
