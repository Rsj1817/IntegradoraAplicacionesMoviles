package com.example.myapplication.network

import com.example.myapplication.model.User
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val username: String,
    val password: String
)

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): User
}
