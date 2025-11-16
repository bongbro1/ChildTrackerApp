package com.example.childtrackerapp.child.helper

import android.content.Context

import android.media.MediaRecorder
import android.util.Log

import java.io.File



class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File? {
        return try {
            outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)

                prepare()
                start()
            }

            outputFile
        } catch (e: Exception) {
            Log.e("Recorder", "Start error: ${e.message}")
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile
        } catch (e: Exception) {
            Log.e("Recorder", "Stop error: ${e.message}")
            null
        }
    }
}


