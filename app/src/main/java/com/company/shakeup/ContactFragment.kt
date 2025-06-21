package com.company.shakeup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.company.shakeup.databinding.FragmentContactBinding
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Gère l'ajout de contacts liés à un utilisateur et active le bouton "Next" après le premier ajout.
 */
class ContactFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private val args: ContactFragmentArgs by navArgs()
    private lateinit var binding: FragmentContactBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val userId = args.idUser.toString()
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_contact,
            container,
            false
        )


        // Désactiver "Next" jusqu'au premier ajout
        binding.btnSaveContact.isEnabled = false
        binding.btnAddContact.setOnClickListener {
            val nom = binding.editName.text.toString().trim()
            val telephone = binding.editPhone.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()

            // Validation des entrées
            if (nom.isEmpty() || telephone.isEmpty() || email.isEmpty()) {
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

            // Obtenir l'ID suivant pour idContact
            db.collection("contacts")
                .whereEqualTo("idUser", UserSession.UserId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val maxId = snapshot.documents
                        .map { it.getLong("idContact")?.toInt() ?: 0 }
                        .maxOrNull() ?: 0
                    val nextId = maxId + 1

                    // Création et enregistrement du contact
                    val contact = Contact(
                        idContact = nextId,
                        idUser = userId,
                        name = nom,
                        phone = telephone,
                        email = email
                    )
                    db.collection("contacts")
                        .document()      // ou .document(nextId.toString()) si on veut l'utiliser comme ID
                        .set(contact)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Contact ajouté (ID $nextId)", Toast.LENGTH_SHORT).show()
                            // Réinitialiser les champs
                            binding.editName.text?.clear()
                            binding.editPhone.text?.clear()
                            binding.editEmail.text?.clear()
                            // Activer "Next"
                            binding.btnSaveContact.isEnabled = true
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Erreur ajout contact : ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erreur lecture contacts : ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Navigation vers l'écran suivant
        binding.btnSaveContact.setOnClickListener {
            val nom = binding.editName.text.toString().trim()
            val telephone = binding.editPhone.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()
            if (!nom.isEmpty() && !telephone.isEmpty() && !email.isEmpty()) {
                Toast.makeText(requireContext(), " Merci de cliquer sur le “+” pour ajouter le contact  avant de poursuivre.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{

                val action = ContactFragmentDirections.actionContactFragmentToAutorisationFragment(userId)
                binding.root.findNavController().navigate(action)
            }

        }

        return binding.root
    }

    private fun isValidPhoneNumber(phone: String) = phone.length == 10
    private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContactFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
