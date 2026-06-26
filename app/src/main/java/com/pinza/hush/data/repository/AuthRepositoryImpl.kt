package com.pinza.hush.data.repository

import com.pinza.hush.data.datasource.local.UserPreferencesDataSource
import com.pinza.hush.data.datasource.remote.FirebaseAuthDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuthDataSource,
    private val userPreferences: UserPreferencesDataSource
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // 1. Autenticar en Firebase
            val authResult = firebaseAuth.signIn(email, password)

            if (authResult.isSuccess) {
                val userId = authResult.getOrThrow()

                // 2. Obtener datos del usuario
                val user = firebaseAuth.getCurrentUser()
                user?.let {
                    // 3. Guardar localmente
                    userPreferences.saveUser(
                        id = it.uid,
                        name = it.displayName ?: "",
                        email = it.email ?: "",
                        token = it.uid // O usar un token real
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(authResult.exceptionOrNull() ?: Exception("Error en login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        return try {
            // 1. Crear usuario en Firebase Auth
            val signUpResult = firebaseAuth.signUp(email, password)

            if (signUpResult.isSuccess) {
                val userId = signUpResult.getOrThrow()

                // 2. Actualizar perfil con nombre
                firebaseAuth.updateProfile(userId, name)

                // 3. Obtener usuario y guardar localmente
                val user = firebaseAuth.getCurrentUser()
                user?.let {
                    userPreferences.saveUser(
                        id = it.uid,
                        name = it.displayName ?: name,
                        email = it.email ?: email,
                        token = it.uid
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(signUpResult.exceptionOrNull() ?: Exception("Error en registro"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        userPreferences.clearUser()  // ✅ clearUser es suspend
    }

    override fun isLoggedIn(): Boolean {
        // Podrías verificar tanto Firebase como local
        return firebaseAuth.isUserLoggedIn()
    }

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.getCurrentUser()?.email
    }
}