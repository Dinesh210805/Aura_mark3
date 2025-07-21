package com.aura.aura_mark3.audio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Log
import com.aura.aura_mark3.utils.AudioFileUtils

class AudioRecorderService : Service() {

    companion object {
        const val CHANNEL_ID = "AURA_AUDIO_RECORDING"
        const val NOTIFICATION_ID = 1
        const val SAMPLE_RATE = 16000
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        const val ACTION_TRANSCRIPTION = "com.aura.aura_mark3.TRANSCRIPTION"
        const val EXTRA_TRANSCRIPTION = "transcription"
        
        // New constants for streaming
        const val CHUNK_DURATION_MS = 2000 // 2 seconds per chunk
        const val SILENCE_THRESHOLD = 1500 // Audio level below this is considered silence
        const val SILENCE_DURATION_MS = 1500 // 1.5 seconds of silence to auto-stop
        const val MIN_RECORDING_DURATION_MS = 1000 // Minimum 1 second recording
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = AtomicBoolean(false)
    private var outputFile: File? = null
    
    // Streaming variables
    private val audioBuffer = mutableListOf<Short>()
    private var lastVoiceDetectedTime = 0L
    private var recordingStartTime = 0L
    private var currentAudioLevel = 0
    
    // Handler for main thread operations
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i("AURA_AUDIO", "AudioRecorderService created with streaming support")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("AURA_AUDIO", "AudioRecorderService onStartCommand called")
        
        if (!checkPermission()) {
            Log.e("AURA_AUDIO", "Missing RECORD_AUDIO permission")
            broadcastError("Missing microphone permission")
            stopSelf()
            return START_NOT_STICKY
        }
        
        try {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.i("AURA_AUDIO", "Started foreground service successfully")
            
            // Start recording with a small delay to ensure service is fully started
            mainHandler.postDelayed({
                startRecording()
            }, 500)
            
        } catch (e: Exception) {
            Log.e("AURA_AUDIO", "Failed to start foreground service", e)
            broadcastError("Failed to start recording service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        return START_STICKY
    }

    private fun broadcastError(message: String) {
        val intent = Intent(ACTION_TRANSCRIPTION).apply {
            putExtra(EXTRA_TRANSCRIPTION, "")
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun checkPermission(): Boolean {
        val permission = android.Manifest.permission.RECORD_AUDIO
        val result = checkSelfPermission(permission)
        return result == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        // Remove TTS shutdown
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AURA Audio Recording",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AURA is listening…")
            .setContentText("Voice command mode active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun startRecording() {
        if (isRecording.get()) {
            Log.w("AURA_AUDIO", "Already recording, ignoring start request")
            return
        }
        
        if (!checkPermission()) {
            Log.e("AURA_AUDIO", "Cannot start recording - missing permission")
            broadcastError("Microphone permission denied")
            stopSelf()
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("AURA_AUDIO", "Invalid buffer size: $bufferSize")
            broadcastError("Audio configuration error")
            stopSelf()
            return
        }

        try {
            audioRecord = AudioRecord(
                AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AURA_AUDIO", "AudioRecord failed to initialize")
                audioRecord?.release()
                broadcastError("Failed to initialize audio recording")
                stopSelf()
                return
            }
            
            outputFile = File(cacheDir, "recording_${System.currentTimeMillis()}.pcm")
            
            Log.i("AURA_AUDIO", "Starting streaming audio recording")
            isRecording.set(true)
            recordingStartTime = System.currentTimeMillis()
            lastVoiceDetectedTime = recordingStartTime
            audioBuffer.clear()
            
            audioRecord?.startRecording()

            recordingThread = Thread {
                processStreamingAudio()
            }
            recordingThread?.start()
            
            Log.i("AURA_AUDIO", "Streaming audio recording started successfully")
            
        } catch (e: SecurityException) {
            Log.e("AURA_AUDIO", "Security exception while creating AudioRecord", e)
            broadcastError("Audio permission denied")
            stopSelf()
        } catch (e: Exception) {
            Log.e("AURA_AUDIO", "Unexpected error starting recording", e)
            broadcastError("Failed to start recording")
            stopSelf()
        }
    }
    
    private fun processStreamingAudio() {
        val buffer = ShortArray(1024)
        val chunkSizeInSamples = SAMPLE_RATE * CHUNK_DURATION_MS / 1000 // 2 seconds worth of samples
        
        try {
            FileOutputStream(outputFile).use { fos ->
                Log.i("AURA_AUDIO", "Starting streaming audio processing")
                
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Calculate audio level for silence detection
                        currentAudioLevel = calculateAudioLevel(buffer, read)
                        
                        // Add to buffer for processing
                        for (i in 0 until read) {
                            audioBuffer.add(buffer[i])
                        }
                        
                        // Write to file for backup
                        val byteBuffer = ByteArray(read * 2)
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            byteBuffer[i * 2] = (sample.toInt() and 0xff).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xff).toByte()
                        }
                        fos.write(byteBuffer)
                        
                        // Check for voice activity
                        if (currentAudioLevel > SILENCE_THRESHOLD) {
                            lastVoiceDetectedTime = System.currentTimeMillis()
                        }
                        
                        // Auto-stop detection: silence for too long
                        val currentTime = System.currentTimeMillis()
                        val silenceDuration = currentTime - lastVoiceDetectedTime
                        val totalRecordingTime = currentTime - recordingStartTime
                        
                        if (silenceDuration > SILENCE_DURATION_MS && 
                            totalRecordingTime > MIN_RECORDING_DURATION_MS) {
                            Log.i("AURA_AUDIO", "Auto-stopping due to silence: ${silenceDuration}ms")
                            break
                        }
                        
                        // Process chunk if we have enough data
                        if (audioBuffer.size >= chunkSizeInSamples) {
                            processAudioChunk()
                        }
                        
                    } else if (read < 0) {
                        Log.w("AURA_AUDIO", "AudioRecord read error: $read")
                        break
                    }
                    
                    Thread.sleep(10) // Small delay to prevent CPU overload
                }
                
                Log.i("AURA_AUDIO", "Streaming audio processing finished")
                
                // Process final chunk if any audio was recorded
                if (audioBuffer.isNotEmpty()) {
                    processFinalAudio()
                }
            }
        } catch (e: Exception) {
            Log.e("AURA_AUDIO", "Error in streaming audio processing", e)
            isRecording.set(false)
        }
    }
    
    private fun calculateAudioLevel(buffer: ShortArray, length: Int): Int {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        return kotlin.math.sqrt(sum / length).toInt()
    }
    
    private fun processAudioChunk() {
        // For now, we'll accumulate chunks and process at the end
        // In a full streaming implementation, you would send this chunk to STT
        Log.d("AURA_AUDIO", "Processing audio chunk: ${audioBuffer.size} samples")
    }
    
    private fun processFinalAudio() {
        if (audioBuffer.isEmpty()) {
            Log.w("AURA_AUDIO", "No audio data to process")
            broadcastError("No audio captured")
            return
        }
        
        Log.i("AURA_AUDIO", "Processing final audio: ${audioBuffer.size} samples")
        
        // Convert accumulated buffer to WAV and transcribe
        val wavFile = File(cacheDir, "final_recording.wav")
        convertBufferToWav(audioBuffer.toShortArray(), wavFile)
        transcribeAudioFile(wavFile)
    }
    
    private fun convertBufferToWav(audioData: ShortArray, wavFile: File) {
        try {
            val totalAudioLen = audioData.size * 2L // 2 bytes per sample
            val totalDataLen = totalAudioLen + 36
            val sampleRate = SAMPLE_RATE
            val channels = 1
            val byteRate = 16 * sampleRate * channels / 8
            
            FileOutputStream(wavFile).use { fos ->
                fos.write(createWavHeader(totalAudioLen, totalDataLen, sampleRate, channels, byteRate))
                
                // Write audio data
                for (sample in audioData) {
                    fos.write(sample.toInt() and 0xff)
                    fos.write((sample.toInt() shr 8) and 0xff)
                }
            }
            
            Log.i("AURA_AUDIO", "Converted buffer to WAV: ${wavFile.length()} bytes")
        } catch (e: Exception) {
            Log.e("AURA_AUDIO", "Error converting buffer to WAV", e)
        }
    }

    private fun stopRecording() {
        if (!isRecording.get()) {
            Log.w("AURA_AUDIO", "Not recording, ignoring stop request")
            return
        }

        Log.i("AURA_AUDIO", "Stopping audio recording")
        isRecording.set(false)

        try {
            // Wait for recording thread to finish
            recordingThread?.join(3000) // 3 second timeout
        } catch (e: InterruptedException) {
            Log.w("AURA_AUDIO", "Recording thread join interrupted", e)
        }

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("AURA_AUDIO", "Error stopping/releasing AudioRecord", e)
        }
        
        audioRecord = null
        recordingThread = null

        outputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                Log.i("AURA_AUDIO", "Processing recorded file: ${file.length()} bytes")
                transcribeAudioFile(file)
            } else {
                Log.w("AURA_AUDIO", "No audio data recorded")
                broadcastError("No audio captured")
            }
        } ?: run {
            Log.w("AURA_AUDIO", "No output file created")
            broadcastError("Recording failed")
        }
        
        // Stop the service
        stopSelf()
    }

    private fun loadApiKey(): String {
        // Try environment variable first
        val envKey = System.getenv("GROQ_API_KEY")
        if (!envKey.isNullOrBlank()) {
            return "Bearer $envKey"
        }
        // Fallback to properties file
        return try {
            val properties = java.util.Properties()
            assets.open("api_keys.properties").use {
                properties.load(it)
            }
            val fileKey = properties.getProperty("groq_api_key", "")
            if (fileKey.isNotBlank()) "Bearer $fileKey" else ""
        } catch (e: Exception) {
            Log.e("AURA_API", "Failed to load API key", e)
            ""
        }
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val totalAudioLen = pcmFile.length()
        val totalDataLen = totalAudioLen + 36
        val sampleRate = SAMPLE_RATE
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8

        val data = ByteArray(1024)
        FileOutputStream(wavFile).use { fos ->
            fos.write(createWavHeader(totalAudioLen, totalDataLen, sampleRate, channels, byteRate))
            pcmFile.inputStream().use { fis ->
                var bytesRead: Int
                while (fis.read(data).also { bytesRead = it } != -1) {
                    fos.write(data, 0, bytesRead)
                }
            }
        }
    }

    private fun createWavHeader(
        totalAudioLen: Long,
        totalDataLen: Long,
        sampleRate: Int,
        channels: Int,
        byteRate: Int
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 16 / 8).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        return header
    }

    private fun transcribeAudioFile(file: File) {
        val wavFile = File(cacheDir, "recording.wav")
        convertPcmToWav(file, wavFile)
        
        // Save the audio file for backend processing
        saveAudioFileForBackend(wavFile)
        
        // For now, return a placeholder transcription
        // The backend will handle actual STT processing
        val placeholderTranscription = "Audio saved for backend processing"
        
        Log.i("AURA_STT", "Audio saved: ${wavFile.length()} bytes")
        
        // Broadcast placeholder result
        val intent = Intent(ACTION_TRANSCRIPTION).apply {
            putExtra(EXTRA_TRANSCRIPTION, placeholderTranscription)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Save audio file for backend processing
     */
    private fun saveAudioFileForBackend(wavFile: File) {
        try {
            // Copy to the AudioFileUtils managed directory
            val audioData = wavFile.readBytes()
            AudioFileUtils.saveAudioToFile(this, audioData, "last_recording.wav")
            Log.i("AURA_AUDIO", "Saved audio file for backend processing: ${audioData.size} bytes")
        } catch (e: Exception) {
            Log.e("AURA_AUDIO", "Failed to save audio file for backend", e)
        }
    }

    // Remove speak() and speakInternal()

}
