package com.overdevx.arhybe.network

import com.overdevx.arhybe.model.PredictionResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface EcgService {
    /**
     * Endpoint untuk mengambil prediksi terakhir (jika diperlukan).
     * GET /api/predictions/{device_id}
     */
    @GET("api/predictions/{device_id}")
    suspend fun getPredictions(
        @Path("device_id") deviceId: String
    ): Response<PredictionResult>
}