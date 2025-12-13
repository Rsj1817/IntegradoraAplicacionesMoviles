package com.example.myapplication.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.util.MediaRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val recorderHelper = MediaRecorderHelper()
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private fun getRecordingsDirectory(): File {
        val app = getApplication<Application>()
        val extMusic = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val snapDir = File((extMusic ?: app.cacheDir), "SnapRec")
        if (!snapDir.exists()) {
            snapDir.mkdirs()
        }
        return if (snapDir.exists() && snapDir.canWrite()) {
            snapDir
        } else {
            app.cacheDir
        }
    }

    fun startRecording() {
        val recordingsDir = getRecordingsDirectory()
        val file = File(
            recordingsDir,
            "audio_${System.currentTimeMillis()}.mp4"
        )

        recorderHelper.startRecording(file)
        _isRecording.value = true
        _isPaused.value = false
        _timerSeconds.value = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isRecording.value) break
                if (!_isPaused.value) {
                    _timerSeconds.value = _timerSeconds.value + 1
                }
            }
        }
    }

    fun stopRecording(): File? {
        _isRecording.value = false
        _isPaused.value = false
        timerJob?.cancel()
        timerJob = null
        val file = recorderHelper.stopRecording()
        _timerSeconds.value = 0
        return file
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            recorderHelper.pauseRecording()
            _isPaused.value = true
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            recorderHelper.resumeRecording()
            _isPaused.value = false
        }
    }
}
