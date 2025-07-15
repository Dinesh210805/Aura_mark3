package com.aura.aura_mark3.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

/**
 * Utility function to play audio from HTTP response
 */
object AudioPlayer {
    fun playAudioFromResponse(
        context: Context,
        responseBody: ResponseBody,
        onComplete: () -> Unit,
        onError: () -> Unit
    ) {
        try {
            // Save the audio data to a temporary file
            val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.mp3")
            
            // Write the response body to the temp file
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Play the audio file
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepareAsync()
                setOnPreparedListener {
                    start()
                }
                setOnCompletionListener {
                    release()
                    tempFile.delete()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AUDIO_PLAYER", "MediaPlayer error: what=$what, extra=$extra")
                    release()
                    tempFile.delete()
                    onError()
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e("AUDIO_PLAYER", "Error playing audio from response", e)
            onError()
        }
    }
}
