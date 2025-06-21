package com.company.shakeup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contacts: List<Contact>,
    private val onEditClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        // Gonfler le layout item_contact pour chaque item du RecyclerView
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        // Lier chaque contact à la vue
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nomContact)
        private val phoneTextView: TextView = itemView.findViewById(R.id.telephoneContact)
        private val emailTextView: TextView = itemView.findViewById(R.id.emailContact)
        private val editButton: ImageView = itemView.findViewById(R.id.btnEdit)
        private val deleteButton: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(contact: Contact) {
            // Mettre à jour les vues avec les données du contact
            nameTextView.text = contact.name
            phoneTextView.text = contact.phone
            emailTextView.text = contact.email
            if (contact.idUser != UserSession.UserId) {
                itemView.visibility = View.GONE // Hide irrelevant contacts
                return
            }

            // Gérer les clics sur les boutons Modifier et Supprimer
            editButton.setOnClickListener {
                // On appelle le callback (onEditClick) en lui passant l'ID du contact
                onEditClick(contact.idContact.toString())
            }

            deleteButton.setOnClickListener {
                // On appelle le callback pour supprimer le contact
                onDeleteClick(contact.idContact.toString())
            }
        }
    }
}
