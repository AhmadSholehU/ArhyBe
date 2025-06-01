package com.overdevx.arhybe.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EcgStatus(
    @Json(name = "device_id")
    val deviceId: String,
    val ready: Boolean,
    @Json(name = "duration_sec")
    val durationSec: Double,
    val chunks: Int,
    val samples: Int
)