package com.overdevx.arhybe.repository


import android.util.Log
import com.overdevx.arhybe.network.EcgService
import javax.inject.Inject

data class ClaimRequest(val device_id: String)

class DeviceRepository @Inject constructor(
    private val apiService: EcgService
) {
    suspend fun claimDevice(token: String, deviceId: String): Boolean {
        return try {
            val response = apiService.claimDevice("Bearer $token", ClaimRequest(device_id = deviceId))
            if (response.isSuccessful) {
                Log.d("DeviceRepository", "Device successfully claimed: ${response.body()}")
                true
            } else {
                Log.e("DeviceRepository", "Failed to claim device: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "Exception when claiming device", e)
            false
        }
    }
}

