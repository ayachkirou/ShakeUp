package com.company.shakeup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.company.shakeup.databinding.FragmentUpdateProfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User

class UpdateProfil : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val profilBinding = DataBindingUtil.inflate<FragmentUpdateProfilBinding>(
            inflater,
            R.layout.fragment_update_profil,
            container,
            false
        )

        val db = FirebaseFirestore.getInstance()
        val userId = UserSession.UserId

        // 🔴 Déconnexion
        profilBinding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        // ✅ Récupération des infos utilisateur depuis Firestore
        db.collection("users").document(userId.toString()).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    profilBinding.Nom.setText(document.getString("nom") ?: "Nom inconnu")
                    profilBinding.Age.setText(document.getString("age") ?: "Âge inconnu")
                    profilBinding.Ville.setText(document.getString("ville") ?: "Ville inconnue")
                    profilBinding.Telephone.setText(document.getString("telephone") ?: "Téléphone inconnu")

                    // ✅ Gérer le sexe via les RadioButton
                    when (document.getString("sexe")) {
                        "Homme" -> profilBinding.radioHomme.isChecked = true
                        "Femme" -> profilBinding.radioFemme.isChecked = true
                    }
                } else {
                    afficherErreur(profilBinding, "Utilisateur introuvable")
                }
            }
            .addOnFailureListener { e ->
                afficherErreur(profilBinding, "Erreur de chargement")
                Log.e("Firestore", "Erreur lors de la récupération : ", e)
            }

        // ✅ Sauvegarde des modifications
        profilBinding.btnSave.setOnClickListener {
            val nom = profilBinding.Nom.text.toString()
            UserSession.UserName=nom
            val age = profilBinding.Age.text.toString()
            val ville = profilBinding.Ville.text.toString()
            val telephone = profilBinding.Telephone.text.toString()
            val sexe = when (profilBinding.Sexe.checkedRadioButtonId) {
                R.id.radioHomme -> "Homme"
                R.id.radioFemme -> "Femme"
                else -> ""
            }

            val updates = hashMapOf(
                "nom" to nom,
                "age" to age,
                "ville" to ville,
                "telephone" to telephone,
                "sexe" to sexe
            )

            db.collection("users").document(userId.toString())
                .update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d("Firestore", "Profil mis à jour avec succès.")
                    Toast.makeText(requireContext(), "Profil mis à jour avec succès.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Erreur lors de la mise à jour : ", e)
                    Toast.makeText(requireContext(), "Erreur lors de la mise à jour : ", Toast.LENGTH_SHORT).show()
                }
        }

        return profilBinding.root
    }

    private fun afficherErreur(binding: FragmentUpdateProfilBinding, message: String) {
        binding.Nom.setText(message)
        binding.Age.setText(message)
        binding.Ville.setText(message)
        binding.Telephone.setText(message)
        binding.radioHomme.isChecked = false
        binding.radioFemme.isChecked = false
    }
}
