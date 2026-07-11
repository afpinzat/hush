package com.pinza.hush.utils

import android.util.Patterns

object Validator {

    /**
     * Valida si un correo electrónico tiene un formato correcto.
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailPattern)
    }

    /**
     * Valida si una contraseña cumple con el mínimo de 6 caracteres.
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Lógica de negocio: Una canción es válida si tiene título y artista no vacíos.
     */
    fun isSongValid(title: String, artist: String): Boolean {
        return title.isNotBlank() && artist.isNotBlank()
    }
}
