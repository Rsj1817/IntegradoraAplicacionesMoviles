package com.example.myapplication.model

data class User(
    val id: Long,
    val username: String,
    val email: String?,
    val token: String
)
