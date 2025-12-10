package com.example.myapplication.repository

import com.example.myapplication.model.User

/**
 * Contrato (interfaz) para el repositorio de usuario / autenticación.
 * Implementa este contrato en UserRepositoryImpl.
 */
interface UserRepository {
    /**
     * Intenta autenticar con username y password.
     * Devuelve Result.success(User) en caso de exito (incluyendo token),
     * o Result.failure(Throwable) si falla.
     */
    suspend fun login(username: String, password: String): Result<User>

    /** Cierra sesión localmente (borra token / prefs) */
    suspend fun logout()

    /** Devuelve el usuario actual si hay sesión activa (puede estar guardado en prefs) */
    suspend fun getCurrentUser(): User?

    /** True si hay token / sesión activa */
    suspend fun isLoggedIn(): Boolean
}
