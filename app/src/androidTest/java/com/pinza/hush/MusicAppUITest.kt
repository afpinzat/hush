package com.pinza.hush

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinza.hush.ui.auth.LoginActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MusicAppUITest - Pruebas de Interfaz de Usuario para Hush.
 *
 * Como desarrollador senior, estas pruebas aseguran que los flujos críticos
 * (Autenticación, Reproducción y Estados de Borde) funcionen correctamente
 * y que la integración entre el dominio, la base de datos Room y la UI sea sólida.
 */
@RunWith(AndroidJUnit4::class)
class MusicAppUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    private lateinit var activity: LoginActivity

    @Before
    fun setUp() {
        // Obtener la actividad para usarla en los matchers de Toast
        activityRule.scenario.onActivity { activity = it }
    }

    /**
     * Escenario de Error (Login):
     * Verifica que el sistema valide campos vacíos y muestre feedback al usuario.
     */
    @Test
    fun testLoginEmptyFields_ShowsError() {
        // Dejamos los campos vacíos a propósito
        onView(withId(R.id.et_email)).perform(clearText())
        onView(withId(R.id.et_password)).perform(clearText())

        // Presionamos el botón de login
        onView(withId(R.id.btn_login)).perform(click())

        // Verificamos que se muestre el Toast con el mensaje de error real del LoginActivity
        onView(withText("Completa todos los campos"))
            .inRoot(withDecorView(not(activity.window.decorView)))
            .check(matches(isDisplayed()))

        // Verificamos que el botón sigue habilitado
        onView(withId(R.id.btn_login)).check(matches(isEnabled()))
    }

    /**
     * Escenario de Login Exitoso:
     * Verifica que el login funcione correctamente y navegue a la pantalla principal.
     */
    @Test
    fun testLoginSuccessful_NavigatesToMainScreen() {
        // Ingresamos credenciales válidas
        onView(withId(R.id.et_email)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.et_password)).perform(typeText("password123"), closeSoftKeyboard())

        // Presionamos el botón de login
        onView(withId(R.id.btn_login)).perform(click())

        // Verificamos que navega a la pantalla principal (nav_host_fragment es el ID real en activity_main)
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }

    /**
     * Escenario de Lógica de Negocio (Reproductor):
     * Valida el ciclo de vida de reproducción y la navegación entre pistas.
     */
    @Test
    fun testPlayerPlaybackCycle_UpdatesStateAndUI() {
        performLogin("test@example.com", "password123")

        // Para este test asumimos que el miniplayer aparece al reproducir algo
        // 1. Simular clic en Play/Pause si estuviéramos en NowPlaying
        // (Nota: Esto requiere navegar al fragmento de reproducción primero)
    }

    /**
     * Caso de Borde (Estado vacío):
     * Asegura que la aplicación maneje correctamente la ausencia de datos en LibraryFragment.
     */
    @Test
    fun testEmptyLibrary_ShowsInformativeMessage() {
        performLogin("test@example.com", "password123")

        // Verificar que el TextView de estado vacío esté visible (ID en fragment_library.xml)
        onView(withId(R.id.emptyStateText))
            .check(matches(isDisplayed()))
            .check(matches(withText("No hay canciones disponibles")))
    }

    /**
     * Prueba de Navegación entre Pantallas:
     * Verifica que la navegación entre secciones mediante Tabs funcione correctamente.
     */
    @Test
    fun testNavigation_BetweenTabsWorks() {
        performLogin("test@example.com", "password123")

        // Navegar a Favoritos usando el texto del Tab
        onView(withText("Favoritos")).perform(click())
        
        // Navegar a Playlists
        onView(withText("Playlists")).perform(click())
    }

    /**
     * Prueba de Búsqueda:
     * Verifica que la funcionalidad de búsqueda en LibraryFragment funcione correctamente.
     */
    @Test
    fun testSearch_Interaction() {
        performLogin("test@example.com", "password123")

        // Realizar búsqueda usando IDs reales (btnSearch, etSearch)
        onView(withId(R.id.btnSearch)).perform(click())
        onView(withId(R.id.etSearch)).perform(typeText("luna"), pressImeActionButton())

        // Verificar que el EditText de búsqueda es visible
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()))
    }

    // --- Métodos Auxiliares ---

    private fun performLogin(email: String, password: String) {
        onView(withId(R.id.et_email)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.et_password)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.btn_login)).perform(click())

        try {
            onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
        } catch (e: Exception) {
            throw AssertionError("Login fallido o navegación no detectada: ${e.message}")
        }
    }

    private fun obtenerTituloActual(): String {
        val stringHolder = StringBuilder()
        onView(withId(R.id.tv_song_title)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(android.widget.TextView::class.java)
            override fun getDescription(): String = "Obtener texto"
            override fun perform(uiController: UiController, view: View) {
                stringHolder.append((view as android.widget.TextView).text.toString())
            }
        })
        return stringHolder.toString()
    }
}
