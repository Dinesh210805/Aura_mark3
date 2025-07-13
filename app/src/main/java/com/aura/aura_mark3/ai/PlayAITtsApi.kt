package com.aura.aura_mark3.ai

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

// Data class for PlayAI-TTS request
// See Groq API docs for required fields
// Example: { "model": "playai-tts-1", "input": "Hello world", "voice": "nova" }
data class PlayAITtsRequest(
    val model: String = "playai-tts-1",
    val input: String,
    val voice: String = "nova"
)

interface PlayAITtsApi {
    @POST("/v1/audio/speech")
    fun synthesizeSpeech(
        @Header("Authorization") authHeader: String,
        @Body request: PlayAITtsRequest
    ): Call<ResponseBody>
}

fun providePlayAITtsApi(): PlayAITtsApi {
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
    return retrofit.create(PlayAITtsApi::class.java)
}

fun playAudioFromResponse(
    context: Context,
    responseBody: ResponseBody,
    onComplete: (() -> Unit)? = null,
    onError: ((Exception) -> Unit)? = null
) {
    val TAG = "PlayAI_TTS"
    try {
        // Save to temp file
        val tempFile = File.createTempFile("playai_tts", ".mp3", context.cacheDir)
        FileOutputStream(tempFile).use { fos ->
            fos.write(responseBody.bytes())
        }
        Log.i(TAG, "Audio file saved: ${tempFile.absolutePath}")
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setOnPreparedListener {
                        Log.i(TAG, "MediaPlayer prepared, starting playback.")
                        start()
                    }
                    setOnCompletionListener {
                        Log.i(TAG, "Playback complete.")
                        onComplete?.invoke()
                        tempFile.delete()
                        release()
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        onError?.invoke(Exception("MediaPlayer error: what=$what, extra=$extra"))
                        tempFile.delete()
                        release()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during MediaPlayer setup/playback", e)
                onError?.invoke(e)
                tempFile.delete()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception saving or preparing audio file", e)
        onError?.invoke(e)
    }
} 