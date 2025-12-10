package com.example.myapplication.repository

import com.example.myapplication.model.User

/**
 * Contrato (interfaz) para el repositorio de usuario / autenticación.
 */
interface UserRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    suspend fun isLoggedIn(): Boolean
}
