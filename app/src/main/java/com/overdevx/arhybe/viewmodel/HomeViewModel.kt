package com.overdevx.arhybe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overdevx.arhybe.model.PredictionResult
import com.overdevx.arhybe.repository.EcgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EcgRepository,
    private val webSocket: WebSocket
) : ViewModel() {
    init {
        Log.d("HomeViewModel", "HomeViewModel dibuat → WebSocket instance = $webSocket")
        // Anda bisa mengirim pesan inisialisasi (opsional):
        // webSocket.send("{\"type\":\"init\",\"device_id\":\"ESP32_ECG_01\"}")
    }
    // --- 1. State untuk tracking on/off ---
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // --- 2. Timer (format detik tersisa) ---
    private val _remainingTime = MutableStateFlow(0L) // dalam detik
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private var timerJob: Job? = null

    // --- 3. Data ECG (list ter-update untuk chart) ---
    // Asumsikan kita simpan buffer sliding window (misal 7000 data terakhir)
    private val _ecgBuffer = MutableStateFlow<List<Float>>(emptyList())
    val ecgBuffer: StateFlow<List<Float>> = _ecgBuffer.asStateFlow()

    // --- 4. Prediksi Terakhir ---
    private val _predictionResult = MutableStateFlow<PredictionResult?>(null)
    val predictionResult: StateFlow<PredictionResult?> = _predictionResult.asStateFlow()

    init {
        // Berlangganan ke aliran ECG
        viewModelScope.launch {
            repository.getEcgStream().collect { batch ->
                // Tambahkan batch ke buffer; jaga maxSize contohnya 7000
                val currentList = _ecgBuffer.value.toMutableList()
                currentList.addAll(batch)
                if (currentList.size > 7000) {
                    // hapus data tertua
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
     * Fungsi dipanggil ketika tombol Start/Stop ditekan.
     */
    fun toggleTracking() {
        val newState = !(_isTracking.value)
        _isTracking.value = newState

        if (newState) {
            startTimer(5 * 60L) // 5 menit = 300 detik
        } else {
            stopTimer()
        }
    }

    private fun startTimer(totalSeconds: Long) {
        stopTimer() // pastikan job sebelumnya dibatalkan

        _remainingTime.value = totalSeconds

        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0 && _isTracking.value) {
                delay(1000L)
                _remainingTime.value = _remainingTime.value - 1
            }
            // Jika timer mencapai 0, otomatis hentikan tracking
            if (_remainingTime.value <= 0) {
                _isTracking.value = false
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingTime.value = 0L
    }
}