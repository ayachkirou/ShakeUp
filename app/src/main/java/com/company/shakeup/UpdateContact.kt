package com.company.shakeup
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.databinding.DataBindingUtil
import com.company.shakeup.databinding.FragmentUpdateContactBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class UpdateContact : Fragment() {

    private lateinit var contactAdapter: ContactAdapter
    private val contactList = mutableListOf<Contact>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentUpdateContactBinding>(
            inflater,
            R.layout.fragment_update_contact,
            container,
            false
        )

        val recyclerView = binding.recyclerContacts
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadContacts()

        binding.btnAjouterContact.setOnClickListener {
            val dialog = AddContactDialogFragment()
            dialog.contactAddedListener = object : ContactAddedListener {
                override fun onContactAdded() {
                    loadContacts() // Recharge la liste
                }
            }
            dialog.show(childFragmentManager, "AddContactDialog")
        }


        // Initialisation de l'adaptateur avec les callbacks
        contactAdapter = ContactAdapter(contactList, ::onEditClick, ::onDeleteClick)
        recyclerView.adapter = contactAdapter

        return binding.root
    }

    // Callback de modification du contact
    private fun onEditClick(contactId: String) {
        // Ici on ouvre le DialogFragment pour éditer le contact
        val dialog = EditContactDialogFragment(contactId)
        dialog.show(childFragmentManager, "EditContactDialog")
    }

    // Callback de suppression du contact
    private fun onDeleteClick(contactId: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("contacts")
                .whereEqualTo("idUser", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() > 1) {
                        // Supprimer le contact
                        db.collection("contacts")
                            .whereEqualTo("idContact", contactId.toInt()) // attention à l'utilisation de toInt() ici
                            .get()
                            .addOnSuccessListener { result ->
                                for (doc in result) {
                                    db.collection("contacts").document(doc.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Contact supprimé", Toast.LENGTH_SHORT).show()
                                            loadContacts() // Recharger la liste après suppression
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Erreur de suppression", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                    } else {
                        Toast.makeText(context, "Impossible de supprimer. Au moins un contact est requis.", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(context, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
        }
    }


    // Charger les contacts depuis Firestore

    private fun loadContacts() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("contacts")
                .whereEqualTo("idUser", userId)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Toast.makeText(context, "Erreur de chargement en temps réel", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        // Vide la liste avant de la remplir avec les nouveaux contacts
                        contactList.clear()

                        // Parcours les documents récupérés et les ajoute à la liste
                        for (document in snapshots.documents) {
                            val contact = document.toObject(Contact::class.java)
                            if (contact != null) {
                                contactList.add(contact)
                            }
                        }

                        // Notifie l'adaptateur pour qu'il rafraîchisse le RecyclerView
                        contactAdapter.notifyDataSetChanged()
                    }
                }
        } else {
            Toast.makeText(context, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
        }
    }

}

