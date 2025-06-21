package com.overdevx.arhybe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.overdevx.arhybe.navigation.MainNavHost
import com.overdevx.arhybe.ui.components.BottomNavigationBar
import com.overdevx.arhybe.ui.screens.AuthScreen
import com.overdevx.arhybe.ui.theme.ArhyBeTheme
import com.overdevx.arhybe.ui.theme.primary
import com.overdevx.arhybe.viewmodel.AuthState
import com.overdevx.arhybe.viewmodel.AuthViewModel
import com.overdevx.arhybe.viewmodel.BluetoothViewModelAdvance
import com.overdevx.arhybe.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


const val TAG_BLE = "BLE_WiFi_Provision"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Dapatkan instance BluetoothViewModel menggunakan Hilt
    private val bluetoothViewModel: BluetoothViewModelAdvance by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Array izin yang diperlukan untuk BLE
    private val requiredBlePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION // Tetap direkomendasikan
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // ActivityResultLauncher untuk menangani hasil permintaan izin
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.d(TAG_BLE, "Semua izin BLE diberikan via launcher (MainActivity).")
                // Aksi setelah izin diberikan biasanya akan di-trigger ulang oleh UI
                // atau dari ViewModel jika ada state yang menunggu izin.
                // Contoh: Jika pengguna menekan tombol scan dan izin diminta, setelah izin diberikan,
                // pengguna mungkin perlu menekan tombol scan lagi, atau ViewModel bisa otomatis mencoba lagi.
                // Untuk kasus ini, kita biarkan UI yang memanggil aksi BLE lagi.
                Toast.makeText(this, "Izin BLE diberikan. Anda bisa mencoba lagi aksi Bluetooth.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG_BLE, "Tidak semua izin BLE diberikan via launcher (MainActivity).")
                Toast.makeText(this, "Beberapa izin BLE ditolak. Fungsionalitas mungkin terbatas.", Toast.LENGTH_LONG).show()
                // Handle kasus di mana beberapa izin penting ditolak (misalnya, tampilkan dialog penjelasan)
            }
        }

    /**
     * Fungsi publik untuk dipanggil dari Composable (melalui lambda)
     * untuk memicu permintaan izin BLE.
     */
    fun requestBlePermissions() {
        val permissionsToRequest = requiredBlePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG_BLE, "Meminta izin BLE dari MainActivity: ${permissionsToRequest.joinToString()}")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG_BLE, "Semua izin BLE sudah diberikan (saat requestBlePermissions dipanggil di MainActivity).")
            // Jika fungsi ini dipanggil dan izin sudah ada, Composable pemanggil
            // dapat melanjutkan dengan aksi BLE-nya.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            bluetoothViewModel.provisioningSuccessEvent.collectLatest { deviceId ->
                // Event diterima dari BluetoothViewModel!

                // 1. Dapatkan token dari AuthViewModel
                val token = authViewModel.getIdToken()

                // 2. Panggil fungsi di HomeViewModel dengan membawa token dan deviceId
                homeViewModel.onDeviceProvisioned(token, deviceId)
            }
        }
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()
            ArhyBeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (authState) {
                        is AuthState.Authenticated -> {
                            val navController = rememberNavController()
                            Scaffold(
                                bottomBar = {
                                    Column {
                                        // 1. Buat Box yang berfungsi sebagai shadow
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp) // Tinggi area shadow
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            primary.copy(alpha = 0.3f)

                                                        )
                                                    )
                                                )
                                        )
                                        // 2. Tampilkan BottomNavigationBar di bawah shadow
                                        BottomNavigationBar(navController = navController)
                                    }
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    MainNavHost(navController = navController,
                                        bluetoothViewModel = bluetoothViewModel,
                                        requestPermissionsLambda = {requestBlePermissions()})
                                }
                            }
                        }
                        is AuthState.Unauthenticated -> {
                            AuthScreen(navController = rememberNavController()) // NavController sementara
                        }
                        is AuthState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Error -> {
                            // Tampilkan pesan error atau AuthScreen lagi
                            AuthScreen(navController = rememberNavController())
                        }
                    }

                }
            }
        }
    }


}

