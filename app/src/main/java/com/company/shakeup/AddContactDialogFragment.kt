package com.company.shakeup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.company.shakeup.databinding.DialogAddContactBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddContactDialogFragment : DialogFragment() {

    private var _binding: DialogAddContactBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    var contactAddedListener: ContactAddedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = UserSession.UserId
        if (userId == null) {
            Toast.makeText(requireContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        binding.btnAddContact.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val phone = binding.editPhone.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()

            // Validation
            if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                showToast("Veuillez remplir tous les champs.")
                return@setOnClickListener
            }
            if (!isValidPhoneNumber(phone)) {
                showToast("Veuillez entrer un numéro de téléphone valide (10 chiffres).")
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                showToast("Veuillez entrer un email valide.")
                return@setOnClickListener
            }

            // Récupérer le prochain idContact pour l'utilisateur
            db.collection("contacts")
                .whereEqualTo("idUser", userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val maxId = snapshot.documents
                        .mapNotNull { it.getLong("idContact")?.toInt() }
                        .maxOrNull() ?: 0
                    val nextId = maxId + 1

                    val contact = Contact(
                        idContact = nextId,
                        idUser = userId,
                        name = name,
                        phone = phone,
                        email = email
                    )

                    // Enregistrement du contact
                    db.collection("contacts")
                        .add(contact)
                        .addOnSuccessListener {
                            showToast("Contact ajouté avec succès (ID $nextId)")
                            contactAddedListener?.onContactAdded() // ← Appel ici
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            showToast("Erreur lors de l'ajout : ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    showToast("Erreur Firestore : ${e.message}")
                }
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean =
        phone.length == 10 && phone.all { it.isDigit() }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
interface ContactAddedListener {
    fun onContactAdded()
}