package com.example.test.overlay.net

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class WebSocketStreamer(
    private val url: String,               // ws://<host>/stream?sr=44100&ch=1&token=...
    connectTimeoutSec: Long = 5,
    readTimeoutSec: Long = 0               // 0 = без таймаута
) : WebSocketListener() {

    companion object {
        private const val TAG = "WebSocketStreamer"
        private const val CLOSE_TIMEOUT_MS = 3000L
    }

    private val main = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .build()

    @Volatile private var ws: WebSocket? = null
    @Volatile private var started = false
    @Volatile private var awaitingResponse = false

    /** Старт соединения */
    fun start() {
        if (started) return
        started = true
        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, this)
    }

    /** Отправка PCM-чанка (16-bit LE) */
    fun sendChunk(samples: ShortArray, count: Int) {
        val socket = ws ?: return
        if (!started) return
        val bb = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) bb.putShort(samples[i])
        val bytes = bb.array()
        socket.send(bytes.toByteString(0, bytes.size)) // без deprecated API
    }

    /** Отмена обработки (сервер вернёт {"type":"cancelled"}) */
    fun sendCancel() {
        val socket = ws ?: return
        socket.send("CANCEL")
        awaitingResponse = true
        scheduleForceClose("cancel-timeout")
    }

    /** Завершение: шлём END и ЖДЁМ JSON; закрываемся после приёма */
    fun stop(code: Int = 1000, reason: String = "done") {
        val socket = ws ?: return
        if (started) {
            socket.send("END")
            awaitingResponse = true
            scheduleForceClose("end-timeout")
        } else {
            socket.close(code, reason)
            ws = null
        }
        started = false
    }

    private fun scheduleForceClose(reason: String) {
        main.postDelayed({
            if (awaitingResponse) {
                Log.w(TAG, "forceClose: $reason")
                ws?.close(1000, reason)
                ws = null
                awaitingResponse = false
            }
        }, CLOSE_TIMEOUT_MS)
    }

    // ---------------- WebSocketListener ----------------

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "onOpen ${response.code}")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // красиво печатаем JSON в логи
        val pretty = prettyJsonOrRaw(text)
        Log.d(TAG, "JSON:\n$pretty")

        // получили ответ — можно закрывать
        if (awaitingResponse) {
            awaitingResponse = false
            webSocket.close(1000, "done")
            ws = null
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d(TAG, "<- ${bytes.size} bytes (ignored)")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "onClosing $code $reason")
        ws = null
        awaitingResponse = false
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "onClosed $code $reason")
        ws = null
        awaitingResponse = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "onFailure", t)
        ws = null
        awaitingResponse = false
    }

    // ---------------- utils ----------------

    private fun prettyJsonOrRaw(text: String): String {
        val s = text.trim()
        return try {
            when {
                s.startsWith("{") -> JSONObject(s).toString(2)
                s.startsWith("[") -> JSONArray(s).toString(2)
                else -> text
            }
        } catch (_: Exception) {
            text
        }
    }
}
