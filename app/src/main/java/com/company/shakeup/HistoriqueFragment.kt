package com.company.shakeup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.company.shakeup.adapter.HistoriqueAdapter
import com.company.shakeup.databinding.FragmentHistoriqueBinding
import com.company.shakeup.Danger
import com.company.shakeup.UserSession
import com.google.firebase.firestore.FirebaseFirestore

class HistoriqueFragment : Fragment() {

    private lateinit var binding: FragmentHistoriqueBinding
    private val db = FirebaseFirestore.getInstance()
    private val dangerList = mutableListOf<Danger>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_historique, container, false)

        val adapter = HistoriqueAdapter(dangerList)
        binding.recyclerViewHistorique.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHistorique.adapter = adapter

        val userId = UserSession.UserId

        db.collection("historique")
            .whereEqualTo("id_user", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.recyclerViewHistorique.visibility = View.GONE
                    binding.aucunDangerText.visibility = View.VISIBLE
                } else {
                    dangerList.clear()
                    for (doc in documents) {
                        val danger = doc.toObject(Danger::class.java)
                        dangerList.add(danger)
                    }
                    adapter.notifyDataSetChanged()
                    binding.aucunDangerText.visibility = View.GONE
                    binding.recyclerViewHistorique.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                binding.recyclerViewHistorique.visibility = View.GONE
                binding.aucunDangerText.visibility = View.VISIBLE
            }

        return binding.root
    }
}
