package com.example.myapplication.network

import android.os.Build
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private fun isEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val prod = Build.PRODUCT.lowercase()
        val model = Build.MODEL.lowercase()
        return fp.contains("generic") || fp.contains("unknown") ||
                prod.contains("sdk") || prod.contains("emulator") ||
                model.contains("emulator")
    }

    private val BASE_URL: String = if (isEmulator()) {
        "http://10.0.2.2:5000/"
    } else {
        "http://10.221.7.186:5000/"
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
