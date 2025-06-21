package com.overdevx.arhybe.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClaimResponse(
    @Json(name = "status")
    val status: String,

    @Json(name = "message")
    val message: String
)