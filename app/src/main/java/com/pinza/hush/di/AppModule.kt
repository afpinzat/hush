package com.pinza.hush.data.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pinza.hush.data.datasource.local.UserPreferencesDataSource
import com.pinza.hush.data.datasource.remote.FirebaseAuthDataSource
import com.pinza.hush.data.repository.AuthRepository
import com.pinza.hush.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // Firebase
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // FirebaseAuthDataSource
    @Provides
    @Singleton
    fun provideFirebaseAuthDataSource(
        auth: FirebaseAuth
    ): FirebaseAuthDataSource {
        return FirebaseAuthDataSource(auth)
    }

    // UserPreferencesDataSource
    @Provides
    @Singleton
    fun provideUserPreferencesDataSource(
        @ApplicationContext context: Context
    ): UserPreferencesDataSource {
        return UserPreferencesDataSource(context)
    }

    // ✅ ESTO ES LO QUE FALTA: Bind de AuthRepository
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuthDataSource,
        userPreferences: UserPreferencesDataSource
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, userPreferences)
    }
}