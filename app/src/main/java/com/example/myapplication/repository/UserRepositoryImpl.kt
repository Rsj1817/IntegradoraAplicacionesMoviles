package com.example.myapplication.repository

import android.content.Context
import com.example.myapplication.model.User
import com.example.myapplication.network.ApiService
import com.example.myapplication.network.LoginRequest

class UserRepositoryImpl(
    private val api: ApiService,
    private val context: Context
) : UserRepository {

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            val user = api.login(LoginRequest(username, password))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        // Aquí luego puedes borrar SharedPreferences / Token
    }
}
