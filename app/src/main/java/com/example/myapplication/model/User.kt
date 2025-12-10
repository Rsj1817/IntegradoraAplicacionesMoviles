package com.example.myapplication.model

/**
 * Modelo simple de usuario.
 * Podemos extenderlo con más campos que tu backend devuelva (avatarUrl, roles, etc.)
 */
data class User(
    val id: Long = 0L,
    val username: String = "",
    val email: String? = null,
    val token: String? = null
)
