package com.company.shakeup

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.company.shakeup.databinding.FragmentAutorisationBinding

class AutorisationFragment : Fragment() {

    private val args: AutorisationFragmentArgs by navArgs()
    private lateinit var binding: FragmentAutorisationBinding

    // Liste des permissions à demander
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.SEND_SMS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAutorisationBinding.inflate(inflater, container, false)

        binding.btnRequestPermissions.setOnClickListener {
            if (arePermissionsGranted()) {
                navigateToAccueil()
            } else {
                requestPermissions(permissions, 100)
            }
        }

        return binding.root
    }

    private fun arePermissionsGranted(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Vérifier si la permission pour les notifications a été accordée
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Demander la permission à l'utilisateur
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun navigateToAccueil() {
        Toast.makeText(
            requireContext(),
            "Permissions accordées. Bienvenue !",
            Toast.LENGTH_SHORT
        ).show()
        // Démarrage de l'Activity Accueil
        val intent = Intent(requireContext(), AccueilActivity::class.java)
        intent.putExtra("id",args.userId.toString())
        startActivity(intent)
        // Facultatif : fermer le fragment ou l'activité hôte
        requireActivity().finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                navigateToAccueil()
            } else {
                Toast.makeText(requireContext(), "Veuillez accepter toutes les permissions.", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // La permission de notification a été accordée
                Toast.makeText(requireContext(), "Permission de notification accordée.", Toast.LENGTH_SHORT).show()
            } else {
                // La permission de notification n'a pas été accordée
                Toast.makeText(requireContext(), "Permission de notification refusée.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Exemple de méthode pour créer et afficher une notification
    private fun createNotification(context: Context) {
        val channelId = "default_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Titre de la notification")
            .setContentText("C'est une notification qui apparaît même quand l'écran est verrouillé.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}
