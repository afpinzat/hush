package com.pinza.hush.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hush.app.ui.auth.AuthViewModel
import com.pinza.hush.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import com.pinza.hush.ui.auth.LoginActivity

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Muestra el email del usuario actual
        binding.tvUserEmail.text = viewModel.getCurrentUserEmail() ?: ""

        // Botón cerrar sesión
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}