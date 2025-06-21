package com.company.shakeup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.company.shakeup.databinding.FragmentAuthentificationBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AuthentificationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AuthentificationFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bindingAuthentification = DataBindingUtil.inflate<FragmentAuthentificationBinding>(
            inflater,
            R.layout.fragment_authentification,
            container,
            false
        )
        // Récupérer l'instance de Firestore
        val db = FirebaseFirestore.getInstance()

// Récupérer l'instance de FirebaseAuth (pour obtenir l'ID de l'utilisateur connecté)
        val auth = FirebaseAuth.getInstance()

        bindingAuthentification.btnInscription.setOnClickListener {
            val nom = bindingAuthentification.etName.text.toString().trim()
            val age = bindingAuthentification.etAge.text.toString().trim()
            val sexeId = bindingAuthentification.rgSexe.checkedRadioButtonId
            val ville = bindingAuthentification.etVille.text.toString().trim()
            val telephone = bindingAuthentification.etPhone.text.toString().trim()
            val email = bindingAuthentification.etEmail.text.toString().trim()
            val motDePasse = bindingAuthentification.etPassword.text.toString().trim()

            // Validation des entrées
            if (nom.isEmpty() || age.isEmpty() || sexeId == -1 || ville.isEmpty() || telephone.isEmpty() || email.isEmpty() || motDePasse.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Veuillez entrer un email valide.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidPhoneNumber(telephone)) {
                Toast.makeText(requireContext(), "Veuillez entrer un numéro de téléphone valide.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidPassword(motDePasse)) {
                Toast.makeText(requireContext(), "Le mot de passe doit contenir au moins 6 caractères.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Créer l'objet User ici (après toutes les validations)
            val sexe = if (sexeId == R.id.rbHomme) "Homme" else "Femme"  // tu dois aussi récupérer le sexe !

            val user = User(
                nom = nom,
                age = age,
                sexe = sexe,
                ville = ville,
                telephone = telephone
            )

            // ✅ Créer l'utilisateur dans Firebase Authentication
            auth.createUserWithEmailAndPassword(email, motDePasse)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // ✅ Si la création de l'utilisateur est réussie

                        val userId = auth.currentUser?.uid
                          UserSession.UserId=auth.currentUser?.uid
                        if (userId != null) {
                            // ✅ Enregistrer les autres informations dans Firestore
                            db.collection("users")
                                .document(userId)
                                .set(user)  // Utiliser l'ID de l'utilisateur comme clé dans Firestore
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Utilisateur inscrit avec succès", Toast.LENGTH_SHORT).show()
                                    val action = AuthentificationFragmentDirections.actionAuthentificationFragmentToContactFragment(userId)
                                    bindingAuthentification.root.findNavController().navigate(action)

                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Erreur lors de l'inscription : ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(requireContext(), "Erreur : ID utilisateur non récupéré.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Si la création de l'utilisateur échoue
                        Toast.makeText(requireContext(), "Erreur lors de l'inscription : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }



        return bindingAuthentification.root
    }


private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.length ==10
}
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
private fun isValidPassword(password: String): Boolean {
    return password.length >= 6
}
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AuthentificationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AuthentificationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}