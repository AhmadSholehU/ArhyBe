package com.overdevx.arhybe

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.overdevx.arhybe.navigation.MainNavHost
import com.overdevx.arhybe.ui.components.BottomNavigationBar
import com.overdevx.arhybe.ui.screens.BluetoothScreen
import com.overdevx.arhybe.ui.theme.ArhyBeTheme
import com.overdevx.arhybe.viewmodel.BluetoothViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


const val TAG_BLE = "BLE_WiFi_Provision"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    // Dapatkan instance BluetoothViewModel menggunakan Hilt
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

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
        enableEdgeToEdge()
        setContent {
//            ArhyBeTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    BluetoothScreen(
//                        discoveredDevices = discoveredDevices,
//                        isScanning = isScanning.value,
//                        connectionState = connectionState.value,
//                        wifiStatusFromEsp = wifiStatusFromEsp.value,
//                        onScanClicked = {
//                            if (_isScanning.value) stopBleScan() else checkAndRequestPermissions()
//                        },
//                        onConnectDevice = { device -> connectToDevice(device) },
//                        onDisconnectDevice = { disconnectDevice() },
//                        onSendCredentials = { ssid, password ->
//                            sendWifiCredentials(ssid, password)
//                        }
//                    )
//                }
//            }

            ArhyBeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Scaffold(
                        bottomBar = { BottomNavigationBar(navController = navController) }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            MainNavHost(navController = navController,
                                bluetoothViewModel = bluetoothViewModel,
                                requestPermissionsLambda = {requestBlePermissions()})
                        }
                    }
                }
            }
        }
    }


}

