package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.util.MediaRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val recorderHelper = MediaRecorderHelper()

    var isRecording = false
        private set

    var timerSeconds = 0
        private set

    fun startRecording() {
        val file = File(
            getApplication<Application>().cacheDir,
            "audio_${System.currentTimeMillis()}.mp4"
        )

        recorderHelper.startRecording(file)
        isRecording = true

        viewModelScope.launch {
            timerSeconds = 0
            while (isRecording) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    fun stopRecording(): File? {
        isRecording = false
        return recorderHelper.stopRecording()
    }
}
