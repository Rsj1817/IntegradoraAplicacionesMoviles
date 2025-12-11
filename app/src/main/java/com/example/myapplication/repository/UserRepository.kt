package com.example.myapplication.repository

import com.example.myapplication.model.User

interface UserRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
}
