package com.example.myapplication.util

import android.media.MediaRecorder
import java.io.File

class MediaRecorderHelper {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(file: File) {
        outputFile = file

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile
    }

    fun pauseRecording() {
        recorder?.pause()
    }

    fun resumeRecording() {
        recorder?.resume()
    }
}
