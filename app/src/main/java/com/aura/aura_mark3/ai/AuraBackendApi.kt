package com.aura.aura_mark3.ai

import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * AURA Backend API Integration
 * Replaces direct Groq API calls with intelligent backend orchestration
 */

// Request/Response models for backend communication
data class BackendChatRequest(
    val text: String,
    val session_id: String? = null
)

data class BackendProcessRequest(
    val session_id: String? = null
)

data class BackendChatResponse(
    val success: Boolean,
    val response: String,
    val intent: String?,
    val session_id: String
)

data class BackendProcessResponse(
    val success: Boolean,
    val response: String,
    val intent: String?,
    val action_plan: List<ActionStep>?,
    val session_id: String,
    val processing_time: Float?
)

data class ActionStep(
    val type: String,
    val description: String,
    val x: Int? = null,
    val y: Int? = null,
    val text: String? = null,
    val app_name: String? = null,
    val confidence: Float? = null,
    val method: String? = null,
    val requires_screen: Boolean? = null
)

data class BackendHealthResponse(
    val status: String,
    val services: Map<String, String>,
    val timestamp: String
)

interface AuraBackendApi {
    
    @POST("chat")
    fun chatWithText(
        @Body request: BackendChatRequest
    ): Call<BackendChatResponse>
    
    @Multipart
    @POST("process")
    fun processVoiceAndScreen(
        @Part audio: MultipartBody.Part,
        @Part screenshot: MultipartBody.Part,
        @Part("session_id") sessionId: RequestBody
    ): Call<BackendProcessResponse>
    
    @GET("health")
    fun getHealth(): Call<BackendHealthResponse>
}

/**
 * Provider function for AURA Backend API
 */
fun provideAuraBackendApi(baseUrl: String = "http://10.0.2.2:8000/"): AuraBackendApi {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased for backend processing
        .readTimeout(120, TimeUnit.SECONDS)    // Increased for LangGraph workflow
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(AuraBackendApi::class.java)
}

/**
 * Helper function to convert Bitmap to File for multipart upload
 */
fun bitmapToJpegFile(bitmap: Bitmap, context: android.content.Context): File {
    val file = File(context.cacheDir, "screenshot_${System.currentTimeMillis()}.jpg")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    outputStream.close()
    return file
}

/**
 * Helper function to convert audio file to multipart body
 */
fun audioFileToMultipart(audioFile: File): MultipartBody.Part {
    val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
}

/**
 * Helper function to convert screenshot file to multipart body
 */
fun screenshotFileToMultipart(screenshotFile: File): MultipartBody.Part {
    val requestFile = screenshotFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("screenshot", screenshotFile.name, requestFile)
}
