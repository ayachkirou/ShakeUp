package com.company.shakeup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import android.Manifest
import android.app.PendingIntent
import android.os.Handler
import android.os.Looper

class AudioMonitoringService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1234
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 1024
        private const val THRESHOLD = 2500
        private const val RECORDING_DURATION = 6000L // 6 seconds recording
        private const val DEBOUNCE_TIME = 2000L // 2 seconds between detections
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isInCooldown = false
    private val COOLDOWN_DURATION = 60000L // 1 minute

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_COOLDOWN" -> {
                resetMonitoringState()
                return START_STICKY
            }
        }
        if (!hasRecordingPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isMonitoring) {
            isMonitoring = true
            startAudioMonitoring()
        }
        return START_STICKY
    }
    private fun hasRecordingPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) == PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startAudioMonitoring() {
        serviceScope.launch {
            initializeAudioRecord()

            while (isMonitoring) {
                try {
                    startVoiceActivationDetection()
                    delay(1000) // Check every second
                } catch (e: Exception) {
                    Log.e("AudioMonitoring", "Error in monitoring loop", e)
                }
            }
        }
    }
    private fun initializeAudioRecord() {
        if (!hasRecordingPermission()) {
            Log.e("AudioMonitoring", "Missing recording permission")
            stopSelf()
            return
        }
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            ).apply {
                // Validate initialization state
                if (state == AudioRecord.STATE_UNINITIALIZED) {
                    Log.e("AudioMonitoring", "AudioRecord initialization failed")
                    release()
                    stopSelf()
                }
            }
        } catch (e: SecurityException) {
            Log.e("AudioMonitoring", "Microphone access denied", e)
            stopSelf()
        }
    }

    private fun startVoiceActivationDetection() {
        val buffer = ShortArray(BUFFER_SIZE)
        audioRecord?.startRecording()

        while (isMonitoring) {
            if (isInCooldown) continue // Skip if in cooldown

            val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
            if (read > 0 && !isRecording) {
                val amplitude = buffer.maxOf { abs(it.toInt()) }
                if (amplitude > THRESHOLD) {
                    Log.d("AudioMonitoring", "Voice detected! Starting recording...")
                    isRecording = true
                    serviceScope.launch {
                        try {
                            TranscriptionManager.startRecording(this@AudioMonitoringService)
                            delay(RECORDING_DURATION)

                            val result = TranscriptionManager.stopAndAnalyze(this@AudioMonitoringService)
                            if (result) {
                                triggerEmergencyNotification()
                            }
                        } finally {
                            isRecording = false
                            delay(DEBOUNCE_TIME)
                        }
                    }
                }
            }
        }
    }
    private suspend fun triggerEmergencyNotification() {
        Log.d("AudioMonitoring", "Triggering emergency notification...")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Add version check for channel creation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createEmergencyNotificationChannel()
        }

        // Build notification with actions
        val notification = NotificationCompat.Builder(this, "emergency_channel")
            .setContentTitle("Alerte Vocale Détectée!")
            .setContentText("Répondez dans 30s")
            .setSmallIcon(R.drawable.emergency)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setTimeoutAfter(ShakeDetectionService.AUTO_RESPONSE_DELAY_MS)
            .addAction(
                R.drawable.safe,
                "Non",
                createResponsePendingIntent(false)
            )
            .addAction(
                R.drawable.emergency,
                "Oui",
                createResponsePendingIntent(true)
            )
            .build()

        notificationManager.notify(ShakeDetectionService.EMERGENCY_NOTIFICATION_ID, notification)

        // Schedule auto-response (same as ShakeDetectionService)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (isMonitoring) {
                Log.d("AudioMonitoring", "Auto-réponse déclenchée")
                NotificationReceiver.handleEmergencyResponse(this, true)
                resetMonitoringState()
            }
        }, ShakeDetectionService.AUTO_RESPONSE_DELAY_MS)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createEmergencyNotificationChannel() {
        val channelId = "emergency_channel"
        val channel = NotificationChannel(
            channelId,
            "Emergency Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun createResponsePendingIntent(isEmergency: Boolean): PendingIntent {
        return PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            Intent(this, NotificationReceiver::class.java).apply {
                putExtra("emergency", isEmergency)
                action = "STOP_AUTO_RESPONSE"
            },
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun resetMonitoringState() {
        isInCooldown = true // Enter cooldown
        Handler(Looper.getMainLooper()).postDelayed({
            isInCooldown = false
        }, COOLDOWN_DURATION)
    }

    private fun createNotification(): Notification {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safety Monitoring Active")
            .setContentText("Listening for SOS keywords")
            .setSmallIcon(R.drawable.warning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "audio_monitoring_channel"
        val channel = NotificationChannel(
            channelId,
            "Audio Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background audio monitoring for safety"
        }

        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        isRecording = false
        audioRecord?.stop()
        serviceScope.launch {
            delay(500)
            audioRecord?.release()
        }
        serviceScope.cancel()
    }

}