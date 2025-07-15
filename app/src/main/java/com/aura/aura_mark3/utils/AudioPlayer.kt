package com.aura.aura_mark3.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class to play audio from various sources
 */
class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Play audio from byte array
     */
    fun playAudio(audioData: ByteArray, onComplete: (() -> Unit)? = null) {
        try {
            // Save to temp file
            val tempFile = File.createTempFile("audio_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioData)
            }
            
            // Play the audio file
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepareAsync()
                setOnPreparedListener {
                    start()
                }
                setOnCompletionListener {
                    release()
                    tempFile.delete()
                    onComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AUDIO_PLAYER", "MediaPlayer error: what=$what, extra=$extra")
                    release()
                    tempFile.delete()
                    onComplete?.invoke()
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e("AUDIO_PLAYER", "Error playing audio from byte array", e)
            onComplete?.invoke()
        }
    }
    
    /**
     * Stop current playback
     */
    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.w("AUDIO_PLAYER", "Error stopping audio: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
    
    /**
     * Play audio from HTTP response (static method for backward compatibility)
     */
    companion object {
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
}
