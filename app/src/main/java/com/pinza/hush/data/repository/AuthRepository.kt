package com.pinza.hush.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pinza.hush.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : IAuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override fun login(email: String, password: String): Flow<Result<FirebaseUser>> = flow {
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                emit(Result.success(it))
            } ?: emit(Result.failure(Exception("Usuario no encontrado")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun register(email: String, password: String): Flow<Result<FirebaseUser>> = flow {
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                emit(Result.success(it))
            } ?: emit(Result.failure(Exception("Error al crear usuario")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
