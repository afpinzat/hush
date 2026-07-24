# Manual Técnico — Hush v1.0

## 1. Descripción del sistema
Hush es un reproductor de música MP3 offline diseñado para Android, enfocado en la simplicidad, el rendimiento y una experiencia visual inmersiva. Resuelve el problema de las interfaces sobrecargadas y la publicidad invasiva en reproductores locales.

- **Usuario objetivo:** Usuarios que almacenan música localmente y buscan una app minimalista con soporte nativo para letras (LRC).
- **Alcance del MVP:** Escaneo de archivos, reproducción en segundo plano, gestión de favoritos y playlists, visualización de letras sincronizadas y efectos visuales dinámicos.

## 2. Arquitectura de la aplicación
La aplicación sigue una arquitectura de capas basada en los principios de **Clean Architecture** y el patrón de diseño **MVVM**:

- **Capa de UI (Presentación):** Implementada con Fragments y Activities. Utiliza *View Binding* para el acceso a vistas y *StateFlow* para observar el estado del ViewModel.
- **Capa de Dominio (Lógica):** Contiene los *UseCases* que encapsulan las reglas de negocio, como `PlayQueueUseCase` o `ScanMusicUseCase`.
- **Capa de Datos:** Implementa los repositorios que median entre el motor de audio (`PlayerManager`), la base de datos local (`Room`) y los servicios en la nube (`Firebase`).

**Patrón de diseño:** MVVM (Model-View-ViewModel).
**Inyección de dependencias:** Dagger Hilt.

## 3. Modelo de datos
El sistema utiliza una base de datos **Room** (v10) con las siguientes entidades:

- **`Song`**: Metadatos del archivo de audio (ID, título, artista, duración, path, album_art).
- **`Playlist`**: Cabecera de las listas de reproducción.
- **`PlaylistSongCrossRef`**: Relación Many-to-Many entre canciones y playlists (tabla `playlist_song_join`).
- **`QueueItem`**: Persistencia de la cola de reproducción actual (tabla `queue`).
- **`PlayerState`**: Estado actual del reproductor para restaurar sesiones.
- **`SongLyrics`**: Letras sincronizadas en formato LRC y traducciones.
- **`User`**: Caché local de la información del usuario autenticado.

**Relaciones:**
- **N:M**: Canciones y Playlists.
- **1:1**: Canción y sus letras (LRC).

## 4. Tecnologías y librerías
- **Framework:** Android SDK (Kotlin 2.2.10).
- **Base de datos:** Room Persistence Library 2.8.4.
- **Autenticación:** Firebase Auth (BOM 33.15.0).
- **Motor de Audio:** Media3 ExoPlayer 1.3.1.
- **Carga de Imágenes:** Coil 2.6.0 (con soporte para Hardware Bitmaps).
- **UI:** Material Design 3, Palette API, Navigation Component.

## 5. Instrucciones para compilar
### Requisitos
- **Android Studio:** Jellyfish o superior.
- **JDK:** 17.
- **SDK Mínimo:** 24 (Android 7.0).
- **SDK Target:** 35.

### Pasos
1. Clonar el repositorio: `git clone https://github.com/afpinzat/hush.git`
2. Abrir el proyecto en Android Studio.
3. Incluir el archivo `google-services.json` en la carpeta `app/`.
4. Sincronizar Gradle.
5. Ejecutar en un dispositivo o emulador (API 24+).

## 6. Estructura del repositorio
- `/app/src/main/java/com/pinza/hush/data/`: DAOs, modelos de base de datos y repositorios.
- `/app/src/main/java/com/pinza/hush/domain/`: Casos de uso e interfaces de repositorio.
- `/app/src/main/java/com/pinza/hush/ui/`: Activities, Fragments, ViewModels y Adapters organizados por flujo.
- `/app/src/main/res/`: Recursos visuales, layouts y definiciones de navegación.

## 7. Historial de versiones
- **v1.0 (20/07/2026):** MVP Completo.
    - Autenticación híbrida con Firebase.
    - Reproductor con partículas dinámicas.
    - Gestión completa de Playlists y Favoritos.
    - Editor de letras LRC integrado.
    - Optimización de RAM y rendimiento de scroll.
