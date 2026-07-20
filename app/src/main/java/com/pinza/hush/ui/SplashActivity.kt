package com.pinza.hush.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pinza.hush.datasource.local.UserPreferencesDataSource
import com.pinza.hush.ui.auth.LoginActivity
import com.pinza.hush.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesDataSource: UserPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usamos lifecycleScope para lanzar una corrutina ligera
        lifecycleScope.launch {
            // ✅ Leemos DataStore de forma síncrona (pero en hilo IO internamente)
            // .first() es suspend pero es rapidísimo (< 10ms en leer un archivo pequeño)
            val isLoggedIn = userPreferencesDataSource.isLoggedIn().first()

            if (isLoggedIn) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}