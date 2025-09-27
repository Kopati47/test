package com.example.test.overlay

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import kotlin.math.max

class RecordingEngine(private val onLevel: (Float) -> Unit) {

    @Volatile private var recording = false
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null

    fun start() {
        if (recording) return
        val sr = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, 4096)

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, sr,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
        )
        audioRecord = rec
        rec.startRecording()
        recording = true

        thread = Thread {
            val buf = ShortArray(2048)
            while (recording && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    var peak = 0
                    for (i in 0 until n) peak = max(peak, abs(buf[i].toInt()))
                    onLevel((peak / 32767f).coerceIn(0f, 1f))
                }
            }
        }.apply { start() }
    }

    fun stop() {
        recording = false
        try { thread?.join(100) } catch (_: InterruptedException) {}
        thread = null
        audioRecord?.apply { try { stop() } catch (_: Exception) {}; release() }
        audioRecord = null
    }
}
