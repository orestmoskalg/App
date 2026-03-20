package com.example.myapplication2.data.remote

import com.example.myapplication2.BuildConfig
import com.example.myapplication2.core.common.AppJson
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@Serializable
private data class OpenAiMessage(val role: String, val content: String)

@Serializable
private data class OpenAiRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAiMessage>,
    val max_tokens: Int = 4096,
    val temperature: Double = 0.3,
)

@Serializable
private data class OpenAiChoice(val message: OpenAiMessage, val finish_reason: String = "")

@Serializable
private data class OpenAiResponse(val choices: List<OpenAiChoice> = emptyList())

private const val BASE_URL = "https://api.openai.com/v1/chat/completions"

class OpenAiApi(private val apiKey: String) {

    private val json = AppJson
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
            }
        }
        .build()

    suspend fun chat(systemPrompt: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
            val request = OpenAiRequest(
                messages = listOf(
                    OpenAiMessage("system", systemPrompt),
                    OpenAiMessage("user", userPrompt),
                )
            )
            val body = json.encodeToString(OpenAiRequest.serializer(), request).toRequestBody(mediaType)
            val raw = executeRaw(body)
            val response = json.decodeFromString(OpenAiResponse.serializer(), raw)
            response.choices.firstOrNull()?.message?.content ?: ""
        }

    private fun executeRaw(body: okhttp3.RequestBody): String {
        val httpRequest = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = try {
            client.newCall(httpRequest).execute()
        } catch (e: Exception) {
            throw java.io.IOException("OpenAI network error: ${e.message}", e)
        }

        return response.use { res ->
            when (res.code) {
                401, 403 -> throw java.io.IOException("OpenAI: invalid API key (HTTP ${res.code})")
                429 -> throw java.io.IOException("OpenAI rate limit. Retry later.")
                in 500..599 -> throw java.io.IOException("OpenAI server error ${res.code}: ${res.message}")
                else -> if (!res.isSuccessful) throw java.io.IOException("OpenAI error ${res.code}: ${res.message}")
                else res.body?.string() ?: throw java.io.IOException("OpenAI: empty response body")
            }
        }
    }
}
