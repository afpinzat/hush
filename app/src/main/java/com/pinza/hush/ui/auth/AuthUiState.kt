package com.hush.app.ui.auth

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()

    data class ValidationError(
        val nameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    ) : AuthUiState()

    data class Error(val message: String) : AuthUiState()
}