package com.pinza.hush.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hush.app.ui.auth.AuthUiState
import com.hush.app.ui.auth.AuthViewModel
import com.pinza.hush.databinding.ActivityRegisterBinding
import com.pinza.hush.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        // Flecha atrás
        binding.btnBack.setOnClickListener { finish() }

        // Botón registrar
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            android.util.Log.d("REGISTER_ACTIVITY", "Registrando - Name: $name, Email: $email")

            viewModel.register(name, email, password, confirmPassword)
        }

        // Enlace a login
        binding.tvLoginLink.setOnClickListener {
            finish() // Regresa a LoginActivity
        }

        // Limpiar errores mientras escribe
        binding.etName.doAfterTextChanged { binding.tilName.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.etConfirmPassword.doAfterTextChanged { binding.tilConfirmPassword.error = null }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    android.util.Log.d("REGISTER_ACTIVITY", "State recibido: $state")

                    when (state) {
                        is AuthUiState.Idle -> {
                            setLoading(false)
                        }

                        is AuthUiState.Loading -> {
                            setLoading(true)
                        }

                        is AuthUiState.ValidationError -> {
                            setLoading(false)
                            binding.tilName.error = state.nameError
                            binding.tilEmail.error = state.emailError
                            binding.tilPassword.error = state.passwordError
                            binding.tilConfirmPassword.error = state.confirmPasswordError

                            // Mostrar Toast con el primer error
                            val firstError = listOf(
                                state.nameError,
                                state.emailError,
                                state.passwordError,
                                state.confirmPasswordError
                            ).firstOrNull { !it.isNullOrEmpty() }

                            firstError?.let {
                                Toast.makeText(this@RegisterActivity, it, Toast.LENGTH_SHORT).show()
                            }
                        }

                        is AuthUiState.Error -> {
                            setLoading(false)
                            android.util.Log.e("REGISTER_ACTIVITY", "Error: ${state.message}")

                            // Mostrar error en un Toast
                            Toast.makeText(
                                this@RegisterActivity,
                                "Error: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            // También mostrar en el campo de email como fallback
                            binding.tilEmail.error = "Error: ${state.message}"
                        }

                        is AuthUiState.Success -> {
                            setLoading(false)
                            android.util.Log.d("REGISTER_ACTIVITY", "Registro exitoso, navegando a Main")

                            // Resetear estado antes de navegar
                            viewModel.resetState()

                            // Navegar a MainActivity
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressRegister.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "Registrando..." else "Registrar"
    }
}