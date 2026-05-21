package com.jarvis.assistant.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jarvis.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AIService {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val history = mutableListOf<Message>()

    // Keep the last 5 exchanges (10 messages) for context
    private val maxHistory = 10

    private val systemPrompt = """
        You are JARVIS, an advanced AI assistant. Your tone is calm, confident, and slightly futuristic.
        Always respond with short, clear sentences suitable for speech — no bullet points, no markdown,
        no asterisks, no numbered lists, and no special symbols. Two to three sentences maximum per reply.
        Be direct, intelligent, and helpful.
    """.trimIndent()

    suspend fun getResponse(userInput: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            history.add(Message("user", userInput))
            if (history.size > maxHistory) history.removeAt(0)

            val messages = buildList {
                add(Message("system", systemPrompt))
                addAll(history)
            }

            val reqJson = gson.toJson(
                ChatRequest(
                    model = "gpt-4o-mini",
                    messages = messages,
                    maxTokens = 150,
                    temperature = 0.75
                )
            )

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .post(reqJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = http.newCall(request).execute()
            val body = response.body?.string() ?: error("Empty response body")
            if (!response.isSuccessful) error("OpenAI ${response.code}: $body")

            val reply = gson.fromJson(body, ChatResponse::class.java)
                .choices.first().message.content.trim()

            history.add(Message("assistant", reply))
            if (history.size > maxHistory) history.removeAt(0)

            Result.success(reply)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearHistory() = history.clear()

    // ── API data classes ──────────────────────────────────────────────────────

    data class Message(val role: String, val content: String)

    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )

    data class ChatResponse(val choices: List<Choice>)
    data class Choice(val message: Message)
}
