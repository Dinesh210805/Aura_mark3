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

// Example response: { "coordinates": { "x": 100, "y": 200, "width": 50, "height": 30 } }
data class VlmCoordinates(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
data class VlmResponse(
    val coordinates: VlmCoordinates?
)

interface GroqVlmApi {
    @Multipart
    @POST("/openai/v1/chat/completions")
    fun locateUiElement(
        @Header("Authorization") authHeader: String,
        @Part image: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("query") query: RequestBody
    ): Call<VlmResponse>
}

fun provideGroqVlmApi(): GroqVlmApi {
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
    return retrofit.create(GroqVlmApi::class.java)
} 