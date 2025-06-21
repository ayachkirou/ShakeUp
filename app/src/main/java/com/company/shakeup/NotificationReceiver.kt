package com.company.shakeup

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .cancelAll()
        if (intent?.getBooleanExtra("emergency", false) == true) {
            if (UserSession.isEmergencyActive) return
            UserSession.isEmergencyActive = true

            // Reset after 1 minute
            Handler(Looper.getMainLooper()).postDelayed({
                UserSession.isEmergencyActive = false
            }, 60000L)
        }

        // Envoie un intent de service pour annuler l’auto-réponse
        Intent(context, ShakeDetectionService::class.java).also { serviceIntent ->
            serviceIntent.action = ShakeDetectionService.STOP_AUTO_RESPONSE
            // Sur Android O+, il faut démarrer un service au premier plan
            ContextCompat.startForegroundService(context, serviceIntent)
        }
        if (intent?.getBooleanExtra("emergency", false) == false){
            Log.d("NotificationReceiver", "Utilisateur n'est pas en danger")
        }
        if (intent?.getBooleanExtra("emergency", false) == true) {
            Log.d("NotificationReceiver", "Utilisateur est en danger")

            getLastKnownLocation(context) { location ->

                val locationUrl = if (location != null) {
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "Localisation indisponible"
                }

                val message = "Je suis en danger. Ma localisation : $locationUrl"

                val userId = UserSession.UserId
                Log.d("NotificationReceiver", "User ID: $userId")
                if (userId.isNullOrEmpty()) {
                    Log.e("NotificationReceiver", "ID utilisateur introuvable")
                    return@getLastKnownLocation
                }

                val db = FirebaseFirestore.getInstance()
                db.collection("contacts")
                    .whereEqualTo("idUser", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("NotificationReceiver", "Nombre de contacts trouvés : ${documents.size()}")
                        if (documents.isEmpty) {
                            Log.w("NotificationReceiver", "Aucun contact trouvé pour l'utilisateur")
                            return@addOnSuccessListener
                        }
                        // ENREGISTREMENT DANS LA COLLECTION HISTORIQUE
                        val historique = hashMapOf(
                            "id_user" to userId,
                            "localisation" to locationUrl,
                            "date_danger" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("historique")
                            .add(historique)
                            .addOnSuccessListener {
                                Log.d("NotificationReceiver", "Danger enregistré dans l'historique")
                            }
                            .addOnFailureListener { e ->
                                Log.e("NotificationReceiver", "Erreur lors de l'enregistrement historique: ${e.message}")
                            }

                        for (doc in documents) {
                            val phone = doc.getString("phone")

                            // ENVOI SMS
                            if (!phone.isNullOrEmpty()) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        context.getSystemService(SmsManager::class.java)
                                    } else {
                                        SmsManager.getDefault()
                                    }
                                    smsManager.sendTextMessage(phone, null, message, null, null)
                                    Log.d("NotificationReceiver", "SMS envoyé à $phone")
                                } else {
                                    Log.e("NotificationReceiver", "Permission SEND_SMS non accordée")
                                }
                            }

                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationReceiver", "Erreur Firestore: ${e.message}")
                    }
            }

        }

    }


    private fun getLastKnownLocation(context: Context, onResult: (Location?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("NotificationReceiver", "Permission localisation non accordée")
            onResult(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                onResult(location)
            }
            .addOnFailureListener {
                Log.e("NotificationReceiver", "Impossible d'obtenir la localisation : ${it.message}")
                onResult(null)
            }
    }

    companion object {
        fun handleEmergencyResponse(context: Context, isEmergency: Boolean) {
            // Send intent to start cooldown instead of stopping service
            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = "ACTION_COOLDOWN"
                putExtra("emergency", isEmergency)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
