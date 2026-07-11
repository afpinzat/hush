package com.pinza.hush.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    val currentUser: FirebaseUser?
    fun login(email: String, password: String): Flow<Result<FirebaseUser>>
    fun register(email: String, password: String): Flow<Result<FirebaseUser>>
    fun logout()
    fun isUserLoggedIn(): Boolean
}
