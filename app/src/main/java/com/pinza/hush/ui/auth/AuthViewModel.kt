package com.pinza.hush.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.pinza.hush.datasource.local.UserPreferencesDataSource
import com.pinza.hush.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email, password).collect { result ->
                result.fold(
                    onSuccess = { firebaseUser ->
                        // ✅ GUARDAR LA SESIÓN EN DATASTORE
                        userPreferencesDataSource.saveUser(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            token = firebaseUser.uid // Usamos el UID como token de sesión
                        )
                        _uiState.update { it.copy(isLoading = false, user = firebaseUser, isSuccess = true) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(email, password).collect { result ->
                result.fold(
                    onSuccess = { firebaseUser ->
                        // ✅ GUARDAR LA SESIÓN EN DATASTORE
                        userPreferencesDataSource.saveUser(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            token = firebaseUser.uid
                        )
                        _uiState.update { it.copy(isLoading = false, user = firebaseUser, isSuccess = true) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(user = null, isSuccess = false) }
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}
