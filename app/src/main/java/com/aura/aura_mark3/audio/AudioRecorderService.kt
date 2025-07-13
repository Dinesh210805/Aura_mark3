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
import com.aura.aura_mark3.ai.GroqSttApi
import com.aura.aura_mark3.ai.SttResponse
import com.aura.aura_mark3.ai.provideGroqSttApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.speech.tts.TextToSpeech
import java.util.*

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
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = AtomicBoolean(false)
    private var outputFile: File? = null
    private var tts: TextToSpeech? = null

    private var isTtsInitialized = false


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsInitialized = true
                Log.i("AURA_TTS", "TTS initialized successfully.")
            } else {
                Log.e("AURA_TTS", "TTS Initialization failed")
            }
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (checkPermission()) {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            startRecording()
        } else {
            Log.e("AURA_AUDIO", "Missing RECORD_AUDIO permission")
            stopSelf()
        }
        return START_STICKY
    }

    private fun checkPermission(): Boolean {
        val permission = android.Manifest.permission.RECORD_AUDIO
        val result = checkSelfPermission(permission)
        return result == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AURA Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
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
        if (isRecording.get()) return
        if (!checkPermission()) {
            Log.e("AURA_AUDIO", "Cannot start recording - missing permission")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        try {
            audioRecord = AudioRecord(
                AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
            )

            outputFile = File(cacheDir, "recording.pcm")
            isRecording.set(true)
            audioRecord?.startRecording()

            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                FileOutputStream(outputFile).use { fos ->
                    while (isRecording.get()) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
            }

            recordingThread?.start()
        } catch (e: SecurityException) {
            Log.e("AURA_AUDIO", "Security exception while creating AudioRecord", e)
            stopSelf()
        }
    }

    private fun stopRecording() {
        if (!isRecording.get()) return

        isRecording.set(false)

        try {
            recordingThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null

        outputFile?.let { file ->
            transcribeAudioFile(file)
        }
    }

    private fun loadApiKey(): String {
        return try {
            val properties = java.util.Properties()
            assets.open("api_keys.properties").use {
                properties.load(it)
            }
            "Bearer ${properties.getProperty("groq_api_key", "")}"
        } catch (e: Exception) {
            Log.e("AURA_API", "Failed to load API key", e)
            "Bearer "
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

        val apiKey = loadApiKey()
        val groqApi: GroqSttApi = provideGroqSttApi()

        val requestFile = wavFile.asRequestBody("audio/wav".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", wavFile.name, requestFile)
        val modelPart = "whisper-large-v3".toRequestBody("text/plain".toMediaTypeOrNull())

        val call = groqApi.transcribeAudio(
            authHeader = apiKey,
            file = filePart,
            model = modelPart
        )

        call.enqueue(object : Callback<SttResponse> {
            override fun onResponse(call: Call<SttResponse>, response: Response<SttResponse>) {
                if (response.isSuccessful) {
                    val transcription = response.body()?.text ?: ""
                    Log.i("AURA_STT", "Transcription: $transcription")
                    if (transcription.isNotBlank()) {
                        speak(transcription)
                    } else {
                        speak("Sorry, I couldn’t hear anything.")
                    }

                    val intent = Intent(ACTION_TRANSCRIPTION).apply {
                        putExtra(EXTRA_TRANSCRIPTION, transcription)
                    }
                    sendBroadcast(intent)
                } else {
                    Log.e("AURA_STT", "Transcription failed: ${response.code()} ${response.message()}")
                    speak("Sorry, speech recognition failed with code ${response.code()}.")
                }
            }

            override fun onFailure(call: Call<SttResponse>, t: Throwable) {
                Log.e("AURA_STT", "Transcription error", t)
                speak("Failed to connect to server. Please try again.")
            }
        })
    }

    private fun speak(message: String) {
        if (isTtsInitialized) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w("AURA_TTS", "speak() called before TTS was initialized: \"$message\"")
        }
    }

}
