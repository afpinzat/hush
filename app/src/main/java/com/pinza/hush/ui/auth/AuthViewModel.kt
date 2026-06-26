package com.hush.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val emailError = validateEmail(email)
        val passwordError = validatePasswordBasic(password)

        if (emailError != null || passwordError != null) {
            _uiState.value = AuthUiState.ValidationError(
                emailError = emailError,
                passwordError = passwordError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email.trim(), password)
            _uiState.value = if (result.isSuccess) AuthUiState.Success
            else AuthUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        val nameError = validateName(name)
        val emailError = validateEmail(email)
        val passwordError = validatePasswordBasic(password)
        val confirmPasswordError = validateConfirmPassword(password, confirmPassword)

        if (nameError != null || emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.value = AuthUiState.ValidationError(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(name, email, password)
            _uiState.value = if (result.isSuccess) AuthUiState.Success
            else AuthUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    // ✅ AGREGAR ESTE MÉTODO
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun getCurrentUserEmail(): String? {
        return authRepository.getCurrentUserEmail()
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    // ── VALIDACIONES ──────────────────────────────────────────────────────
    private fun validateName(name: String): String? = when {
        name.isBlank() || name.trim().length < 2 -> "Ingresa tu nombre completo"
        else -> null
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "El correo es obligatorio"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Ingresa un correo válido"
        else -> null
    }

    private fun validatePasswordBasic(password: String): String? = when {
        password.isBlank() -> "La contraseña es obligatoria"
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "Confirma tu contraseña"
        confirm != password -> "Las contraseñas no coinciden"
        else -> null
    }
}