package com.overdevx.arhybe.viewmodel
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
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
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject

// --- Constants ---
const val TAG_BLE = "BLE_WiFi_Provision"
val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
val WIFI_CREDENTIALS_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
val WIFI_STATUS_CHAR_UUID: UUID = UUID.fromString("0c75a186-5972-4187-8f73-3ad9f8afc8d9")
val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// --- State Management for UI ---
enum class ProvisioningSubScreen {
    CHECKLIST,
    PAIRING,
    WIFI_CONFIG
}

@HiltViewModel
class BluetoothViewModelAdvance @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val bluetoothManager: BluetoothManager by lazy {
        application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private var bluetoothGatt: BluetoothGatt? = null
    private var wifiCredentialsCharacteristic: BluetoothGattCharacteristic? = null

    // --- StateFlows for UI ---
    private val _discoveredDevicesFlow = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevicesFlow.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionState = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.DISCONNECTED)
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _currentSubScreen = MutableStateFlow(ProvisioningSubScreen.CHECKLIST)
    val currentSubScreen: StateFlow<ProvisioningSubScreen> = _currentSubScreen.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isWifiProvisioned = MutableStateFlow(false)
    val isWifiProvisioned: StateFlow<Boolean> = _isWifiProvisioned.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    private val _provisioningSuccessEvent = MutableSharedFlow<String>()
    val provisioningSuccessEvent = _provisioningSuccessEvent.asSharedFlow()
    init {
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        checkBluetoothState()
    }

    fun checkBluetoothState() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
    }

    fun navigateToSubScreen(screen: ProvisioningSubScreen) {
        _currentSubScreen.value = screen
    }
    // ================== FIX: KEMBALIKAN SCAN CALLBACK YANG HILANG ==================
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                // Periksa apakah nama device tidak kosong, untuk menghindari device tanpa nama
                if (!device.name.isNullOrEmpty()) {
                    _discoveredDevicesFlow.update { currentList ->
                        if (currentList.none { it.address == device.address }) {
                            Log.d(TAG_BLE, "Device found: ${device.name} (${device.address})")
                            currentList + device
                        } else {
                            currentList
                        }
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG_BLE, "BLE Scan Failed with error code: $errorCode")
            _isScanning.value = false
        }
    }
    // ==============================================================================

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = DeviceConnectionState.CONNECTED
                    bluetoothGatt = gatt
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = DeviceConnectionState.DISCONNECTED
                    closeGatt()
                }
            } else {
                _connectionState.value = DeviceConnectionState.ERROR("Connection failed, status: $status")
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                wifiCredentialsCharacteristic = service?.getCharacteristic(WIFI_CREDENTIALS_CHAR_UUID)
                val statusChar = service?.getCharacteristic(WIFI_STATUS_CHAR_UUID)

                if (statusChar != null && gatt != null) {
                    enableNotifications(application, gatt, statusChar)
                } else {
                    _connectionState.value = DeviceConnectionState.ERROR("Required service/char not found")
                }
            } else {
                _connectionState.value = DeviceConnectionState.ERROR("Service discovery failed")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG_BLE, "Credentials sent successfully.")
            } else {
                Log.e(TAG_BLE, "Credential write failed, status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val statusString = String(value, Charset.forName("UTF-8"))
            if (statusString.contains("Connected! IP:")) {
                _isWifiProvisioned.value = true
                viewModelScope.launch {
                    // Kirim event bahwa provisioning berhasil dengan deviceId "ESP32_ECG_01"
                    _provisioningSuccessEvent.emit("ESP32_ECG_01")
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                viewModelScope.launch {
                    _connectionState.value = DeviceConnectionState.READY_FOR_WIFI
                    navigateToSubScreen(ProvisioningSubScreen.CHECKLIST)
                }
            } else {
                _connectionState.value = DeviceConnectionState.ERROR("Notification setup failed")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan(context: Context) {
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        if (!hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.e(TAG_BLE, "ACCESS_FINE_LOCATION permission missing for startBleScan on older Android.")
            _connectionState.value = DeviceConnectionState.ERROR("Error: Location Permission Missing")
            return
        }
        if (bluetoothLeScanner == null || !_isBluetoothEnabled.value) return

        _isScanning.value = true
        _discoveredDevicesFlow.value = emptyList()
        val scanFilters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build())
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            Log.d(TAG_BLE, "BLE Scan actually started.")
            handler.postDelayed({
                if (_isScanning.value) { // Hanya stop jika masih scanning
                    stopBleScan(context) // Pass context
                }
            }, 10000)
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on startScan: ${se.message}")
            _isScanning.value = false
            _connectionState.value = DeviceConnectionState.ERROR("Error: Scan Permission Denied (runtime)")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan(context: Context) {
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        if (_isScanning.value) {
            bluetoothLeScanner?.stopScan(scanCallback)
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(context: Context, device: BluetoothDevice) {
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        stopBleScan(context)
        _connectionState.value = DeviceConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(application, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun sendWifiCredentials(context: Context, ssid: String, pass: String) {
        if (bluetoothGatt == null || wifiCredentialsCharacteristic == null) return
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return

        val credentials = "$ssid;$pass"
        wifiCredentialsCharacteristic?.let { char ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    char,
                    credentials.toByteArray(Charset.forName("UTF-8")),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                char.value = credentials.toByteArray(Charset.forName("UTF-8"))
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeCharacteristic(char)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(context: Context, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val cccdDescriptor = characteristic.getDescriptor(CCCD_UUID) ?: return
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return

        gatt.setCharacteristicNotification(characteristic, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccdDescriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            cccdDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(cccdDescriptor)
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        if (bluetoothGatt == null) return
        if (hasPermission(application, Manifest.permission.BLUETOOTH_CONNECT) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothGatt?.close()
        }
        bluetoothGatt = null
    }

    fun resetAndDisconnect() {
        closeGatt()
        _connectionState.value = DeviceConnectionState.DISCONNECTED
        _isWifiProvisioned.value = false
        navigateToSubScreen(ProvisioningSubScreen.CHECKLIST)
    }

    override fun onCleared() {
        super.onCleared()
        closeGatt()
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

}

sealed class DeviceConnectionState {
    object DISCONNECTED : DeviceConnectionState()
    object CONNECTING : DeviceConnectionState()
    object CONNECTED : DeviceConnectionState()
    object READY_FOR_WIFI: DeviceConnectionState()
    data class ERROR(val message: String) : DeviceConnectionState()
}


