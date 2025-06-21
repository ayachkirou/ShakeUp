package com.company.shakeup

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

object TranscriptionManager {

    private var recorder: MediaRecorder? = null
    private lateinit var audioFile: File

    fun startRecording(context: Context) {
        try {
            audioFile = File(context.cacheDir, "sos_audio.mp4") // Use MP4 container

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000) // Match Whisper's expected sample rate
                setAudioChannels(1)         // Mono audio
                setOutputFile(audioFile.absolutePath)

                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("AudioRec", "MediaRecorder prepare failed", e)
                    release()
                    return
                }

                start()
                Log.d("AudioRec", "Recording started successfully")
            }
        } catch (e: Exception) {
            Log.e("AudioRec", "MediaRecorder initialization failed", e)
            recorder?.release()
            recorder = null
        }
    }

    suspend fun stopAndAnalyze(context: Context): Boolean {
        try {
            recorder?.apply {
                try {
                    stop()
                    // Add cleanup delay
                    delay(300)
                } catch (e: Exception) {
                    Log.e("AudioRec", "Stop failed", e)
                }
                release()
            }
            recorder = null

            if (!::audioFile.isInitialized || !audioFile.exists()) {
                Log.e("TranscriptionManager", "Audio file not found")
                return false
            }

            // Send to backend
            val userId = UserSession.UserId ?: run {
                Log.e("TranscriptionManager", "No user ID found")
                return false
            }

            val response = WhisperRepository.sendAudioToBackend(context, audioFile, userId)

            // Process response FIRST
            val result = response?.isSuccessful == true && response.body()?.match == true

            // Delete file AFTER processing response
            if (::audioFile.isInitialized && audioFile.exists()) {
                audioFile.delete()
                Log.d("TranscriptionManager", "Audio file deleted")
            }

            return result

        } catch (e: Exception) {
            Log.e("TranscriptionManager", "Analysis failed", e)
            // Ensure file deletion even on error
            if (::audioFile.isInitialized && audioFile.exists()) {
                audioFile.delete()
            }
            return false
        }
    }
}
