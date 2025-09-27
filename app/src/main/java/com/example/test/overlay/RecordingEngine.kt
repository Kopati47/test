package com.example.test.overlay

import android.content.Context
import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlin.math.abs
import kotlin.math.max

/**
 * Streaming-аудиозапись (PCM 16-bit, mono, 44.1kHz).
 * Безопасно обрабатывает отсутствие разрешения и SecurityException.
 *
 * @param onLevel  визуальный уровень (0..1)
 * @param onChunk  поток PCM-данных (samples, count)
 * @param onError  ошибка старта/чтения (напр. нет разрешения)
 * @param appContext (опц.) если передать — сделаем явную проверку разрешения
 */
class RecordingEngine(
    private val onLevel: (Float) -> Unit,
    private val onChunk: (ShortArray, Int) -> Unit,
    private val onError: (Throwable) -> Unit = {},
    private val appContext: Context? = null
) {
    @Volatile private var recording = false
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null

    fun start() {
        if (recording) return

        // (1) Явная проверка разрешения, если есть Context
        if (appContext != null) {
            val granted = ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                onError(SecurityException("RECORD_AUDIO permission not granted"))
                return
            }
        }

        val sr = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, 4096)

        // (2) Создание AudioRecord с защитой
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sr,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )
        } catch (t: Throwable) {
            onError(t)
            return
        }

        // (3) Старт с защитой
        try {
            rec.startRecording()
        } catch (t: Throwable) {
            try { rec.release() } catch (_: Exception) {}
            onError(t)
            return
        }

        audioRecord = rec
        recording = true

        thread = Thread {
            val buf = ShortArray(2048)
            try {
                while (recording && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n > 0) {
                        var peak = 0
                        for (i in 0 until n) {
                            val v = abs(buf[i].toInt())
                            if (v > peak) peak = v
                        }
                        val level = (peak / 32767f).coerceIn(0f, 1f)
                        onLevel(level)
                        onChunk(buf, n)
                    }
                }
            } catch (t: Throwable) {
                onError(t)
            }
        }.apply { start() }
    }

    fun stop() {
        recording = false
        try { thread?.join(100) } catch (_: InterruptedException) {}
        thread = null
        audioRecord?.apply {
            try { stop() } catch (_: Exception) {}
            try { release() } catch (_: Exception) {}
        }
        audioRecord = null
    }
}
