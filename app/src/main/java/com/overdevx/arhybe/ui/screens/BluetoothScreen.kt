package com.overdevx.arhybe.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.overdevx.arhybe.viewmodel.BluetoothViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.ui.theme.textColorBlue
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite

const val TAG_BLE = "BLE_WiFi_Provision"

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    navController: NavController, // Jika perlu navigasi dari sini
    bluetoothViewModel: BluetoothViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit // Lambda untuk memicu permintaan izin dari Activity
) {
    val discoveredDevices by bluetoothViewModel.discoveredDevices.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val isScanning by bluetoothViewModel.isScanning.collectAsStateWithLifecycle()
    val connectionState by bluetoothViewModel.connectionState.collectAsStateWithLifecycle()
    val wifiStatusFromEsp by bluetoothViewModel.wifiStatusFromEsp.collectAsStateWithLifecycle()
    val isWifiProvisioned by bluetoothViewModel.isWifiProvisioned.collectAsStateWithLifecycle()
    var showWifiDialog by remember { mutableStateOf(false) }

    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    val context = LocalContext.current // Dapatkan context
    // Jika dialog harus ditampilkan
    if (showWifiDialog) {
        WifiCredentialsDialog(
            onDismissRequest = { showWifiDialog = false },
            onConfirm = { ssid, password ->
                if (ssid.isNotBlank()) {
                    bluetoothViewModel.sendWifiCredentials(context, ssid, password)
                } else {
                    Log.w(TAG_BLE, "SSID cannot be empty")
                }
                showWifiDialog = false
            }
        )
    }
    // Efek untuk navigasi setelah provisioning berhasil
    LaunchedEffect(isWifiProvisioned) {
        if (isWifiProvisioned) {
            Log.d(
                TAG_BLE,
                "WiFi Provisioned, navigating back from BluetoothScreen or to next screen."
            )
            // Contoh: Navigasi kembali ke HomeScreen atau ke screen fitur utama
            // navController.popBackStack()
            // atau
            // navController.navigate("actual_feature_route") { popUpTo(Screen.BluetoothProvisioning.route) { inclusive = true } }
            // Untuk sekarang, kita set status saja. Navigasi bisa di-handle di HomeScreen.
        }
    }
    if (isWifiProvisioned) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = "Koneksi Berhasil",
                    fontSize = 30.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                    color = textColorWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wifi),
                    contentDescription = null,
                    tint = textColorBlue,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Perangkat ARhyBe telah berhasil terhubung \ndengan wifi",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_light))),
                    color = textColorWhite,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonColors(
                    containerColor = textColorBlue,
                    contentColor = textColorWhite,
                    disabledContainerColor = textColorBlue,
                    disabledContentColor = textColorWhite
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    "Selesai",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                    color = textColorWhite,
                    modifier = Modifier
                )
            }
        }

    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Hubungkan Bluetooth",
                    fontSize = 30.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                    color = textColorWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )
                Text(
                    text = "Nyalakan perangkat ARhyBe untuk mulai \nmenghubungkan",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                    color = textColorWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
            if (discoveredDevices.isEmpty()) {
                DotLottieAnimation(
                    source = DotLottieSource.Asset("bluetooth.lottie"),
                    autoplay = true,
                    loop = true,
                    speed = 1f,
                    useFrameInterpolation = false,
                    playMode = Mode.FORWARD,
                    modifier = Modifier.size(300.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isScanning && discoveredDevices.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(discoveredDevices, key = { it.address }) { device ->
                    val deviceName = try {
                        // Pengecekan izin BLUETOOTH_CONNECT sebelum akses device.name
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
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
                            .clickable(
                                enabled = !connectionState.startsWith("Connected") && !connectionState.startsWith(
                                    "Connecting"
                                ),

                                ) {
                                // Cek izin sebelum konek
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    onRequestPermissions()
                                } else {
                                    bluetoothViewModel.connectToDevice(context, device)

                                }
                            },
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_bluetooth),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = deviceName,
                                fontSize = 16.sp,
                                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                                color = textColorWhite,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = connectionState,
                fontSize = 12.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_light))),
                color = if (connectionState == "Disconnected") textColorRed else textColorGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
            )


            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = wifiStatusFromEsp,
                fontSize = 16.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                color = textColorWhite,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (connectionState.startsWith("Connecting")
                || connectionState.startsWith("Connected")
                || connectionState.startsWith("Ready")
            ) {
                Button(
                    onClick = { showWifiDialog = true },
                    colors = ButtonColors(
                        containerColor = textColorBlue,
                        contentColor = textColorWhite,
                        disabledContainerColor = textColorBlue,
                        disabledContentColor = textColorWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Hubungkan dengan Wifi",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                        color = textColorWhite,
                        modifier = Modifier
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Tombol disconnect selalu ada jika ada upaya koneksi atau sudah terhubung

                Button(
                    onClick = { bluetoothViewModel.disconnectDevice(context) },
                    colors = ButtonColors(
                        containerColor = textColorRed,
                        contentColor = textColorWhite,
                        disabledContainerColor = textColorRed,
                        disabledContentColor = textColorWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Disconnect from ESP32",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                        color = textColorWhite,
                        modifier = Modifier
                    )
                }
            }else{
                Button(
                    onClick = {
                        // Cek izin dulu sebelum scan. Jika tidak ada, panggil lambda onRequestPermissions.
                        val permissionsArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                        val allPermissionsGranted = permissionsArray.all {
                            ContextCompat.checkSelfPermission(
                                context,
                                it
                            ) == PackageManager.PERMISSION_GRANTED
                        }

                        if (allPermissionsGranted) {
                            if (isScanning) bluetoothViewModel.stopBleScan(context) else bluetoothViewModel.startBleScan(
                                context
                            )
                        } else {
                            onRequestPermissions() // Panggil lambda untuk minta izin dari Activity
                        }
                    },
                    colors = ButtonColors(
                        containerColor = textColorBlue,
                        contentColor = textColorWhite,
                        disabledContainerColor = textColorBlue,
                        disabledContentColor = textColorWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isScanning) "Memindai" else "Pindai Perangkat",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                        color = textColorWhite,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiCredentialsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val textFieldColors = TextFieldDefaults.colors(
        focusedTextColor = textColorWhite,
        unfocusedTextColor = textColorWhite,
        focusedContainerColor = background,
        unfocusedContainerColor = background,
        focusedIndicatorColor = textColorBlue,
        unfocusedIndicatorColor = textColorWhite,
        focusedLabelColor = textColorBlue,
        unfocusedLabelColor = textColorWhite
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                "Masukkan Kredensial Wifi",
                fontSize = 16.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                color = textColorWhite
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = {
                        Text(
                            "SSID",
                            fontSize = 16.sp,
                            fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                            color = textColorWhite
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            "Password",
                            fontSize = 16.sp,
                            fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                            color = textColorWhite
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_regular))),
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ssid, password) },
                colors = ButtonDefaults.textButtonColors(contentColor = textColorBlue)
            ) {
                Text(
                    "Kirim",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(contentColor = textColorWhite)
            ) {
                Text(
                    "Batal",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                )
            }
        },
        containerColor = secondary
    )
}

@Composable
private fun InfoDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val textFieldColors = TextFieldDefaults.colors(
        focusedTextColor = textColorWhite,
        unfocusedTextColor = textColorWhite,
        focusedContainerColor = background,
        unfocusedContainerColor = background,
        focusedIndicatorColor = textColorBlue,
        unfocusedIndicatorColor = textColorWhite,
        focusedLabelColor = textColorBlue,
        unfocusedLabelColor = textColorWhite
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Koneksi Berhasil",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                    color = textColorWhite,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

        },
        text = {
            Column {
                Text(
                    "Koneksi Berhasil",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                    color = textColorWhite,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(ssid, password) },
                colors = ButtonColors(
                    containerColor = textColorBlue,
                    contentColor = textColorWhite,
                    disabledContainerColor = textColorBlue,
                    disabledContentColor = textColorWhite
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Oke",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                )
            }
        },

        containerColor = secondary
    )
}