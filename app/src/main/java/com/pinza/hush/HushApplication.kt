package com.hush.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * HushApplication
 * Clase base de la app — obligatoria para Hilt.
 * @HiltAndroidApp dispara la generación de código de Hilt en compile time.
 * Debe estar declarada en AndroidManifest.xml con android:name=".HushApplication"
 */
@HiltAndroidApp
class HushApplication : Application()
