package com.company.shakeup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeDetectionService : Service(), SensorEventListener {

    // Configuration des seuils et délais
    companion object {
        const val SHAKE_THRESHOLD = 25.0 //vitesse de detection d'accélérométre
        const val SHAKES_REQUIRED = 2
        const val SHAKES_WINDOW_MS = 800L
        const val NOTIFICATION_COOLDOWN_MS = 3600000L // 1h  entre deux notifications
        const val AUTO_RESPONSE_DELAY_MS = 60000L // 1min avant auto-reponse
        const val NOTIFICATION_ID = 1001
        const val EMERGENCY_NOTIFICATION_ID = 101
        const val STOP_AUTO_RESPONSE = "com.company.shakeup.action.STOP_AUTO_RESPONSE"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val handler = Handler(Looper.getMainLooper())
    private val shakeTimestamps = mutableListOf<Long>()
    private var lastNotificationTime = 0L
    private var isWaitingForResponse = false

    private val autoResponseRunnable = Runnable {
        if (isWaitingForResponse) {
            Log.d("ShakeService", "Auto-réponse déclenchée")
            sendEmergencyBroadcast()
            resetDetection()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_AUTO_RESPONSE) {
            resetDetection()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        initializeService()
    }

    // Correction 1: Implémentation de onSensorChanged
    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (isWaitingForResponse || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

            val now = System.currentTimeMillis()
            val acceleration = calculateAcceleration(event.values)

            if (acceleration > SHAKE_THRESHOLD) {
                handleShakeEvent(now)
            }
        } catch (e: Exception) {
            Log.e("ShakeService", "Erreur de détection", e)
        }
    }

    // Correction 2: Définition de initializeService
    private fun initializeService() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (accelerometer == null) {
                Log.e("ShakeService", "Capteur non disponible")
                stopSelf()
                return
            }

            startForegroundService()
            startDetection()
        } catch (e: Exception) {
            Log.e("ShakeService", "Erreur d'initialisation", e)
            stopSelf()
        }
    }

    private fun startForegroundService() {
        createNotificationChannel("foreground_channel", "Shake Detection Service")

        val notification = NotificationCompat.Builder(this, "foreground_channel")
            .setContentTitle("Protection active")
            .setContentText("Détection des secousses en cours")
            .setSmallIcon(R.drawable.warning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun calculateAcceleration(values: FloatArray): Double {
        val (x, y, z) = values
        return sqrt((x * x + y * y + z * z).toDouble())
    }

    private fun handleShakeEvent(timestamp: Long) {
        shakeTimestamps.removeAll { it < timestamp - SHAKES_WINDOW_MS }
        shakeTimestamps.add(timestamp)

        if (shakeTimestamps.size >= SHAKES_REQUIRED && timestamp - lastNotificationTime > NOTIFICATION_COOLDOWN_MS) {
            triggerEmergencyAlert(timestamp)
        }
    }

    private fun triggerEmergencyAlert(timestamp: Long) {
        Log.d("ShakeService", "Alerte déclenchée")
        lastNotificationTime = timestamp
        isWaitingForResponse = true
        shakeTimestamps.clear()
        stopDetection()
        sendEmergencyNotification()
        scheduleAutoResponse()
    }

    private fun sendEmergencyNotification() {
        try {
            createNotificationChannel("emergency_channel", "Emergency Alerts")

            val notification = NotificationCompat.Builder(this, "emergency_channel")
                .setContentTitle("Alerte de sécurité!")
                .setContentText("Êtes-vous en danger ?")
                .setSmallIcon(R.drawable.emergency)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setTimeoutAfter(AUTO_RESPONSE_DELAY_MS)
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

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(EMERGENCY_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("ShakeService", "Erreur notification", e)
        }
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

    private fun scheduleAutoResponse() {
        handler.removeCallbacks(autoResponseRunnable)
        handler.postDelayed(autoResponseRunnable, AUTO_RESPONSE_DELAY_MS)
    }

    private fun sendEmergencyBroadcast() {
        try {
            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("emergency", true)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("ShakeService", "Erreur broadcast", e)
        }
    }


    // Ajouter cette méthode pour annuler l'auto-réponse explicitement
    private fun cancelAutoResponse() {
        Log.d("ShakeService", "Annulation auto-réponse")
        handler.removeCallbacks(autoResponseRunnable) // Annule la tâche planifiée
        isWaitingForResponse = false
        // Annule la notification d'urgence
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(EMERGENCY_NOTIFICATION_ID)
    }
    private fun resetDetection() {
        Log.d("ShakeService", "Réinitialisation complète")
        cancelAutoResponse()
        // Redémarrer la détection après le cooldown
        handler.postDelayed({
            startDetection()
        }, NOTIFICATION_COOLDOWN_MS - AUTO_RESPONSE_DELAY_MS)
    }

    private fun startDetection() {
        try {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        } catch (e: Exception) {
            Log.e("ShakeService", "Erreur de démarrage détection", e)
        }
    }

    private fun stopDetection() {
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.e("ShakeService", "Erreur d'arrêt détection", e)
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    // Correction 3: Implémentation de onAccuracyChanged
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implémentation vide mais nécessaire
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoResponseRunnable)
        stopDetection()
        Log.d("ShakeService", "Service arrêté")
    }
}