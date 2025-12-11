package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlaybackViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaybackViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
