package com.overdevx.arhybe.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.primary
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorRed
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.viewmodel.AuthViewModel
import com.overdevx.arhybe.viewmodel.BluetoothViewModelAdvance
import com.overdevx.arhybe.viewmodel.DeviceConnectionState
import com.overdevx.arhybe.viewmodel.ProvisioningSubScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothConnectionBottomSheet(
    onDismiss: () -> Unit,
    viewModel: BluetoothViewModelAdvance,
    onRequestPermissions: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val isWifiProvisioned by viewModel.isWifiProvisioned.collectAsStateWithLifecycle()

    // Efek untuk menutup sheet secara otomatis saat provisioning berhasil
    LaunchedEffect(isWifiProvisioned) {
        if (isWifiProvisioned) {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetAndDisconnect()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // Konten utama sheet
        BluetoothSheetContent(
            viewModel = viewModel,
            onRequestPermissions = onRequestPermissions,
            onFinished = {
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismiss()
                    }
                }
            }
        )
    }
}

@Composable
private fun BluetoothSheetContent(
    viewModel: BluetoothViewModelAdvance,
    onRequestPermissions: () -> Unit,
    onFinished: () -> Unit
) {
    val subScreen by viewModel.currentSubScreen.collectAsStateWithLifecycle()

    Column(modifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = 32.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (subScreen) {
                    ProvisioningSubScreen.CHECKLIST -> "Hubungkan Perangkat Anda"
                    ProvisioningSubScreen.PAIRING -> "Pilih Perangkat Anda"
                    ProvisioningSubScreen.WIFI_CONFIG -> "Konfigurasi WiFi Perangkat"
                },
                color = textColorWhite,
                fontSize = 20.sp,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                modifier = Modifier.weight(1f)
            )
            // Tombol close tidak lagi diperlukan karena ada drag handle dan gestur
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = subScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, 90))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            label = "SubScreenAnimation"
        ) { targetScreen ->
            when (targetScreen) {
                ProvisioningSubScreen.CHECKLIST -> ChecklistScreen(
                    viewModel,
                    onRequestPermissions,
                    onFinished
                )

                ProvisioningSubScreen.PAIRING -> PairingScreen(viewModel)
                ProvisioningSubScreen.WIFI_CONFIG -> WifiConfigScreen(viewModel)
            }
        }
    }
}


@Composable
private fun ChecklistScreen(
    viewModel: BluetoothViewModelAdvance,
    onRequestPermissions: () -> Unit,
    onFinished: () -> Unit
) {

    val context = LocalContext.current
    val isBtEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isWifiProvisioned by viewModel.isWifiProvisioned.collectAsStateWithLifecycle()
    var hasPermissions by remember { mutableStateOf(hasAllPermissions(context)) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.checkBluetoothState() }

    LaunchedEffect(Unit) {
        hasPermissions = hasAllPermissions(context)
        viewModel.checkBluetoothState()
    }

    Column {
        ChecklistItem2(
            title = "Bluetooth Aktif",
            description = if (isBtEnabled) "Bluetooth sudah aktif" else "Nyalakan Bluetooth untuk melanjutkan",
            isCompleted = isBtEnabled,
            isActive = true
        ) {
            if (!isBtEnabled) {
                Button(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Aktifkan",
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                        fontSize = 14.sp,
                        color = textColorWhite
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = secondary
        )

        val isDeviceConnected = connectionState is DeviceConnectionState.READY_FOR_WIFI
        ChecklistItem2(
            title = "Hubungkan ke Perangkat",
            description = if (isDeviceConnected) "Perangkat ARhyBe terhubung" else "Ketuk 'Mulai' untuk mencari perangkat",
            isCompleted = isDeviceConnected,
            isActive = isBtEnabled && hasPermissions
        ) {
            if (!isDeviceConnected) {
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
                            viewModel.navigateToSubScreen(ProvisioningSubScreen.PAIRING)
                        } else {
                            onRequestPermissions()
                        }
                    },
                    enabled = isBtEnabled && hasPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Mulai",
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                        fontSize = 14.sp,
                        color = textColorWhite,
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = secondary
        )

        ChecklistItem2(
            title = "Konfigurasi WiFi",
            description = if (isWifiProvisioned) "WiFi berhasil dikonfigurasi" else "Selesaikan langkah sebelumnya",
            isCompleted = isWifiProvisioned,
            isActive = isDeviceConnected
        ) {
            if (!isWifiProvisioned) {
                Button(
                    onClick = { viewModel.navigateToSubScreen(ProvisioningSubScreen.WIFI_CONFIG) },
                    enabled = isDeviceConnected,
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Konfigurasi",
                        fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                        fontSize = 14.sp,
                        color = textColorWhite,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onFinished() }, // Diubah di sini
            modifier = Modifier.fillMaxWidth(),
            enabled = isWifiProvisioned,
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Selesai",
                fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                fontSize = 14.sp,
                color = textColorWhite
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PairingScreen(viewModel: BluetoothViewModelAdvance) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val devices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startBleScan(context)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isScanning) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textColorGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Mencari perangkat terdekat...",
                    fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                    fontSize = 14.sp,
                    color = textColorWhite
                )
            }
        }

        LazyColumn(modifier = Modifier.heightIn(max = 250.dp, min = 100.dp)) {
            items(devices, key = { it.address }) { device ->

                val isConnecting = connectionState == DeviceConnectionState.CONNECTING
                DeviceItem(device = device, isConnecting = isConnecting) {
                    if (!isConnecting) {
                        viewModel.connectToDevice(context, device)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiConfigScreen(
    viewModel: BluetoothViewModelAdvance
) {
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    // State untuk menampilkan loading indicator saat proses pengiriman
    var isSendingConfig by remember { mutableStateOf(false) }

    // Gunakan coroutine scope untuk memanggil suspend function dari onClick
    val coroutineScope = rememberCoroutineScope()

    val textFieldColors = TextFieldDefaults.colors(
        focusedTextColor = textColorWhite,
        unfocusedTextColor = textColorWhite,
        cursorColor = textColorGreen,
        focusedContainerColor = secondary,
        unfocusedContainerColor = secondary,
        focusedIndicatorColor = textColorGreen,
        unfocusedIndicatorColor = Color.Transparent,
        focusedLabelColor = textColorGreen,
        unfocusedLabelColor = textColorWhite,
    )

    Column {
        // ... (UI OutlinedTextField untuk SSID dan Password tetap sama)
        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("WiFi Name (SSID)") },
            // ...
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("WiFi Password") },
            // ...
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Perubahan Utama Ada di Sini ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tampilkan loading jika sedang mengirim
            if (isSendingConfig) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = textColorGreen,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mengonfigurasi...", color = textColorWhite)
            } else {
                TextButton(
                    onClick = { viewModel.navigateToSubScreen(ProvisioningSubScreen.CHECKLIST) },
                    enabled = !isSendingConfig
                ) {
                    Text("Cancel", color = textColorWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Jalankan proses pengiriman di dalam coroutine
                        coroutineScope.launch {
                            isSendingConfig = true // Tampilkan loading
                            viewModel.sendWifiCredentials(context, ssid, password)

                            isSendingConfig = false // Sembunyikan loading
                            viewModel.navigateToSubScreen(ProvisioningSubScreen.CHECKLIST)
                        }
                    },
                    enabled = !isSendingConfig,
                    colors = ButtonDefaults.buttonColors(containerColor = textColorGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send & Connect")
                }
            }
        }
    }
}

// --- Helper Composables ---

@Composable
private fun ChecklistItem2(
    title: String,
    description: String,
    isCompleted: Boolean,
    isActive: Boolean,
    actionButton: @Composable (() -> Unit)? = null
) {
    val icon =
        if (isCompleted) painterResource(R.drawable.ic_check_circle) else painterResource(R.drawable.ic_cancel_circle)
    val iconColor = if (isCompleted) textColorGreen else if (isActive) textColorRed else Color.Gray
    val contentColor = if (isActive || isCompleted) textColorWhite else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = "Status",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_semibold))),
                color = contentColor, fontSize = 16.sp
            )
            Text(
                description,
                fontFamily = FontFamily(listOf(Font(R.font.sofia_medium))),
                fontSize = 14.sp, color = contentColor.copy(alpha = 0.7f), lineHeight = 18.sp
            )
        }
        if (actionButton != null) {
            Box(contentAlignment = Alignment.CenterEnd) {
                actionButton()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItem(device: BluetoothDevice, isConnecting: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = !isConnecting, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = secondary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_aritmia),
                contentDescription = "Device",
                tint = textColorWhite,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name ?: "Unknown Device",
                    color = textColorWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(device.address, color = textColorWhite.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textColorGreen)
            } else {
                Icon(
                    painterResource(id = R.drawable.ic_chevron),
                    contentDescription = "Connect",
                    tint = textColorWhite
                )
            }
        }
    }
}

private fun hasAllPermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
        )
    }
    return permissions.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}


