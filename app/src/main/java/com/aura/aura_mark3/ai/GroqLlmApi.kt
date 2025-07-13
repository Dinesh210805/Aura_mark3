package com.aura.aura_mark3.ai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Call
import java.util.concurrent.TimeUnit

// Data classes for Groq LLM request/response

data class LlmMessage(
    val role: String, // "user" or "system"
    val content: String
)

data class LlmRequest(
    val model: String, // e.g., "llama-3.3-70b-versatile"
    val messages: List<LlmMessage>
)

data class LlmChoice(
    val message: LlmMessage
)

data class LlmResponse(
    val choices: List<LlmChoice>
)

interface GroqLlmApi {
    @POST("/openai/v1/chat/completions")
    fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: LlmRequest
    ): Call<LlmResponse>
}

fun provideGroqLlmApi(): GroqLlmApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    return retrofit.create(GroqLlmApi::class.java)
} 