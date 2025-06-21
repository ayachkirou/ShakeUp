package com.company.shakeup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.company.shakeup.databinding.FragmentEditContactBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditContactDialogFragment(private val contactId: String) : DialogFragment() {

    private lateinit var binding: FragmentEditContactBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_edit_contact,
            container,
            false
        )

        // Charger les données actuelles du contact
        loadContactData(contactId.toInt())

        // Bouton pour enregistrer les modifications
        binding.btnSave.setOnClickListener {
            saveContactChanges(contactId.toInt())
        }

        return binding.root
    }

    private fun loadContactData(contactId: Int) {
        db.collection("contacts")
            .whereEqualTo("idContact", contactId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.first()
                    val contact = doc.toObject(Contact::class.java)
                    binding.editName.setText(contact.name)
                    binding.editPhone.setText(contact.phone)
                    binding.editEmail.setText(contact.email)
                } else {
                    Toast.makeText(context, "Contact introuvable", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveContactChanges(contactId: Int) {
        val name = binding.editName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        // Validation des champs
        if (name.isBlank() || phone.isBlank() || email.isBlank()) {
            Toast.makeText(context, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPhoneNumber(phone)) {
            Toast.makeText(context, "Numéro de téléphone invalide", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(context, "Adresse email invalide", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedContact = hashMapOf(
            "name" to name,
            "phone" to phone,
            "email" to email
        )

        db.collection("contacts")
            .whereEqualTo("idContact", contactId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.first().id
                    db.collection("contacts").document(docId)
                        .update(updatedContact as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Contact modifié avec succès", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Contact non trouvé", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length == 10 && phone.all { it.isDigit() }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
