package com.dark.launcher.data.repo

import com.dark.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
private data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage = ChatMessage()
)

@Serializable
private data class ChatMessage(
    val content: String = ""
)

@Serializable
private data class ClaudeResponse(
    val content: List<ClaudeContent> = emptyList()
)

@Serializable
private data class ClaudeContent(
    val text: String = ""
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent = GeminiContent()
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart> = emptyList()
)

@Serializable
private data class GeminiPart(
    val text: String = ""
)

@Singleton
class AskRepository @Inject constructor(
    private val settings: LauncherSettingsRepository
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val jsonType = "application/json".toMediaType()

    suspend fun ask(query: String): String = withContext(Dispatchers.IO) {
        val excerpts = if (settings.webScrapeEnabledFlow.first()) searchFirecrawl(query) else ""
        answer(query, excerpts)
    }

    private suspend fun firecrawlKey(): String {
        val user = settings.firecrawlApiKeyFlow.first()
        return user.ifBlank { BuildConfig.FIRECRAWL_API_KEY }
    }

    private suspend fun askKey(): String {
        val user = settings.askApiKeyFlow.first()
        val provider = settings.askProviderFlow.first()
        if (user.isNotBlank()) return user
        return if (provider == AskProvider.GROQ) settings.groqApiKeyFlow.first() else ""
    }

    private suspend fun searchFirecrawl(query: String): String {
        val key = firecrawlKey()
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

    private val systemPrompt = """
        You are D.A.R.K., a terse, no-nonsense assistant embedded in an Android terminal.
        Answer the user's question directly in a few clear lines. Use the web excerpts
        below when they are relevant; otherwise answer from your own knowledge and say so.
        Never fabricate citations. Keep it under 120 words.
    """.trimIndent()

    private fun userContent(query: String, excerpts: String): String =
        if (excerpts.isBlank()) {
            "Question: $query"
        } else {
            "Question: $query\n\nWeb excerpts from a live search:\n$excerpts"
        }

    private suspend fun answer(query: String, excerpts: String): String {
        val provider = settings.askProviderFlow.first()
        val model = settings.askModelFlow.first()
        val key = askKey()
        if (key.isBlank()) {
            return "ask is offline — add your ${provider.label} API key in D.A.R.K. Settings."
        }
        return when (provider) {
            AskProvider.ANTHROPIC -> claudeAnswer(model, key, query, excerpts)
            AskProvider.GEMINI -> geminiAnswer(model, key, query, excerpts)
            else -> openAiCompatAnswer(provider, model, key, query, excerpts)
        }
    }

    private suspend fun openAiCompatAnswer(
        provider: AskProvider,
        model: String,
        key: String,
        query: String,
        excerpts: String
    ): String {
        val url = when (provider) {
            AskProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            AskProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
            else -> "https://api.groq.com/openai/v1/chat/completions"
        }
        val payload = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("temperature", JsonPrimitive(0.4))
            put("max_tokens", JsonPrimitive(600))
            put(
                "messages",
                kotlinx.serialization.json.JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(systemPrompt))
                        },
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userContent(query, excerpts)))
                        }
                    )
                )
            )
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .post(payload.toRequestBody(jsonType))
            .build()

        val response = client.newCall(request).execute()
        return try {
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IllegalStateException("${provider.label} API ${response.code}")
            }
            val parsed = json.decodeFromString<ChatResponse>(body)
            val content = parsed.choices.firstOrNull()?.message?.content.orEmpty().trim()
            if (content.isEmpty()) throw IllegalStateException("empty answer")
            content
        } finally {
            response.close()
        }
    }

    private suspend fun claudeAnswer(
        model: String,
        key: String,
        query: String,
        excerpts: String
    ): String {
        val payload = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("max_tokens", JsonPrimitive(600))
            put(
                "messages",
                kotlinx.serialization.json.JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userContent(query, excerpts)))
                        }
                    )
                )
            )
        }.toString()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .post(payload.toRequestBody(jsonType))
            .build()

        val response = client.newCall(request).execute()
        return try {
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IllegalStateException("Claude API ${response.code}")
            }
            val parsed = json.decodeFromString<ClaudeResponse>(body)
            val content = parsed.content.joinToString("") { it.text }.trim()
            if (content.isEmpty()) throw IllegalStateException("empty answer")
            content
        } finally {
            response.close()
        }
    }

    private suspend fun geminiAnswer(
        model: String,
        key: String,
        query: String,
        excerpts: String
    ): String {
        val payload = buildJsonObject {
            put(
                "contents",
                kotlinx.serialization.json.JsonArray(
                    listOf(
                        buildJsonObject {
                            put(
                                "parts",
                                kotlinx.serialization.json.JsonArray(
                                    listOf(
                                        buildJsonObject {
                                            put("text", JsonPrimitive(userContent(query, excerpts)))
                                        }
                                    )
                                )
                            )
                        }
                    )
                )
            )
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key")
            .post(payload.toRequestBody(jsonType))
            .build()

        val response = client.newCall(request).execute()
        return try {
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IllegalStateException("Gemini API ${response.code}")
            }
            val parsed = json.decodeFromString<GeminiResponse>(body)
            val content = parsed.candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text }.orEmpty().trim()
            if (content.isEmpty()) throw IllegalStateException("empty answer")
            content
        } finally {
            response.close()
        }
    }
}
