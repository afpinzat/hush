package com.pinza.hush

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinza.hush.data.local.dao.UserDao
import com.pinza.hush.data.local.database.MusicDatabase
import com.pinza.hush.data.local.model.User
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Pruebas de integración para la base de datos Room.
 * Estas pruebas se ejecutan en un dispositivo físico o emulador.
 */
@RunWith(AndroidJUnit4::class)
class UserDatabaseTest {

    private lateinit var userDao: UserDao
    private lateinit var db: MusicDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Creamos la base de datos in-memory. Se destruye al cerrar el proceso.
        db = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .allowMainThreadQueries() // Solo para pruebas, evita manejar hilos complejos
            .build()
        userDao = db.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndReadUser() = runBlocking {
        // Arrange
        val user = User(email = "test@hush.com", password = "password123")
        
        // Act
        userDao.insertUser(user)
        val result = userDao.getUserByEmail("test@hush.com")

        // Assert
        assertNotNull("El usuario debería existir en la base de datos", result)
        assertEquals("El email debería coincidir", user.email, result?.email)
        assertEquals("La contraseña debería coincidir", user.password, result?.password)
    }

    @Test
    @Throws(Exception::class)
    fun simulateLoginFlow() = runBlocking {
        // Arrange
        val email = "login@hush.com"
        val password = "correct_password"
        val user = User(email = email, password = password)
        userDao.insertUser(user)

        // Act - Caso Exitoso
        val loggedInUser = userDao.login(email, password)
        
        // Act - Caso Fallido (Contraseña incorrecta)
        val wrongPasswordUser = userDao.login(email, "wrong_pass")
        
        // Act - Caso Fallido (Correo inexistente)
        val nonExistentUser = userDao.login("non@existent.com", password)

        // Assert
        assertNotNull("El login debería ser exitoso con credenciales correctas", loggedInUser)
        assertEquals("Debería retornar el usuario correcto", email, loggedInUser?.email)
        
        assertNull("El login debería fallar con contraseña incorrecta", wrongPasswordUser)
        assertNull("El login debería fallar con correo inexistente", nonExistentUser)
    }
}
