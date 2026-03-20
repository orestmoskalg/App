package com.example.myapplication2.data.remote

import com.example.myapplication2.BuildConfig
import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.config.RegulationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.pow
import java.util.concurrent.TimeUnit

// ── Request / Response models ──────────────────────────────────────────────────

@Serializable
internal data class GrokMessage(val role: String, val content: String)

@Serializable
internal data class GrokRequest(
    val model: String = MODEL,
    val messages: List<GrokMessage>,
    val max_tokens: Int = DEFAULT_MAX_TOKENS,
    val temperature: Double = 0.3,
    val stream: Boolean = false,
)

@Serializable
internal data class GrokChoice(val message: GrokMessage, val finish_reason: String = "")

@Serializable
internal data class GrokResponse(val choices: List<GrokChoice> = emptyList())

// ── Public result types ────────────────────────────────────────────────────────

data class GrokSearchResult(
    val content: String,
    val citations: List<WebCitation>,
)

data class WebCitation(
    val url: String,
    val title: String,
    val snippet: String = "",
    val publishedDate: String = "",
)

// ── Errors ─────────────────────────────────────────────────────────────────────

sealed class GrokError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unauthorized(msg: String) : GrokError(msg)
    class RateLimited(msg: String) : GrokError(msg)
    class ServerError(val code: Int, msg: String) : GrokError("HTTP $code: $msg")
    class EmptyResponse : GrokError("API returned empty response")
    class ParseError(cause: Throwable) : GrokError("Failed to parse API response", cause)
    class NetworkError(cause: Throwable) : GrokError("Network error: ${cause.message}", cause)
}

// ── Constants ──────────────────────────────────────────────────────────────────
// Using OpenAI gpt-4o-mini: fast, cheap, great for structured JSON output.
// Switch MODEL to "gpt-4o" for higher quality at higher cost.

private const val MODEL               = "gpt-4o-mini"
private const val BASE_URL            = "https://api.openai.com/v1/chat/completions"
private const val DEFAULT_MAX_TOKENS  = 4096
private const val SOCIAL_MAX_TOKENS   = 2048

// ── GrokApi ────────────────────────────────────────────────────────────────────
// NOTE: Class name kept as GrokApi for compatibility with existing codebase.
// Internally calls OpenAI API.

class GrokApi(
    private var apiKey: String,
    private val regulationConfig: RegulationConfig = RegulationConfig(),
) {

    private val json      = AppJson
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /** Hot-swap the API key — new key is used on the very next request. */
    fun updateKey(newKey: String) { apiKey = newKey }

    private val timeoutSec: Long = regulationConfig.apiTimeoutSeconds.toLong().coerceAtLeast(15L)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSec, TimeUnit.SECONDS)
        .readTimeout(timeoutSec, TimeUnit.SECONDS)
        .writeTimeout(timeoutSec, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    // HEADERS only — never BODY (would log the API key)
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
            }
        }
        .build()

    // ── Standard chat completion ───────────────────────────────────────────────

    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): String = withContext(Dispatchers.IO) {
        chatWithRetries { encodeChatBody(systemPrompt, userPrompt, maxTokens) }
    }

    private fun encodeChatBody(systemPrompt: String, userPrompt: String, maxTokens: Int): okhttp3.RequestBody {
        val request = GrokRequest(
            messages = listOf(
                GrokMessage("system", systemPrompt),
                GrokMessage("user", userPrompt),
            ),
            max_tokens = maxTokens,
        )
        return json.encodeToString(GrokRequest.serializer(), request).toRequestBody(mediaType)
    }

    private suspend fun chatWithRetries(buildBody: () -> okhttp3.RequestBody): String {
        val max = regulationConfig.maxRetries.coerceAtLeast(1)
        var last: Exception? = null
        for (attempt in 0 until max) {
            try {
                val raw = executeRaw(buildBody())
                val response = runCatching { json.decodeFromString(GrokResponse.serializer(), raw) }
                    .getOrElse { throw GrokError.ParseError(it) }
                return response.choices.firstOrNull()?.message?.content
                    ?: throw GrokError.EmptyResponse()
            } catch (e: GrokError.RateLimited) {
                last = e
                if (attempt == max - 1) throw e
            } catch (e: GrokError.ServerError) {
                last = e
                if (e.code !in 502..504 || attempt == max - 1) throw e
            } catch (e: GrokError.NetworkError) {
                last = e
                if (attempt == max - 1) throw e
            }
            val backoff = min(10_000L, (2.0.pow(attempt).toLong() * 1000L).coerceAtLeast(500L))
            delay(backoff)
        }
        throw last ?: GrokError.EmptyResponse()
    }

    // ── Web-enriched chat (simulated via system prompt) ────────────────────────
    // OpenAI does not expose a native live-search parameter like Grok.
    // We instruct the model to base its answer on up-to-date MDR/IVDR sources
    // and return structured citations. Real web search can be added later via
    // the OpenAI Responses API (search_tool) when it becomes generally available.

    suspend fun chatWithWebSearch(
        systemPrompt: String,
        userPrompt: String,
        @Suppress("UNUSED_PARAMETER") webSources: List<String> = listOf("web"),
        @Suppress("UNUSED_PARAMETER") maxResults: Int = 10,
        maxTokens: Int = SOCIAL_MAX_TOKENS,
    ): GrokSearchResult = withContext(Dispatchers.IO) {
        val enrichedSystem = """$systemPrompt

IMPORTANT: Your training data includes EU MDR 2017/745, IVDR 2017/746, all MDCG guidance
documents up to 2024, and major regulatory news. Use this knowledge to simulate real
practitioner discussions. Generate realistic citations in the format the caller expects."""

        val content = chatWithRetries {
            val request = GrokRequest(
                messages = listOf(
                    GrokMessage("system", enrichedSystem),
                    GrokMessage("user", userPrompt),
                ),
                max_tokens = maxTokens,
            )
            json.encodeToString(GrokRequest.serializer(), request).toRequestBody(mediaType)
        }
        GrokSearchResult(content = content, citations = emptyList())
    }

    // ── HTTP execution ─────────────────────────────────────────────────────────

    private fun executeRaw(body: okhttp3.RequestBody): String {
        if (apiKey.isBlank()) {
            throw GrokError.Unauthorized(
                "OpenAI API key is not configured. Add OPENAI_API_KEY to secrets.properties and rebuild."
            )
        }
        val httpRequest = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = try {
            client.newCall(httpRequest).execute()
        } catch (e: Exception) {
            throw GrokError.NetworkError(e)
        }

        return response.use { res ->
            when (res.code) {
                401, 403 -> throw GrokError.Unauthorized(
                    "Invalid OpenAI API key (HTTP ${res.code}). Key is set in secrets.properties (OPENAI_API_KEY)."
                )
                429 -> throw GrokError.RateLimited(
                    "OpenAI API rate limit exceeded. Wait a few minutes and try again."
                )
                in 500..599 -> throw GrokError.ServerError(res.code, res.message)
                else -> if (!res.isSuccessful)
                    throw GrokError.ServerError(res.code, res.message)
                else res.body?.string() ?: throw GrokError.EmptyResponse()
            }
        }
    }
}
