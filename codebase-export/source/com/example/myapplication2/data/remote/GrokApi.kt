package com.example.myapplication2.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface GrokApi {
    @POST("chat/completions")
    suspend fun createCompletion(@Body request: GrokChatRequest): GrokChatResponse
}

@Serializable
data class GrokChatRequest(
    val model: String,
    val messages: List<GrokMessage>,
    val temperature: Double,
    @SerialName("max_tokens")
    val maxTokens: Int,
)

@Serializable
data class GrokMessage(
    val role: String,
    val content: String,
)

@Serializable
data class GrokChatResponse(
    val choices: List<GrokChoice> = emptyList(),
)

@Serializable
data class GrokChoice(
    val message: GrokMessage,
)
