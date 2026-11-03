package com.example.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.example.data.model.BleScalePacketParser
import com.example.data.model.DiscoveredBleScale
import com.example.data.model.ScaleBrandProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    VIRTUAL_SIMULATOR
}

class BleScaleClient(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var activeGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleScale>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleScale>> = _discoveredDevices.asStateFlow()

    private val _connectedScaleName = MutableStateFlow<String?>(null)
    val connectedScaleName: StateFlow<String?> = _connectedScaleName.asStateFlow()

    private val _selectedProtocol = MutableStateFlow(ScaleBrandProtocol.FUTULA_KOFEZAVR)
    val selectedProtocol: StateFlow<ScaleBrandProtocol> = _selectedProtocol.asStateFlow()

    private val _rawWeightGrams = MutableStateFlow(0.0f)
    val rawWeightGrams: StateFlow<Float> = _rawWeightGrams.asStateFlow()

    private val _statusMessage = MutableStateFlow("Готов к поиску Bluetooth-весов")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var softwareTareOffset: Float = 0.0f

    companion object {
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun selectProtocol(protocol: ScaleBrandProtocol) {
        _selectedProtocol.value = protocol
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _statusMessage.value = "Bluetooth отключён или недоступен на устройстве. Включите Bluetooth или запустите «Виртуальные весы»."
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _statusMessage.value = "BLE-сканер недоступен. Используйте режим «Виртуальные весы»."
            return
        }

        try {
            _discoveredDevices.value = emptyList()
            _connectionState.value = BleConnectionState.SCANNING
            _statusMessage.value = "Поиск умных кофейных весов поблизости (BLE)..."

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            _statusMessage.value = "Нет разрешения на Bluetooth-сканирование"
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            _statusMessage.value = "Ошибка запуска сканера: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
                _statusMessage.value = "Поиск завершён (найдено: ${_discoveredDevices.value.size})"
            }
        } catch (_: Exception) {
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName ?: return
            if (name.isBlank()) return

            val address = device.address ?: return
            val rssi = result.rssi
            val detected = ScaleBrandProtocol.detectFromDeviceName(name)

            _discoveredDevices.update { current ->
                val existingIdx = current.indexOfFirst { it.address == address }
                val item = DiscoveredBleScale(name, address, rssi, detected)
                if (existingIdx >= 0) {
                    current.toMutableList().apply { set(existingIdx, item) }
                } else {
                    current + item
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            _statusMessage.value = "Ошибка BLE сканирования (код $errorCode)"
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(scale: DiscoveredBleScale) {
        stopScan()
        val adapter = bluetoothAdapter ?: return
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(scale.address)
            _selectedProtocol.value = scale.detectedProtocol
            _connectionState.value = BleConnectionState.CONNECTING
            _statusMessage.value = "Подключение к ${scale.name} (${scale.detectedProtocol.title})..."
            activeGatt?.close()
            activeGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            _statusMessage.value = "Не удалось подключиться: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {
        }
        activeGatt = null
        writeCharacteristic = null
        _connectedScaleName.value = null
        _connectionState.value = BleConnectionState.DISCONNECTED
        _statusMessage.value = "Отключено от весов"
    }

    fun activateVirtualSimulator() {
        stopScan()
        try {
            activeGatt?.close()
        } catch (_: Exception) {
        }
        activeGatt = null
        softwareTareOffset = 0f
        _rawWeightGrams.value = 0f
        _connectedScaleName.value = "BeanZen Virtual Scale (${_selectedProtocol.value.title})"
        _connectionState.value = BleConnectionState.VIRTUAL_SIMULATOR
        _statusMessage.value = "Подключено: Виртуальные весы (${_selectedProtocol.value.title})"
    }

    fun updateVirtualWeight(grams: Float) {
        _rawWeightGrams.value = grams
    }

    @SuppressLint("MissingPermission")
    fun sendTare() {
        val gatt = activeGatt
        val writeChar = writeCharacteristic
        val cmd = _selectedProtocol.value.tareCommand

        if (_connectionState.value == BleConnectionState.CONNECTED && gatt != null && writeChar != null && cmd != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(writeChar, cmd, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    writeChar.value = cmd
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(writeChar)
                }
                _statusMessage.value = "Команда TARE (0.0 г) отправлена на весы"
                return
            } catch (_: Exception) {
            }
        }

        // Software tare fallback (or virtual scale tare)
        softwareTareOffset = _rawWeightGrams.value + softwareTareOffset
        _rawWeightGrams.value = 0.0f
        _statusMessage.value = "Тарирование выполнено: 0.0 г"
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedScaleName.value = gatt.device.name ?: gatt.device.address
                    _connectionState.value = BleConnectionState.CONNECTED
                    _statusMessage.value = "Подключено к ${_connectedScaleName.value}. Поиск сервисов..."
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectedScaleName.value = null
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _statusMessage.value = "Соединение с весами разорвано"
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val protocol = _selectedProtocol.value
            val targetServiceUuid = runCatching { UUID.fromString(protocol.serviceUuid) }.getOrNull()
            val targetNotifyUuid = runCatching { UUID.fromString(protocol.notifyCharUuid) }.getOrNull()
            val targetWriteUuid = protocol.writeCharUuid?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            var subscribed = false

            // 1. Try protocol-specific service & characteristic first
            if (targetServiceUuid != null && targetNotifyUuid != null) {
                val service = gatt.getService(targetServiceUuid)
                val notifyChar = service?.getCharacteristic(targetNotifyUuid)
                if (notifyChar != null) {
                    enableNotifications(gatt, notifyChar)
                    subscribed = true
                }
                if (targetWriteUuid != null) {
                    writeCharacteristic = service?.getCharacteristic(targetWriteUuid)
                }
            }

            // 2. Fallback: scan all GATT characteristics with PROPERTY_NOTIFY or PROPERTY_INDICATE
            if (!subscribed) {
                for (service in gatt.services) {
                    for (characteristic in service.characteristics) {
                        val props = characteristic.properties
                        if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                            (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                        ) {
                            enableNotifications(gatt, characteristic)
                            subscribed = true
                        }
                        if (writeCharacteristic == null &&
                            ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
                        ) {
                            writeCharacteristic = characteristic
                        }
                    }
                }
            }

            _statusMessage.value = if (subscribed) {
                "Поток веса активен (${protocol.title})"
            } else {
                "Подключено, ожидание данных веса..."
            }
        }

        @SuppressLint("MissingPermission")
        private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePacket(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handlePacket(value)
        }

        private fun handlePacket(bytes: ByteArray) {
            val parsed = BleScalePacketParser.parseWeightGrams(bytes, _selectedProtocol.value)
            if (parsed != null) {
                _rawWeightGrams.value = parsed - softwareTareOffset
            }
        }
    }
}
