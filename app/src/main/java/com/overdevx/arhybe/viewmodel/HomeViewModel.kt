package com.overdevx.arhybe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overdevx.arhybe.model.EcgStatus
import com.overdevx.arhybe.model.PredictionResult
import com.overdevx.arhybe.network.EcgWebSocketListener
import com.overdevx.arhybe.repository.EcgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EcgRepository,
    private val okHttpClient: OkHttpClient,
    private val wsListener: EcgWebSocketListener
) : ViewModel() {

    companion object {
        private const val POLLING_INTERVAL_MS = 20_000L  // cek tiap 20 detik
        private const val DEVICE_ID = "ESP32_ECG_01"
        private const val WS_URL = "ws://192.168.7.85:8000/ws"
    }

    // --- 1. State untuk tracking on/off ---
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // --- 2. Data ECG (buffer untuk chart) ---
    private val _ecgBuffer = MutableStateFlow<List<Float>>(emptyList())
    val ecgBuffer: StateFlow<List<Float>> = _ecgBuffer.asStateFlow()

    // --- 3. Prediksi Terakhir ---
    private val _predictionResult = MutableStateFlow<PredictionResult?>(null)
    val predictionResult: StateFlow<PredictionResult?> = _predictionResult.asStateFlow()

    // --- 4. Status ECG (ready/duration/chunks/samples) ---
    private val _ecgStatus = MutableStateFlow<EcgStatus?>(null)
    val ecgStatus: StateFlow<EcgStatus?> = _ecgStatus.asStateFlow()

    private var statusPollingJob: Job? = null
    private var webSocket: WebSocket? = null

    init {
        // Berlangganan ke aliran ECG
        viewModelScope.launch {
            repository.getEcgStream().collect { batch ->
                Log.d("HomeViewModel", "Batch ECG diterima, size = ${batch.size}")
                val currentList = _ecgBuffer.value.toMutableList()
                currentList.addAll(batch)
                if (currentList.size > 7000) {
                    val overflow = currentList.size - 7000
                    repeat(overflow) { currentList.removeAt(0) }
                }
                _ecgBuffer.value = currentList.toList()
            }
        }

        // Berlangganan ke aliran Prediksi
        viewModelScope.launch {
            repository.getPredictionStream().collect { pred ->
                _predictionResult.value = pred
            }
        }
    }

    /**
     * Dipanggil saat tombol Start/Stop ditekan.
     */
    fun toggleTracking() {
        val newState = !_isTracking.value
        _isTracking.value = newState

        if (newState) {
            connectWebSocket()
            startPollingEcgStatus()
        } else {
            stopPollingEcgStatus()
            disconnectWebSocket()
            _ecgStatus.value = null
        }
    }

    /** Mulai polling status ECG setiap 20 detik */
    private fun startPollingEcgStatus() {
        statusPollingJob?.cancel()
        statusPollingJob = viewModelScope.launch {
            while (_isTracking.value) {
                try {
                    val status = repository.fetchEcgStatus(DEVICE_ID)
                    if (status != null) {
                        Log.d("HomeViewModel", "ECG Status: ready=${status.ready}, duration=${status.durationSec}")
                        _ecgStatus.value = status

                        // Jika server sudah siap, hentikan tracking otomatis
                        if (status.ready) {
                            _isTracking.value = false
                            stopPollingEcgStatus()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Polling status gagal: ${e.localizedMessage}")
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    private fun stopPollingEcgStatus() {
        statusPollingJob?.cancel()
        statusPollingJob = null
    }

    /** Inisiasi WebSocket */
    private fun connectWebSocket() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, wsListener)
        Log.d("HomeViewModel", "WebSocket dibuat → $webSocket")
    }

    /** Tutup WebSocket */
    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Closing by user")
        webSocket = null
        Log.d("HomeViewModel", "WebSocket ditutup")
    }
}
