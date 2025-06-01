package com.overdevx.arhybe.repository

import com.overdevx.arhybe.model.EcgStatus
import com.overdevx.arhybe.model.PredictionResult
import com.overdevx.arhybe.network.EcgService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcgRepository @Inject constructor(
    private val ecgChannel: Channel<List<Float>>,
    private val predictionChannel: Channel<PredictionResult>,
    private val ecgService: EcgService
) {
    /**
     * Flow untuk data batch ECG (List<Float>) real‐time.
     * Setiap kali batch diterima lewat WebSocket, akan emit List<Float> baru.
     */
    fun getEcgStream(): Flow<List<Float>> = ecgChannel.receiveAsFlow()

    /**
     * Flow untuk hasil prediksi real‐time.
     */
    fun getPredictionStream(): Flow<PredictionResult> = predictionChannel.receiveAsFlow()

    /**
     * (Opsional) Jika ingin memanggil endpoint REST untuk prediksi:
     */
//    private val ecgService: EcgService
//    suspend fun fetchLatestPrediction(deviceId: String): PredictionResult? {
//        val response = ecgService.getPredictions(deviceId)
//        return if (response.isSuccessful) response.body() else null
//    }

    /** Panggil endpoint status ECG */
    suspend fun fetchEcgStatus(deviceId: String): EcgStatus? {
        return try {
            val response = ecgService.getEcgStatus(deviceId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

}