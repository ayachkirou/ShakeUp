package com.company.shakeup

import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.company.shakeup.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.pm.PackageManager

class LoginActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val loginBinding: ActivityLoginBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_login)

        val intentToAccueil = Intent(this, AccueilActivity::class.java)
        val intentToRegister = Intent(this, RegisterActivity::class.java)

        loginBinding.registerId.setOnClickListener {
            startActivity(intentToRegister)
        }

        loginBinding.loginId.setOnClickListener {
            val email = loginBinding.emailUser.text.toString().trim()
            val password = loginBinding.passwordUser.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Veuillez entrer un email valide.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val auth = FirebaseAuth.getInstance()
            auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { signInTask ->
                                if (signInTask.isSuccessful) {
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {
                                        if (areAllPermissionsGranted()) {
                                            intentToAccueil.putExtra("id", userId)
                                            startActivity(intentToAccueil)
                                            finish()
                                        } else {
                                            ActivityCompat.requestPermissions(
                                                this@LoginActivity,
                                                permissions,
                                                100
                                            )
                                        }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Erreur lors de la récupération de l'utilisateur.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Email ou mot de passe incorrect.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        val intentToAccueil = Intent(this, AccueilActivity::class.java)
                        intentToAccueil.putExtra("id", userId)
                        startActivity(intentToAccueil)
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Veuillez accepter toutes les permissions pour utiliser l'application.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}