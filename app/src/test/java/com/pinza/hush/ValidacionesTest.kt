package com.pinza.hush
import com.pinza.hush.utils.Validator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Clase de pruebas unitarias para validar la lógica de autenticación y negocio.
 * Sigue el patrón AAA (Arrange, Act, Assert).
 */
class ValidacionesTest {

    // --- PRUEBA 1: VALIDACIÓN DE CORREO ---

    @Test
    fun validarCorreo_correoValido_retornaTrue() {
        // 1. Arrange (Preparar)
        val correo = "usuario@hush.com"

        // 2. Act (Actuar)
        val resultado = Validator.isValidEmail(correo)

        // 3. Assert (Verificar)
        assertTrue("El correo debería ser válido", resultado)
    }

    @Test
    fun validarCorreo_correoSinArroba_retornaFalse() {
        // Arrange
        val correo = "usuariohush.com"

        // Act
        val resultado = Validator.isValidEmail(correo)

        // Assert
        assertFalse("El correo sin @ debería ser inválido", resultado)
    }

    // --- PRUEBA 2: VALIDACIÓN DE CONTRASEÑA ---

    @Test
    fun validarPassword_longitudSuficiente_retornaTrue() {
        // Arrange
        val password = "password123"

        // Act
        val resultado = Validator.isValidPassword(password)

        // Assert
        assertTrue("La contraseña de más de 6 caracteres debería ser válida", resultado)
    }

    @Test
    fun validarPassword_longitudCorta_retornaFalse() {
        // Arrange
        val password = "123"

        // Act
        val resultado = Validator.isValidPassword(password)

        // Assert
        assertFalse("La contraseña de menos de 6 caracteres debería ser rechazada", resultado)
    }

    // --- PRUEBA 3: LÓGICA DE NEGOCIO (CANCIÓN) ---

    @Test
    fun validarCancion_datosCompletos_retornaTrue() {
        // Arrange
        val titulo = "The Less I Know The Better"
        val artista = "Tame Impala"

        // Act
        val resultado = Validator.isSongValid(titulo, artista)

        // Assert
        assertTrue("Una canción con título y artista debería ser válida", resultado)
    }

    @Test
    fun validarCancion_tituloVacio_retornaFalse() {
        // Arrange
        val titulo = ""
        val artista = "Tame Impala"

        // Act
        val resultado = Validator.isSongValid(titulo, artista)

        // Assert
        assertFalse("Una canción sin título no debería ser válida", resultado)
    }
}
