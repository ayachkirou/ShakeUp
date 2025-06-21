package com.company.shakeup.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.company.shakeup.databinding.ItemDangerBinding
import com.company.shakeup.Danger
import com.company.shakeup.R
import java.text.SimpleDateFormat
import java.util.*

class HistoriqueAdapter(private val dangerList: List<Danger>) :
    RecyclerView.Adapter<HistoriqueAdapter.DangerViewHolder>() {

    inner class DangerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateTextView)
        val localisationText: TextView = itemView.findViewById(R.id.localisationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DangerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_danger, parent, false)
        return DangerViewHolder(view)
    }

    override fun onBindViewHolder(holder: DangerViewHolder, position: Int) {
        val danger = dangerList[position]

        // Conversion de la date en format lisible
        if (danger.date_danger != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy à HH:mm:ss", Locale.getDefault()) // Format souhaité
            val dateString = sdf.format(danger.date_danger.toDate()) // Convertir le Timestamp en Date
            holder.dateText.text = dateString
        } else {
            holder.dateText.text = "Date inconnue"
        }

        // Afficher l'URL de la localisation dans la TextView
        if (danger.localisation.isNotEmpty()) {
            holder.localisationText.text = danger.localisation // Affiche l'URL de localisation
        } else {
            holder.localisationText.text = "Localisation non disponible"
        }

        // Gérer le clic sur la vue
        holder.itemView.setOnClickListener {
            val localisation = danger.localisation
            val context = holder.itemView.context

            // Vérifier si la localisation n'est pas vide
            if (localisation.isNotEmpty()) {
                val gmmIntentUri = Uri.parse(localisation) // Utilise directement l'URL de localisation
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                // Vérifier si Google Maps est installé
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                } else {
                    Toast.makeText(context, "Google Maps non installé.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Localisation non disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }



    override fun getItemCount() = dangerList.size
}
