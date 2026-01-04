package com.safeguard.app.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.safeguard.app.data.models.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Panic Button Manager
 * Connects to Bluetooth panic buttons and key fobs for SOS triggering
 * Supports various BLE panic button devices
 */
class PanicButtonManager(
    private val context: Context,
    private val sosManager: SOSManager
) {

    companion object {
        private const val TAG = "PanicButtonManager"
        
        // Common panic button service UUIDs
        private val PANIC_BUTTON_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val PANIC_BUTTON_CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        
        // Scan timeout
        private const val SCAN_TIMEOUT_MS = 30000L
    }

    data class PanicButton(
        val address: String,
        val name: String,
        val rssi: Int = 0,
        val isConnected: Boolean = false,
        val batteryLevel: Int? = null,
        val lastSeen: Long = System.currentTimeMillis()
    )

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Scanning : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val device: PanicButton) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<PanicButton>>(emptyList())
    val discoveredDevices: StateFlow<List<PanicButton>> = _discoveredDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PanicButton>>(emptyList())
    val pairedDevices: StateFlow<List<PanicButton>> = _pairedDevices.asStateFlow()

    private var isScanning = false

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Start scanning for panic button devices
     */
    fun startScan() {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = ConnectionState.Error("Bluetooth permissions not granted")
            return
        }

        if (!isBluetoothAvailable()) {
            _connectionState.value = ConnectionState.Error("Bluetooth is not enabled")
            return
        }

        if (isScanning) return

        try {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                
                bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
                isScanning = true
                _connectionState.value = ConnectionState.Scanning
                _discoveredDevices.value = emptyList()

                // Auto-stop scan after timeout
                scope.launch {
                    delay(SCAN_TIMEOUT_MS)
                    stopScan()
                }

                Log.d(TAG, "Started BLE scan for panic buttons")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            _connectionState.value = ConnectionState.Error("Failed to start scan: ${e.message}")
        }
    }

    /**
     * Stop scanning
     */
    fun stopScan() {
        if (!isScanning) return

        try {
            if (hasBluetoothPermissions()) {
                bluetoothLeScanner?.stopScan(scanCallback)
            }
            isScanning = false
            
            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
            
            Log.d(TAG, "Stopped BLE scan")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop scan", e)
        }
    }

    /**
     * Connect to a panic button device
     */
    fun connectToDevice(device: PanicButton) {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = ConnectionState.Error("Bluetooth permissions not granted")
            return
        }

        stopScan()
        _connectionState.value = ConnectionState.Connecting

        try {
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                
                bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)
                Log.d(TAG, "Connecting to ${device.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            _connectionState.value = ConnectionState.Error("Failed to connect: ${e.message}")
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        try {
            if (hasBluetoothPermissions()) {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
            bluetoothGatt = null
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Disconnected from panic button")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
        }
    }

    /**
     * Save device as paired
     */
    fun pairDevice(device: PanicButton) {
        val currentPaired = _pairedDevices.value.toMutableList()
        if (currentPaired.none { it.address == device.address }) {
            currentPaired.add(device)
            _pairedDevices.value = currentPaired
            // In production, save to SharedPreferences or database
        }
    }

    /**
     * Remove paired device
     */
    fun unpairDevice(device: PanicButton) {
        val currentPaired = _pairedDevices.value.toMutableList()
        currentPaired.removeAll { it.address == device.address }
        _pairedDevices.value = currentPaired
    }

    /**
     * Auto-connect to paired devices
     */
    fun autoConnectToPairedDevices() {
        scope.launch {
            _pairedDevices.value.forEach { device ->
                if (_connectionState.value is ConnectionState.Disconnected) {
                    connectToDevice(device)
                    delay(5000) // Wait before trying next device
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasBluetoothPermissions()) return
            
            try {
                val device = result.device
                val deviceName = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    device.name ?: "Unknown Device"
                } else {
                    "Unknown Device"
                }

                // Filter for likely panic button devices
                if (isPanicButtonDevice(deviceName, result)) {
                    val panicButton = PanicButton(
                        address = device.address,
                        name = deviceName,
                        rssi = result.rssi
                    )

                    val currentDevices = _discoveredDevices.value.toMutableList()
                    val existingIndex = currentDevices.indexOfFirst { it.address == device.address }
                    
                    if (existingIndex >= 0) {
                        currentDevices[existingIndex] = panicButton
                    } else {
                        currentDevices.add(panicButton)
                    }
                    
                    _discoveredDevices.value = currentDevices
                    Log.d(TAG, "Found panic button: $deviceName (${device.address})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan result", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            isScanning = false
            _connectionState.value = ConnectionState.Error("Scan failed (error: $errorCode)")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    if (hasBluetoothPermissions()) {
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                
                // Find panic button service
                val service = gatt.getService(PANIC_BUTTON_SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(PANIC_BUTTON_CHAR_UUID)
                    if (characteristic != null && hasBluetoothPermissions()) {
                        // Enable notifications for button press
                        gatt.setCharacteristicNotification(characteristic, true)
                        
                        val deviceName = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                            == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            gatt.device.name ?: "Panic Button"
                        } else {
                            "Panic Button"
                        }
                        
                        val connectedDevice = PanicButton(
                            address = gatt.device.address,
                            name = deviceName,
                            isConnected = true
                        )
                        _connectionState.value = ConnectionState.Connected(connectedDevice)
                        Log.d(TAG, "Panic button ready")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            // Button was pressed!
            Log.d(TAG, "Panic button pressed!")
            handlePanicButtonPress()
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // Button was pressed! (Legacy callback)
            Log.d(TAG, "Panic button pressed! (legacy)")
            handlePanicButtonPress()
        }
    }

    private fun handlePanicButtonPress() {
        scope.launch {
            Log.d(TAG, "Triggering SOS from panic button")
            sosManager.triggerSOS(TriggerType.WEARABLE_TRIGGER)
        }
    }

    private fun isPanicButtonDevice(name: String, result: ScanResult): Boolean {
        val lowerName = name.lowercase()
        
        // Check for common panic button device names
        val panicKeywords = listOf(
            "panic", "sos", "emergency", "alert", "safety",
            "itag", "tile", "nut", "key finder", "tracker",
            "button", "fob", "beacon"
        )
        
        if (panicKeywords.any { lowerName.contains(it) }) {
            return true
        }
        
        // Check for panic button service UUID in advertised services
        result.scanRecord?.serviceUuids?.forEach { uuid ->
            if (uuid.uuid == PANIC_BUTTON_SERVICE_UUID) {
                return true
            }
        }
        
        return false
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun cleanup() {
        stopScan()
        disconnect()
    }
}
