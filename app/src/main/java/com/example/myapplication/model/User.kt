package com.example.myapplication.model

/**
 * Modelo simple de usuario.
 */
data class User(
    val id: Long = 0L,
    val username: String = "",
    val email: String? = null,
    val token: String? = null
)
