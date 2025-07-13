package com.aura.aura_mark3.ai

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.Call
import java.util.concurrent.TimeUnit

// Data class for Groq STT response
// Adjust field names as per actual API response
// Example: { "text": "transcribed text here" }
data class SttResponse(
    val text: String
)

interface GroqSttApi {
    @Multipart
    @POST("/openai/v1/audio/transcriptions")
    fun transcribeAudio(
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody // e.g., "whisper-large-v3-turbo"
    ): Call<SttResponse>
}

fun provideGroqSttApi(): GroqSttApi {
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
    return retrofit.create(GroqSttApi::class.java)
} 