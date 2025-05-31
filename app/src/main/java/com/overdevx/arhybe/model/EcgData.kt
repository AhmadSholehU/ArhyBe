package com.overdevx.arhybe.model

import com.squareup.moshi.Json

data class EcgData(
    @Json(name = "type")
    val type: String,
    @Json(name = "device_id")
    val deviceId: String,
    val timestamp: String,               // ⬅– Ubah Long → String
    @Json(name = "ecg_data")
    val ecgData: List<Float>
)