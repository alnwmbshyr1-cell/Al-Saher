package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): Result<Unit> {
        return try {
            release()
            val file = File(context.cacheDir, "alsaher_rec_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(16_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            Result.success(Unit)
        } catch (e: Exception) {
            release()
            Result.failure(e)
        }
    }

    fun stopAndReadBytes(): Result<ByteArray> {
        val file = outputFile
        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            if (file == null || !file.exists() || file.length() <= 0L) {
                error("لم يتم تسجيل أي مقطع صوتي.")
            }
            val bytes = file.readBytes()
            file.delete()
            outputFile = null
            Result.success(bytes)
        } catch (e: Exception) {
            release()
            Result.failure(e)
        }
    }

    fun release() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { outputFile?.delete() }
        outputFile = null
    }
}
