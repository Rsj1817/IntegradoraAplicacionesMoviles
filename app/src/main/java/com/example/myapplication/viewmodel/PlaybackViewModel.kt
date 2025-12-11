package com.example.myapplication.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private var player: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs

    private val _positionMs = MutableStateFlow(0)
    val positionMs: StateFlow<Int> = _positionMs

    fun load(file: File) {
        release()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
        }
        _durationMs.value = player?.duration ?: 0
        _positionMs.value = 0
        _isPlaying.value = false
    }

    fun playPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            _isPlaying.value = false
        } else {
            p.start()
            _isPlaying.value = true
            startProgressLoop()
        }
    }

    fun seekTo(ms: Int) {
        player?.seekTo(ms.coerceAtLeast(0))
        _positionMs.value = player?.currentPosition ?: 0
    }

    fun restart() {
        player?.seekTo(0)
        _positionMs.value = 0
    }

    private fun startProgressLoop() {
        viewModelScope.launch {
            while (player?.isPlaying == true) {
                _positionMs.value = player?.currentPosition ?: 0
                delay(200)
            }
            _isPlaying.value = false
        }
    }

    fun release() {
        player?.release()
        player = null
        _isPlaying.value = false
        _positionMs.value = 0
        _durationMs.value = 0
    }
}
