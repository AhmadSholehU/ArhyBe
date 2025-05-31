package com.overdevx.arhybe.network

import android.util.Log
import com.overdevx.arhybe.model.EcgData
import com.overdevx.arhybe.model.PredictionEnvelope
import com.overdevx.arhybe.model.PredictionResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import java.lang.reflect.Type

class EcgWebSocketListener(
    private val ecgChannel: Channel<List<Float>>,
    private val predictionChannel: Channel<PredictionResult>,
    private val moshi: Moshi
) : WebSocketListener() {

    private val TAG = "EcgWebSocketListener"
    private val ecgEnvelopeType = Types.newParameterizedType(
        Map::class.java, String::class.java, Any::class.java
    )

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WebSocket terbuka")
        // Jika server membutuhkan inisialisasi tertentu, kirim di sini.
        // Namun, di backend FastAPI tidak ada pesan inisialisasi khusus.
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "onMessage: pesan diterima → $text")
        // Pesan JSON bisa berisi "type": "ecg" atau "type": "prediction"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Parsing envelope umum (type, device_id, dll)
                val envelopeAdapter = moshi.adapter(Map::class.java)
                val genericMap = envelopeAdapter.fromJson(text) as? Map<*, *> ?: return@launch
                val type = genericMap["type"] as? String ?: return@launch

                if (type == "ecg") {
                    // format: { "type":"ecg", "device_id":"...", "timestamp":"...", "ecg_data":[... ] }
                    // Kita bisa parse langsung ke EcgData menggunakan Moshi
                    val ecgAdapter = moshi.adapter(EcgData::class.java)
                    val ecgData = ecgAdapter.fromJson(text)
                    ecgData?.ecgData?.let { batch ->
                        ecgChannel.send(batch)
                    }
                } else if (type == "prediction") {
                    // format: { "type":"prediction", "device_id":"...", "data": { ... } }
                    val predAdapter = moshi.adapter(PredictionEnvelope::class.java)
                    val predEnv = predAdapter.fromJson(text)
                    predEnv?.data?.let { pr ->
                        predictionChannel.send(pr)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing WS message: ${e.localizedMessage}")
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closing: $code / $reason")
        webSocket.close(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "WebSocket gagal: ${t.localizedMessage}")
        // Bisa kirim error lewat channel jika ingin
    }
}