package com.company.shakeup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.company.shakeup.databinding.ActivityAccueilBinding

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task


class AccueilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding: ActivityAccueilBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_accueil
        )
        checkIfLocationEnabled(this)

        val db = FirebaseFirestore.getInstance()

        val userId = intent.getStringExtra("id").orEmpty()
        Log.d("DebugUserID", "Utilisateur connecté avec ID : $userId")

        UserSession.UserId = userId

        // Sauvegarde persistante de l'ID utilisateur
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("userId", userId)
            .apply()

        // Récupération des infos utilisateur depuis Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nom = document.getString("nom") ?: "Nom inconnu"
                    binding.userName.text = nom
                } else {
                    binding.userName.text = "Utilisateur introuvable"
                }
            }
            .addOnFailureListener { e ->
                binding.userName.text = "Erreur de chargement"
                Log.e("Firestore", "Erreur lors de la récupération : ", e)
            }

        // Bouton SOS
        binding.buttonSOS.setOnClickListener {
            val sosDialog = SosDialogFragment()
            sosDialog.show(supportFragmentManager, "SosDialog")
        }

        // Chargement du fragment par défaut
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentId, HistoriqueFragment())
            .commit()

        // Navigation par onglets
        binding.bottomButtons.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btnHistorique -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentId, HistoriqueFragment())
                        .commit()
                    true
                }

                R.id.btncheck -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentId, VerifierFragment())
                        .commit()
                    true
                }

                R.id.idContact -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentId, UpdateContact())
                        .commit()
                    true
                }

                R.id.btnProfil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentId, UpdateProfil())
                        .commit()
                    true
                }

                else -> false
            }
        }


        startShakeService()
        startAudioMonitoringService()

    }
    private fun startShakeService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Méthode pour Android 8+
            startForegroundService(Intent(this, ShakeDetectionService::class.java))
        } else {
            // Méthode pour versions antérieures
            startService(Intent(this, ShakeDetectionService::class.java))
        }
    }
    private var listenerRegistration: ListenerRegistration? = null

    override fun onStart() {
        super.onStart()

        // Récupération de l'ID utilisateur si nécessaire
        var userId = UserSession.UserId
        if (userId.isNullOrEmpty()) {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            userId = prefs.getString("userId", null)
            UserSession.UserId = userId
        }

        val db = FirebaseFirestore.getInstance()

        listenerRegistration = db.collection("users").document(userId.toString())
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.w("RealtimeUpdate", "Erreur d'écoute en temps réel", error)
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val nom = document.getString("nom")
                    UserSession.UserName = nom
                    findViewById<TextView>(R.id.userName).text = nom
                }
            }
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }
    private fun checkIfLocationEnabled(context: Context) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.d("GPS", "Le GPS est activé")
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Cela affiche la boîte de dialogue pour activer le GPS
                    exception.startResolutionForResult(this@AccueilActivity, 1001)
                } catch (e: Exception) {
                    Log.e("GPS", "Erreur lors de la demande d’activation du GPS : ${e.message}")
                }
            }
        }
    }
    private fun startAudioMonitoringService() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }

        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            try {
                val serviceIntent = Intent(this, AudioMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: SecurityException) {
                Log.e("AudioService", "Permission denied for service start", e)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                1002
            )
        }
    }
}


