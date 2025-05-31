package com.overdevx.arhybe.model

import com.squareup.moshi.Json

/**
 * Sesuaikan struktur ini dengan payload JSON yang dikirim server lewat WebSocket:
 * {
 *   "type": "prediction",
 *   "device_id": "ESP32_ECG_01",
 *   "data": {
 *      "timestamp": "...",
 *      "arrhythmia": { "prediction": "...", "probabilities": {...} },
 *      "beat": { "distribution": {...} },
 *      "stress": { "level": "...", "probabilities": {...} }
 *   }
 * }
 */
data class PredictionEnvelope(
    val type: String,
    @Json(name = "device_id")
    val deviceId: String,
    val data: PredictionResult
)

data class PredictionResult(
    val timestamp: String, // ISO format
    val arrhythmia: ArrhythmiaPrediction?,
    val beat: BeatPrediction?,
    val stress: StressPrediction?
)

data class ArrhythmiaPrediction(
    val prediction: String,
    val probabilities: Map<String, Float>
)

data class BeatPrediction(
    val distribution: Map<String, Int>
)

data class StressPrediction(
    val prediction: Int,
    val level: String,
    val probabilities: Map<String, Float>
)
