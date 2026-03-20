package com.example.myapplication2.data.remote

/**
 * Wraps Grok (or DynamicKeyGrokApi) and falls back to OpenAiApi when Grok fails (401, 403, network, etc).
 */
class FallbackGrokApi(
    private val grok: GrokApi,
    private val openai: OpenAiApi,
) {

    suspend fun chat(systemPrompt: String, userPrompt: String): String {
        return try {
            grok.chat(systemPrompt, userPrompt)
        } catch (e: GrokError) {
            openai.chat(systemPrompt, userPrompt)
        } catch (e: Exception) {
            openai.chat(systemPrompt, userPrompt)
        }
    }

    suspend fun chatWithWebSearch(
        systemPrompt: String,
        userPrompt: String,
        webSources: List<String>,
        maxResults: Int,
    ): GrokSearchResult {
        return try {
            grok.chatWithWebSearch(systemPrompt, userPrompt, webSources, maxResults)
        } catch (e: GrokError) {
            val content = openai.chat(systemPrompt, userPrompt)
            GrokSearchResult(content = content, citations = emptyList())
        } catch (e: Exception) {
            val content = openai.chat(systemPrompt, userPrompt)
            GrokSearchResult(content = content, citations = emptyList())
        }
    }
}
