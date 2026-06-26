package com.pinza.hush.data.datasource.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_TOKEN = stringPreferencesKey("user_token")
    }

    // ✅ Guardar usuario
    suspend fun saveUser(id: String, name: String, email: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = id
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
            preferences[USER_TOKEN] = token
        }
    }

    // ✅ Limpiar datos
    suspend fun clearUser() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ✅ Obtener nombre (Flow)
    fun getUserName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME]
        }
    }

    // ✅ Obtener email (Flow)
    fun getUserEmail(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL]
        }
    }

    // ✅ Obtener token (Flow)
    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TOKEN]
        }
    }

    // ✅ Verificar si está logueado (Flow)
    fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TOKEN] != null
        }
    }

    // ✅ OBTENER DATOS DE FORMA SÍNCRONA (CORREGIDO)
    suspend fun getUserDataSync(): UserData? {
        val preferences = context.dataStore.data.first()  // ✅ first() retorna un valor
        val id = preferences[USER_ID]
        val name = preferences[USER_NAME]
        val email = preferences[USER_EMAIL]
        val token = preferences[USER_TOKEN]

        return if (id != null && token != null) {
            UserData(id, name ?: "", email ?: "", token)
        } else {
            null
        }
    }
}

// ✅ Data class para retornar datos
data class UserData(
    val id: String,
    val name: String,
    val email: String,
    val token: String
)