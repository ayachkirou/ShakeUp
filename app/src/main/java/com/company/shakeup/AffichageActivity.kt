package com.company.shakeup

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.company.shakeup.databinding.ActivityAffichageBinding

class AffichageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val affichage_binding: ActivityAffichageBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_affichage)
        var intent=Intent(this,LoginActivity::class.java)
        affichage_binding.buttonGetStarted.setOnClickListener {
         startActivity(intent)
        }
    }
}