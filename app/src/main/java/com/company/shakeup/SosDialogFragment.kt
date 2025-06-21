package com.company.shakeup

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot

class SosDialogFragment : DialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnConfirm: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        firestore = FirebaseFirestore.getInstance()

        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.dialog_sos, null)

        radioGroup = view.findViewById(R.id.radioGroupWords)
        btnConfirm = view.findViewById(R.id.btnConfirm)

        val userId = UserSession.UserId

        // Vérifier si l'ID utilisateur est valide
        if (userId != null) {
            loadSosMessage(userId)
        } else {
            Toast.makeText(context, "User not identified", Toast.LENGTH_SHORT).show()
        }

        btnConfirm.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId

            if (selectedId != -1) {
                val selectedRadio = view.findViewById<RadioButton>(selectedId)
                val selectedText = selectedRadio.text.toString()

                UserSession.UserId?.let { it1 -> saveOrUpdateSos(it1, selectedText) }
            } else {
                Toast.makeText(context, "Please select a message", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setView(view)
        return builder.create()
    }

    private fun loadSosMessage(userId: String) {
        // Récupérer le document de l'utilisateur
        val sosRef = firestore.collection("SOS").document(userId)

        sosRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val message = documentSnapshot.getString("message")
                    if (message != null) {
                        // Mettre à jour le RadioButton en fonction du message
                        selectRadioButton(message)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun selectRadioButton(message: String) {
        when (message) {
            "Help me" -> radioGroup.check(R.id.radioHelpMe)
            "I'm in danger" -> radioGroup.check(R.id.radioInDanger)
            "I'm scared" -> radioGroup.check(R.id.radioScared)
            "I need help" -> radioGroup.check(R.id.radioNeedHelp)
            "Please save me" -> radioGroup.check(R.id.radioSaveMe)
        }
    }

    private fun saveOrUpdateSos(userId: String, message: String) {
        val sosRef = firestore.collection("SOS").document(userId)

        val sosData = mapOf(
            "id_SOS" to userId,  // Utilisation de userId comme id_SOS ici ou générer UUID si nécessaire
            "message" to message,
            "id_User" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        sosRef.set(sosData)
            .addOnSuccessListener {
                Toast.makeText(context, "SOS message saved", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
