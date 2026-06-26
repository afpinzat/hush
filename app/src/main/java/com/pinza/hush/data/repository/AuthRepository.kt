package com.pinza.hush.data.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun logout()  // ✅ suspend
    fun isLoggedIn(): Boolean
    fun getCurrentUserEmail(): String?
}