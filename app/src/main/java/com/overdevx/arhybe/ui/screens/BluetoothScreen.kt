package com.overdevx.arhybe.ui.screens
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.overdevx.arhybe.viewmodel.BluetoothViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
const val TAG_BLE = "BLE_WiFi_Provision"

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    navController: NavController, // Jika perlu navigasi dari sini
    bluetoothViewModel: BluetoothViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit // Lambda untuk memicu permintaan izin dari Activity
) {
    val discoveredDevices by bluetoothViewModel.discoveredDevices.collectAsStateWithLifecycle(initialValue = emptyList())
    val isScanning by bluetoothViewModel.isScanning.collectAsStateWithLifecycle()
    val connectionState by bluetoothViewModel.connectionState.collectAsStateWithLifecycle()
    val wifiStatusFromEsp by bluetoothViewModel.wifiStatusFromEsp.collectAsStateWithLifecycle()
    val isWifiProvisioned by bluetoothViewModel.isWifiProvisioned.collectAsStateWithLifecycle()


    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    val context = LocalContext.current // Dapatkan context

    // Efek untuk navigasi setelah provisioning berhasil
    LaunchedEffect(isWifiProvisioned) {
        if (isWifiProvisioned) {
            Log.d(TAG_BLE, "WiFi Provisioned, navigating back from BluetoothScreen or to next screen.")
            // Contoh: Navigasi kembali ke HomeScreen atau ke screen fitur utama
            // navController.popBackStack()
            // atau
            // navController.navigate("actual_feature_route") { popUpTo(Screen.BluetoothProvisioning.route) { inclusive = true } }
            // Untuk sekarang, kita set status saja. Navigasi bisa di-handle di HomeScreen.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ESP32 BLE WiFi Provisioning", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Cek izin dulu sebelum scan. Jika tidak ada, panggil lambda onRequestPermissions.
            val permissionsArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
            }
            val allPermissionsGranted = permissionsArray.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

            if (allPermissionsGranted) {
                if (isScanning) bluetoothViewModel.stopBleScan(context) else bluetoothViewModel.startBleScan(context)
            } else {
                onRequestPermissions() // Panggil lambda untuk minta izin dari Activity
            }
        }) {
            Text(if (isScanning) "Scanning..." else "Scan BLE Devices")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(connectionState, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (isScanning && discoveredDevices.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(discoveredDevices, key = { it.address }) { device ->
                val deviceName = try {
                    // Pengecekan izin BLUETOOTH_CONNECT sebelum akses device.name
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        "Name N/A (Perm.)"
                    } else {
                        device.name ?: "Unknown Device"
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG_BLE, "SecurityException getting name in UI for ${device.address}")
                    "Name N/A (SecEx)"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !connectionState.startsWith("Connected") && !connectionState.startsWith("Connecting")) {
                            // Cek izin sebelum konek
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                onRequestPermissions()
                            } else {
                                bluetoothViewModel.connectToDevice(context, device)
                            }
                        },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nama: $deviceName")
                        Text("Alamat: ${device.address}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (connectionState.startsWith("Connected") || connectionState.startsWith("Ready to send credentials")) {
            OutlinedTextField(
                value = wifiSsid,
                onValueChange = { wifiSsid = it },
                label = { Text("SSID WiFi") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = wifiPassword,
                onValueChange = { wifiPassword = it },
                label = { Text("Password WiFi") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (wifiSsid.isNotBlank()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        onRequestPermissions()
                    } else {
                        bluetoothViewModel.sendWifiCredentials(context, wifiSsid, wifiPassword)
                    }
                } else {
                    Log.w(TAG_BLE, "SSID tidak boleh kosong")
                    // Tampilkan Snackbar atau pesan error ke pengguna
                }
            }) {
                Text("Send WiFi Credentials")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Tombol disconnect selalu ada jika ada upaya koneksi atau sudah terhubung
        if (connectionState.startsWith("Connecting") || connectionState.startsWith("Connected") || connectionState.startsWith("Ready")) {
            Button(onClick = { bluetoothViewModel.disconnectDevice(context) }) {
                Text("Disconnect from ESP32")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(wifiStatusFromEsp, style = MaterialTheme.typography.bodyLarge)

        if (isWifiProvisioned) {
            Text("WiFi Provisioning Berhasil!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            // Tambahkan tombol untuk kembali atau navigasi otomatis
            Button(onClick = { navController.popBackStack() /* atau ke screen lain */ }) {
                Text("Selesai & Kembali")
            }
        }
    }
}