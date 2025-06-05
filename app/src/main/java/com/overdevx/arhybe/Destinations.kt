package com.overdevx.arhybe

import android.os.Bundle
import androidx.navigation.NavType
import com.overdevx.arhybe.model.ArrhythmiaPrediction
import com.overdevx.arhybe.model.BeatPrediction
import com.overdevx.arhybe.model.StressPrediction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class DiagnosisType {
    ARRHYTHMIA,
    STRESS
}
const val DIAGNOSIS_TYPE_ARRHYTHMIA = "arrhythmia"
const val DIAGNOSIS_TYPE_STRESS = "stress"
@Serializable
data class DiagnosisDetailDestination(
    val diagnosisTypeString: String, // Menggunakan String sebagai pembeda
    // Properti untuk data Aritmia (nullable)
    // Argumen untuk Arrythmia
    val arrhythmiaMainPrediction: String? = null,
    // Kita perlu cara untuk mengirim Map<String, Float> sebagai argumen.
    // Pilihan:
    // 1. Serialize Map ke String JSON dan deserialize di DetailScreen (paling umum)
    // 2. Kirim sebagai beberapa argumen (misal, afibProb, nProb, dll.) - tidak fleksibel
    val arrhythmiaProbabilitiesJson: String? = null, // String JSON dari Map<String, Float>

    // Argumen untuk Beat
    // Sama seperti probabilities, kita bisa serialize Map ke String JSON
    val beatDistributionJson: String? = null, // String JSON dari Map<String, Int>

    // Argumen untuk Stress
    val stressNumericPrediction: Int = 0, // Jika Anda masih membutuhkannya
    val stressLevelPrediction: String? = null,
    val stressProbabilitiesJson: String? = null, // String JSON dari Map<String, Float>

    // Mungkin Anda masih ingin prediksi utama untuk kemudahan
    val overallMainPrediction: String? = null, // Misal, "(N" atau "medium"
    val overallMainProbability: Float = 0f
)

@Serializable
data object BluetoothDestination

@Serializable
object HomeDestination

@Serializable
object SettingsDestination

// BottomNavItem.kt
sealed class BottomNavItem(
    val title: String,
    val route: String,
    val iconRes: Int,
    val selectedIconRes: Int
) {
    object Home : BottomNavItem(
        title = "Home",
        route = Destinations.HOME,
        iconRes = R.drawable.ic_home2,               // Vector drawable untuk state normal
        selectedIconRes = R.drawable.ic_home // Vector drawable untuk state selected
    )
    object Settings : BottomNavItem(
        title = "Settings",
        route = Destinations.SETTINGS,
        iconRes = R.drawable.ic_setting2,
        selectedIconRes = R.drawable.ic_setting
    )

    companion object {
        val items = listOf(Home, Settings)
    }
}

object Destinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

// Definisikan NavType kustom untuk DiagnosisType
val DiagnosisTypeNavType: NavType<DiagnosisType> = object : NavType<DiagnosisType>(
    isNullableAllowed = false // Sesuaikan jika null diizinkan, tapi biasanya tidak untuk enum
) {
    override val name: String
        get() = "com.overdevx.arhybe.DiagnosisType" // Nama unik untuk NavType ini

    override fun put(bundle: Bundle, key: String, value: DiagnosisType) {
        // Serialize enum ke String dan letakkan di Bundle
        bundle.putString(key, Json.encodeToString(value))
    }

    @Suppress("DEPRECATION") // Untuk NavType.get(Bundle, String)
    override fun get(bundle: Bundle, key: String): DiagnosisType? {
        // Ambil String dari Bundle dan deserialize kembali ke enum
        return bundle.getString(key)?.let { Json.decodeFromString<DiagnosisType>(it) }
    }

    override fun parseValue(value: String): DiagnosisType {
        // Parse String (dari route path) kembali ke enum
        return Json.decodeFromString<DiagnosisType>(value)
    }
}
