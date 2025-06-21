package com.company.shakeup

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.company.shakeup.databinding.ActivityAccueilBinding
import com.company.shakeup.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val register_binding: ActivityRegisterBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_register)
        val intent=getIntent()

    }
}