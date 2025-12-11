package com.example.myapplication.network

import com.example.myapplication.model.User
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path

data class LoginRequest(
    val username: String,
    val password: String
)

data class RecordingItem(
    val fileName: String,
    val category: String = "",
    val title: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val transcript: String = ""
)

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): User

    @GET("recordings")
    suspend fun getRecordings(): List<RecordingItem>

    @GET("recordings/{fileName}")
    suspend fun getRecording(@Path("fileName") fileName: String): RecordingItem

    @POST("recordings")
    suspend fun createRecording(@Body item: RecordingItem): RecordingItem

    @PUT("recordings/{fileName}")
    suspend fun updateRecording(@Path("fileName") fileName: String, @Body item: RecordingItem): RecordingItem

    @DELETE("recordings/{fileName}")
    suspend fun deleteRecording(@Path("fileName") fileName: String)

    @retrofit2.http.Multipart
    @POST("recordings/{fileName}/transcribe")
    suspend fun transcribeRecording(
        @Path("fileName") fileName: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): RecordingItem
}
