package com.overdevx.arhybe.viewmodel

import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.overdevx.arhybe.repository.DeviceRepository


//-tambah riwayat (data tersimpan)
//-tampilkan angka" yang bisa dipahami pengguna
//-print hasil ke pdf
// --- BARU: Enum untuk merepresentasikan state pelacakan yang lebih detail ---
enum class TrackingPhase {
    IDLE,       // Tidak melakukan apa-apa
    TRACKING,   // Sedang mengumpulkan data
    PROCESSING  // Mendekati akhir pengumpulan data
}
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EcgRepository,
    private val okHttpClient: OkHttpClient,
    private val wsListener: EcgWebSocketListener,
    private val deviceRepository: DeviceRepository,

) : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    companion object {
        private const val POLLING_INTERVAL_MS = 20_000L
        private const val TARGET_DURATION_SEC = 300
        private const val DEVICE_ID = "ESP32_ECG_01"
        private const val WS_URL = "ws://192.168.7.85:8000/ws"
    }

    // --- MODIFIKASI: Mengganti _isTracking dengan state yang lebih deskriptif ---
    private val _trackingPhase = MutableStateFlow(TrackingPhase.IDLE)
    val trackingPhase: StateFlow<TrackingPhase> = _trackingPhase.asStateFlow()

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
                _trackingPhase.value = TrackingPhase.IDLE
            }
        }
    }

    /**
     * Dipanggil saat tombol Start/Stop ditekan.
     */
    fun toggleTracking() {
        val isCurrentlyIdle = _trackingPhase.value == TrackingPhase.IDLE

        if (isCurrentlyIdle) {
            // Jika sedang idle, mulai tracking
            _trackingPhase.value = TrackingPhase.TRACKING
            _predictionResult.value = null // Hapus prediksi lama
            _ecgBuffer.value = emptyList() // Kosongkan buffer chart
            connectWebSocket()
            startPollingEcgStatus()
        } else {
            // Jika sedang tracking atau processing, hentikan
            _trackingPhase.value = TrackingPhase.IDLE
            stopPollingEcgStatus()
            disconnectWebSocket()
            _ecgStatus.value = null
        }
    }

    /** Mulai polling status ECG setiap 20 detik */
    private fun startPollingEcgStatus() {
        statusPollingJob?.cancel()
        statusPollingJob = viewModelScope.launch {
            while (_trackingPhase.value != TrackingPhase.IDLE) {
                try {
                    val status = repository.fetchEcgStatus(DEVICE_ID)
                    if (status != null) {
                        Log.d("HomeViewModel", "ECG Status: ready=${status.ready}, duration=${status.durationSec}")
                        _ecgStatus.value = status

                        if (status.durationSec >= (TARGET_DURATION_SEC - POLLING_INTERVAL_MS / 1000)) {
                            if (_trackingPhase.value != TrackingPhase.PROCESSING) {
                                Log.d("HomeViewModel", "Durasi mendekati 5 menit, masuk fase PROCESSING.")
                                _trackingPhase.value = TrackingPhase.PROCESSING
                            }
                        }
                        // Jika server sudah siap, hentikan tracking otomatis
                        if (status.ready) {
                            _trackingPhase.value = TrackingPhase.IDLE
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

    fun saveCurrentSessionToHistory() {
        // 1. Pastikan ada pengguna yang sedang login
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("HISTORY", "Tidak ada pengguna yang login, data tidak disimpan.")
            return
        }

        // 2. Ambil hasil prediksi terakhir dari StateFlow Anda
        val finalArrhythmiaResult = predictionResult.value?.arrhythmia?.prediction ?: "N/A"
        val finalArrhythmiaProbability = predictionResult.value?.arrhythmia?.probabilities ?: 0.0f
        val finalStressResult = predictionResult.value?.stress?.prediction ?: "N/A"
        val finalStressProbability = predictionResult.value?.stress?.probabilities ?: 0.0f

        // 3. Buat objek data (Map) untuk disimpan
        val recordingData = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "arrhythmiaPrediction" to finalArrhythmiaResult,
            "arrhythmiaProbability" to finalArrhythmiaProbability,
            "stressPrediction" to finalStressResult,
            "stressProbability" to finalStressProbability
        )

        // 4. Simpan ke Firestore
        firestore.collection("histories").document(userId)
            .collection("recordings")
            .add(recordingData)
            .addOnSuccessListener {
                Log.d("HISTORY", "Riwayat berhasil disimpan dengan ID: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.w("HISTORY", "Gagal menyimpan riwayat", e)
            }
    }

    fun onDeviceProvisioned(token: String?, deviceId: String) {
        // 1. Pastikan token tidak null
        if (token == null) {
            Log.e("HomeViewModel", "Cannot claim device, Firebase ID token is null.")
            // Handle error, mungkin tampilkan pesan untuk login ulang
            return
        }

        viewModelScope.launch {
            // 2. Panggil repository untuk mengklaim perangkat dengan token yang diberikan
            Log.d("HomeViewModel", "Attempting to claim device $deviceId with token.")
            deviceRepository.claimDevice(token, deviceId)
        }
    }
}
