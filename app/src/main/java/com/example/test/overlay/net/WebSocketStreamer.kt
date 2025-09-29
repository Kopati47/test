package com.example.test.overlay.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class WebSocketStreamer(
    private val url: String,
    connectTimeoutSec: Long = 5,
    readTimeoutSec: Long = 0
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .build()

    @Volatile private var ws: WebSocket? = null
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {})
    }

    fun sendChunk(samples: ShortArray, count: Int) {
        val socket = ws ?: return
        if (!started) return
        val bb = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) bb.putShort(samples[i])
        socket.send(ByteString.of(*bb.array()))
    }

    fun sendCancel() {
        ws?.send("CANCEL")
    }

    fun stop(code: Int = 1000, reason: String = "done") {
        started = false
        ws?.close(code, reason)
        ws = null
    }
}
