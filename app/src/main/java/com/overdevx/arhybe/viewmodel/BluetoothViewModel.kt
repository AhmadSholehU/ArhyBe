package com.overdevx.arhybe.viewmodel
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.*
import javax.inject.Inject
// Definisikan konstanta di sini atau import dari file yang sesuai
 const val TAG_BLE = "BLE_WiFi_Provision"
 val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
 val WIFI_CREDENTIALS_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
 val WIFI_STATUS_CHAR_UUID: UUID = UUID.fromString("0c75a186-5972-4187-8f73-3ad9f8afc8d9")
 val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val bluetoothManager: BluetoothManager by lazy {
        application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private var bluetoothLeScanner: BluetoothLeScanner? = null // Akan diinisialisasi nanti

    private var bluetoothGatt: BluetoothGatt? = null
    private var wifiCredentialsCharacteristic: BluetoothGattCharacteristic? = null
    private var wifiStatusCharacteristic: BluetoothGattCharacteristic? = null

    // Mengubah _discoveredDevices menjadi MutableStateFlow
    private val _discoveredDevicesFlow = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevicesFlow.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _wifiStatusFromEsp = MutableStateFlow("ESP WiFi Status: Idle")
    val wifiStatusFromEsp: StateFlow<String> = _wifiStatusFromEsp.asStateFlow()

    // Flag untuk menandakan apakah provisioning WiFi telah berhasil
    // Idealnya ini disimpan di DataStore/SharedPreferences untuk persistensi antar sesi aplikasi
    private val _isWifiProvisioned = MutableStateFlow(false)
    val isWifiProvisioned: StateFlow<Boolean> = _isWifiProvisioned.asStateFlow()


    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000 // 10 detik

    init {
        // Inisialisasi scanner jika adapter tersedia
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                _discoveredDevicesFlow.update { currentList ->
                    if (currentList.none { it.address == device.address }) {
                        currentList + device // Tambahkan device baru ke list
                    } else {
                        currentList // Jika sudah ada, kembalikan list yang sama
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                result.device?.let { device ->
                    _discoveredDevicesFlow.update { currentList ->
                        if (currentList.none { it.address == device.address }) {
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
            Log.e(TAG_BLE, "Scan failed with error code: $errorCode")
            _isScanning.value = false
            _connectionState.value = "Error: Scan Failed ($errorCode)"
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val device = gatt?.device
            val deviceName = try { device?.name ?: device?.address } catch (e: SecurityException) { device?.address ?: "Unknown Device" }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(TAG_BLE, "Successfully connected to $deviceName")
                    _connectionState.value = "Connected to $deviceName"
                    bluetoothGatt = gatt
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            if (hasPermission(application, Manifest.permission.BLUETOOTH_CONNECT)) {
                                val discoveryInitiated = gatt?.discoverServices()
                                Log.d(TAG_BLE, "Service discovery initiated for $deviceName: $discoveryInitiated")
                                if (discoveryInitiated != true) {
                                    withContext(Dispatchers.Main) {
                                        _connectionState.value = "Error: Discovery Init Failed"
                                        disconnectDevice(application) // Pass context
                                    }
                                }
                            } else {
                                Log.e(TAG_BLE,"BLUETOOTH_CONNECT permission missing for discoverServices.")
                                withContext(Dispatchers.Main) {
                                    _connectionState.value = "Error: Connect Permission (Services)"
                                    disconnectDevice(application)
                                }
                            }
                        } catch (se: SecurityException) {
                            Log.e(TAG_BLE,"SecurityException on discoverServices for $deviceName: ${se.message}")
                            withContext(Dispatchers.Main) {
                                _connectionState.value = "Error: Permission Denied (Services)"
                                disconnectDevice(application)
                            }
                        }
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG_BLE, "Successfully disconnected from $deviceName")
                    _connectionState.value = "Disconnected"
                    // _isWifiProvisioned.value = false; // Reset status provisioning jika disconnect? Tergantung kebutuhan.
                    closeGatt()
                }
            } else {
                Log.w(TAG_BLE, "Error $status encountered for $deviceName! Disconnecting...")
                _connectionState.value = "Error: Connection Failed ($status)"
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG_BLE, "Services Discovered!")
                val service = gatt?.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e(TAG_BLE, "Service UUID not found: $SERVICE_UUID")
                    _connectionState.value = "Error: Service Not Found"
                    disconnectDevice(application)
                    return
                }
                wifiCredentialsCharacteristic = service.getCharacteristic(WIFI_CREDENTIALS_CHAR_UUID)
                wifiStatusCharacteristic = service.getCharacteristic(WIFI_STATUS_CHAR_UUID)

                if (wifiCredentialsCharacteristic == null) Log.e(TAG_BLE, "Credentials Char not found")
                if (wifiStatusCharacteristic == null) Log.e(TAG_BLE, "Status Char not found")

                if (wifiStatusCharacteristic != null && gatt != null) {
                    enableNotifications(application, gatt, wifiStatusCharacteristic!!)
                } else {
                    _connectionState.value = if (gatt == null) "Error: GATT null" else "Error: Status Char Missing"
                }
            } else {
                Log.w(TAG_BLE, "Service discovery failed with status: $status")
                _connectionState.value = "Error: Service Discovery Failed ($status)"
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (characteristic?.uuid == WIFI_CREDENTIALS_CHAR_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG_BLE, "Write to WIFI_CREDENTIALS_CHAR_UUID successful.")
                    // Jangan langsung set "Sending...", biarkan ESP yang update status
                } else {
                    Log.e(TAG_BLE, "Write to WIFI_CREDENTIALS_CHAR_UUID failed with status $status")
                    _wifiStatusFromEsp.value = "ESP WiFi Status: Write Failed ($status)"
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            Log.e(TAG_BLE, "!!!!!!!!!!!! ON_CHARACTERISTIC_CHANGED TRIGGERED! UUID: ${characteristic.uuid} !!!!!!!!!!!!") // Log yang sangat jelas

            if (characteristic.uuid == WIFI_STATUS_CHAR_UUID) {
                val statusString = String(value, Charset.forName("UTF-8"))
                Log.i(TAG_BLE, "Notification from WIFI_STATUS_CHAR_UUID: $statusString")
                _wifiStatusFromEsp.value = "ESP WiFi Status: $statusString"
                if (statusString.contains("Connected")) {
                    _isWifiProvisioned.value = true // Tandai WiFi sudah terkonfigurasi dan konek
                    Log.i(TAG_BLE, "WiFi Provisioning successful. isWifiProvisioned set to true.")
                    // Anda bisa tambahkan navigasi otomatis dari sini jika diinginkan,
                    // atau biarkan UI/user yang menentukan.
                    // Contoh: disconnectDevice(application) // Putuskan BLE setelah provisioning berhasil
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor?.uuid == CCCD_UUID && descriptor.characteristic.uuid == WIFI_STATUS_CHAR_UUID) {
                    Log.i(TAG_BLE, "Notifications enabled for WiFi Status")
                    _connectionState.value = "Ready to send credentials"
                }
            } else {
                Log.e(TAG_BLE, "Failed to write CCCD for ${descriptor?.characteristic?.uuid}, status: $status")
                _connectionState.value = "Error: Enabling Notifications Failed ($status)"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan(context: Context) { // Membutuhkan context untuk check permission
        Log.d(TAG_BLE, "Attempting to start BLE scan...")
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(TAG_BLE, "BLUETOOTH_SCAN permission missing for startBleScan.")
            _connectionState.value = "Error: Scan Permission Missing"
            // Pemicuan request permission idealnya dari UI/Activity
            return
        }
        if (!hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.e(TAG_BLE, "ACCESS_FINE_LOCATION permission missing for startBleScan on older Android.")
            _connectionState.value = "Error: Location Permission Missing"
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG_BLE, "Bluetooth not enabled or adapter null")
            _connectionState.value = "Error: Bluetooth off"
            _isScanning.value = false
            return
        }
        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner // Coba inisialisasi ulang jika null
            if (bluetoothLeScanner == null) {
                Log.e(TAG_BLE, "BluetoothLeScanner is null, cannot scan.")
                _connectionState.value = "Error: BLE Scanner unavailable"
                _isScanning.value = false
                return
            }
        }

        if (_isScanning.value) {
            Log.d(TAG_BLE, "Scan already in progress.")
            return
        }

        _discoveredDevicesFlow.value = emptyList()
        _isScanning.value = true
        _connectionState.value = "Scanning..."

        val scanFilters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build())
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            Log.d(TAG_BLE, "BLE Scan actually started.")
            handler.postDelayed({
                if (_isScanning.value) { // Hanya stop jika masih scanning
                    stopBleScan(context) // Pass context
                }
            }, SCAN_PERIOD)
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on startScan: ${se.message}")
            _isScanning.value = false
            _connectionState.value = "Error: Scan Permission Denied (runtime)"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan(context: Context) { // Membutuhkan context untuk check permission
        Log.d(TAG_BLE, "Attempting to stop BLE scan...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
                Log.w(TAG_BLE, "BLUETOOTH_SCAN permission not granted to stop scan. Cannot stop effectively.")
                // _isScanning.value = false // Tetap set false agar UI konsisten
                // return // Mungkin lebih baik tetap mencoba stop jika scanner tidak null
            }
        }
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(TAG_BLE, "BLE Scan stopped via stopBleScan().")
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on stopScan: ${se.message}")
        } finally {
            _isScanning.value = false // Pastikan ini selalu diset false
            if (bluetoothGatt == null && !_connectionState.value.contains("Connected")) {
                _connectionState.value = if (_connectionState.value.startsWith("Error: Scan")) _connectionState.value else "Scan stopped"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(context: Context, device: BluetoothDevice) {
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(TAG_BLE, "BLUETOOTH_CONNECT permission missing for connectToDevice.")
            _connectionState.value = "Error: Connect Permission Missing"
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG_BLE, "Bluetooth not enabled for connect.")
            _connectionState.value = "Error: Bluetooth off"
            return
        }
        if (_isScanning.value) {
            stopBleScan(context)
        }
        val deviceName = try { device.name ?: device.address } catch (e:SecurityException) { device.address }
        _connectionState.value = "Connecting to $deviceName..."
        try {
            bluetoothGatt = device.connectGatt(application, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on connectGatt: ${se.message}")
            _connectionState.value = "Error: Connect Permission Denied (runtime)"
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(context: Context) {
        Log.d(TAG_BLE, "Attempting to disconnect device.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.w(TAG_BLE, "BLUETOOTH_CONNECT permission missing for disconnect. Attempting anyway.")
            }
        }
        try {
            bluetoothGatt?.disconnect() // Ini akan memicu onConnectionStateChange
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on disconnect: ${se.message}")
            _connectionState.value = "Error: Disconnect Permission Denied"
            closeGatt() // Tutup paksa jika disconnect gagal karena izin
        }
    }

    @SuppressLint("MissingPermission") // Izin akan dicek secara eksplisit di dalam fungsi
    private fun closeGatt() {
        Log.d(TAG_BLE, "Attempting to close GATT.")
        if (bluetoothGatt == null) {
            Log.d(TAG_BLE, "GATT already null, no need to close.")
            return
        }

        // Pengecekan izin BLUETOOTH_CONNECT sebelum memanggil close() pada Android S+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    application, // Menggunakan application context dari ViewModel
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG_BLE, "BLUETOOTH_CONNECT permission missing for gatt.close(). Cannot close GATT gracefully.")
                // Dalam kasus ini, kita mungkin tidak bisa menutup GATT dengan aman.
                // Namun, kita tetap akan null-kan referensinya untuk mencegah penggunaan lebih lanjut.
                bluetoothGatt = null
                wifiCredentialsCharacteristic = null
                wifiStatusCharacteristic = null
                Log.w(TAG_BLE, "GATT reference nulled due to missing CONNECT permission for close.")
                return
            }
        }

        try {
            bluetoothGatt?.close()
            Log.d(TAG_BLE, "GATT closed successfully.")
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on gatt.close(): ${se.message}")
            // Meskipun ada exception, kita tetap null-kan untuk cleanup
        } finally {
            // Pastikan semua referensi dibersihkan terlepas dari apakah close() berhasil atau tidak
            bluetoothGatt = null
            wifiCredentialsCharacteristic = null
            wifiStatusCharacteristic = null
            Log.d(TAG_BLE, "GATT and characteristic references nulled after close attempt.")
        }
        // _connectionState.value = "Disconnected" // Biasanya sudah dihandle di onConnectionStateChange atau disconnectDevice
    }



    @SuppressLint("MissingPermission")
    fun sendWifiCredentials(context: Context, ssid: String, pass: String) {
        if (bluetoothGatt == null || wifiCredentialsCharacteristic == null) {
            Log.e(TAG_BLE, "Not connected or characteristic not found for sending credentials.")
            _wifiStatusFromEsp.value = "ESP WiFi Status: Not Connected"
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG_BLE, "BLUETOOTH_CONNECT permission not granted for writing credentials.")
            _wifiStatusFromEsp.value = "ESP WiFi Status: Write Permission Denied"
            return
        }

        val credentials = "$ssid;$pass"
        try {
            val char = wifiCredentialsCharacteristic
            if (char == null) {
                Log.e(TAG_BLE, "wifiCredentialsCharacteristic is null before write.")
                _wifiStatusFromEsp.value = "ESP WiFi Status: Error Char Null"
                return
            }
            // Menggunakan metode writeCharacteristic(BluetoothGattCharacteristic, ByteArray, Int) untuk API 33+
            // dan yang lama untuk versi sebelumnya.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = bluetoothGatt?.writeCharacteristic(
                    char,
                    credentials.toByteArray(Charset.forName("UTF-8")),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // atau WRITE_TYPE_NO_RESPONSE
                )
                Log.d(TAG_BLE, "Attempting to write credentials (API 33+): result code $result")
                if (result == BluetoothStatusCodes.SUCCESS) { // Ganti dengan konstanta yang benar jika beda
                    _wifiStatusFromEsp.value = "ESP WiFi Status: Sending..." // Update setelah pemanggilan berhasil
                } else {
                    _wifiStatusFromEsp.value = "ESP WiFi Status: Write init failed (API 33+ code $result)"
                }
            } else {
                @Suppress("DEPRECATION")
                char.value = credentials.toByteArray(Charset.forName("UTF-8"))
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                val success = bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG_BLE, "Attempting to write credentials (legacy): $success")
                if (success == true) {
                    _wifiStatusFromEsp.value = "ESP WiFi Status: Sending..."
                } else {
                    _wifiStatusFromEsp.value = "ESP WiFi Status: Write init failed (legacy)"
                }
            }
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on writeCharacteristic for credentials: ${se.message}")
            _wifiStatusFromEsp.value = "ESP WiFi Status: Write Permission Denied (runtime)"
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(context: Context, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val cccdDescriptor = characteristic.getDescriptor(CCCD_UUID)
        if (cccdDescriptor == null) {
            Log.e(TAG_BLE, "CCCD not found for ${characteristic.uuid}")
            _connectionState.value = "Error: CCCD Missing"
            return
        }

        // ========== FIX: TAMBAHKAN BARIS INI ==========
        // Beri tahu OS Android untuk mendengarkan notifikasi dari karakteristik ini.
        // Ini harus dipanggil SEBELUM menulis ke descriptor.
        gatt.setCharacteristicNotification(characteristic, true)
        // ===============================================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG_BLE, "BLUETOOTH_CONNECT permission not granted for enabling notifications.")
            _connectionState.value = "Error: Notification Permission Denied"
            return
        }

        try {
            var success = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = gatt.writeDescriptor(cccdDescriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                success = (result == 0) // BluetoothStatusCodes.SUCCESS is 0
            } else {
                @Suppress("DEPRECATION")
                cccdDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                success = gatt.writeDescriptor(cccdDescriptor)
            }
            Log.d(TAG_BLE, "Enabling notifications for ${characteristic.uuid}, success: $success")
            if(!success) {
                Log.e(TAG_BLE, "Failed to initiate writeDescriptor for CCCD.")
            }
        } catch (se: SecurityException) {
            Log.e(TAG_BLE, "SecurityException on writeDescriptor (enableNotifications): ${se.message}")
            _connectionState.value = "Error: Notification Permission Denied (runtime)"
        }
    }
    fun setWifiProvisionedStatus(isProvisioned: Boolean) {
        _isWifiProvisioned.value = isProvisioned
        // Simpan ke DataStore di sini jika diperlukan
    }

    // Fungsi utilitas untuk memeriksa izin (bisa juga diletakkan di file terpisah)
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }
    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG_BLE, "BluetoothViewModel onCleared. Closing GATT.")
        stopBleScan(application) // Hentikan scan jika masih berjalan
        closeGatt()
    }
}