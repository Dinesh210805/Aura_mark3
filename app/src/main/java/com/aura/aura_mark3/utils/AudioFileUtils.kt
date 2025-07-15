package com.aura.aura_mark3.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Audio file utilities for AURA backend integration
 */
object AudioFileUtils {
    
    /**
     * Directory for temporary audio files
     */
    private const val AUDIO_DIR = "aura_audio"
    
    /**
     * Get the audio files directory
     */
    fun getAudioDir(context: Context): File {
        val audioDir = File(context.filesDir, AUDIO_DIR)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir
    }
    
    /**
     * Save recorded audio data to a file
     */
    fun saveAudioToFile(context: Context, audioData: ByteArray, filename: String = "last_recording.wav"): File {
        val audioFile = File(getAudioDir(context), filename)
        try {
            FileOutputStream(audioFile).use { fos ->
                fos.write(audioData)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return audioFile
    }
    
    /**
     * Get the last saved audio file
     */
    fun getLastAudioFile(context: Context): File? {
        val audioFile = File(getAudioDir(context), "last_recording.wav")
        return if (audioFile.exists() && audioFile.length() > 0) {
            audioFile
        } else {
            null
        }
    }
    
    /**
     * Clean up old audio files (keep only the last 5)
     */
    fun cleanupOldAudioFiles(context: Context) {
        val audioDir = getAudioDir(context)
        val files = audioDir.listFiles()?.sortedByDescending { it.lastModified() }
        
        files?.drop(5)?.forEach { file ->
            file.delete()
        }
    }
    
    /**
     * Save bitmap to JPEG file for backend processing
     */
    fun bitmapToJpegFile(bitmap: Bitmap, context: Context, filename: String = "screenshot.jpg"): File {
        val file = File(context.cacheDir, filename)
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }
    
    /**
     * Create temporary audio file with silence (for text-only commands that need audio format)
     */
    fun createSilentAudioFile(context: Context, durationMs: Int = 1000): File {
        val file = File(getAudioDir(context), "silent_audio.wav")
        
        // Create minimal WAV header for silent audio
        val sampleRate = 16000
        val channels = 1
        val bitsPerSample = 16
        val samples = (sampleRate * durationMs / 1000.0).toInt()
        val dataSize = samples * channels * bitsPerSample / 8
        val totalSize = 36 + dataSize
        
        try {
            FileOutputStream(file).use { fos ->
                // WAV header
                fos.write("RIFF".toByteArray())
                fos.write(intToByteArray(totalSize))
                fos.write("WAVE".toByteArray())
                fos.write("fmt ".toByteArray())
                fos.write(intToByteArray(16)) // fmt chunk size
                fos.write(shortToByteArray(1)) // audio format (PCM)
                fos.write(shortToByteArray(channels.toShort()))
                fos.write(intToByteArray(sampleRate))
                fos.write(intToByteArray(sampleRate * channels * bitsPerSample / 8)) // byte rate
                fos.write(shortToByteArray((channels * bitsPerSample / 8).toShort())) // block align
                fos.write(shortToByteArray(bitsPerSample.toShort()))
                fos.write("data".toByteArray())
                fos.write(intToByteArray(dataSize))
                
                // Silent audio data (zeros)
                val silentData = ByteArray(dataSize)
                fos.write(silentData)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        return file
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}
