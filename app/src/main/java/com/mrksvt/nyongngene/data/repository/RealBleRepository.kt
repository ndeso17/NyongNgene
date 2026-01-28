package com.mrksvt.nyongngene.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.mrksvt.nyongngene.data.local.MessageEntity
import com.mrksvt.nyongngene.data.local.PeerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.charset.Charset
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
class RealBleRepository(
    private val context: Context,
    private val messageDao: com.mrksvt.nyongngene.data.local.MessageDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : BleRepository {

    private val notificationHelper = com.mrksvt.nyongngene.utils.NotificationHelper(context)
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val advertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    // UUIDs
    private val SERVICE_UUID = ParcelUuid(UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb"))
    private val MESSAGE_SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    private val MESSAGE_CHAR_UUID = UUID.fromString("0000b81d-0001-1000-8000-00805f9b34fb")

    private val _nearbyPeers = MutableStateFlow<List<PeerEntity>>(emptyList())
    override val nearbyPeers: StateFlow<List<PeerEntity>> = _nearbyPeers
    
    override val activeChannels: kotlinx.coroutines.flow.Flow<List<com.mrksvt.nyongngene.data.local.ChannelProfile>> = messageDao.getChannelProfiles()

    override val messages: kotlinx.coroutines.flow.Flow<List<MessageEntity>> = messageDao.getAllMessages()

    private val foundDevicesMap = mutableMapOf<String, PeerEntity>()
    
    // GATT Server properties
    private var gattServer: BluetoothGattServer? = null
    
    // Store discovered devices map to connect to
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    init {
        // Auto-start if possible
        start()
    }

    override fun start() {
        if (hasPermissions()) {
            setupGattServer()
            startScanning()
            startAdvertising()
        }
    }

    private fun hasPermissions(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                   context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                   context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
             return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setupGattServer() {
        if (gattServer != null) return // Already setup
        
        val gattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                val devName = device.name ?: device.address
                println("GATT Server connection: $devName, status=$status, state=$newState")
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
                
                if (characteristic.uuid == MESSAGE_CHAR_UUID) {
                    val rawContent = String(value, Charset.forName("UTF-8"))
                    println("GATT Server received RAW: '$rawContent' from ${device.address}")
                    
                    var content = rawContent
                    var isBroadcast = false
                    var channelId = device.address
                    var finalSenderName = device.name ?: device.address
                    
                    if (rawContent.startsWith("BRD:")) {
                        isBroadcast = true
                        channelId = "BROADCAST"
                        content = rawContent.removePrefix("BRD:")
                        println("GATT: Parsed as BROADCAST, content='$content'")
                    } else if (rawContent.startsWith("PRV:")) {
                        isBroadcast = false
                        // Format: PRV:SenderName|Message
                        val rest = rawContent.removePrefix("PRV:")
                        println("GATT: PRV rest='$rest', contains |: ${rest.contains("|")}")
                        if (rest.contains("|")) {
                            val split = rest.split("|", limit = 2)
                            finalSenderName = split[0]  // Use name from protocol!
                            content = split[1]
                            println("GATT: Parsed senderName='$finalSenderName', content='$content'")
                        } else {
                            // Legacy format fallback
                            content = rest
                            println("GATT: Legacy format, content='$content'")
                        }
                        
                        // Use sender NAME as channelId for consistency with sender side
                        channelId = finalSenderName
                        println("GATT: Using sender name as channelId='$finalSenderName'")
                    }

                    println("GATT: Final -> channelId='$channelId', senderName='$finalSenderName', content='$content'")
                    
                    val msgEntity = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = device.address,
                        senderName = finalSenderName,  // Fixed: use parsed name
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        isSent = false,
                        isBroadcast = isBroadcast,
                        channelId = channelId
                    )
                    
                    externalScope.launch {
                        messageDao.insertMessage(msgEntity)
                        notificationHelper.showNotification("Message from $finalSenderName", content, channelId)
                    }

                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    }
                }
            }
        }

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        
        val service = BluetoothGattService(MESSAGE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            MESSAGE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristic)
        
        gattServer?.addService(service)
    }

    private fun startScanning() {
        if (!hasPermissions()) return
        
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(filters, settings, scanCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.device
                val deviceName = device.name ?: device.address
                val address = device.address
                
                // Track device for connection
                deviceMap[address] = device
                deviceMap[deviceName] = device // Fallback map by name
                
                val peer = PeerEntity(
                    address = address,
                    name = device.name ?: "Unknown ($address)",
                    rssi = result.rssi,
                    lastSeen = System.currentTimeMillis()
                )
                
                foundDevicesMap[address] = peer
                _nearbyPeers.value = foundDevicesMap.values.toList().sortedByDescending { it.lastSeen }
            } catch (e: SecurityException) {
            }
        }
    }

    private fun startAdvertising() {
        if (!hasPermissions()) return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(SERVICE_UUID)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            println("BLE Advertise Started")
        }

        override fun onStartFailure(errorCode: Int) {
            println("BLE Advertise Failed: $errorCode")
        }
    }

    override suspend fun sendMessage(to: String, message: String, isBroadcast: Boolean): Long {
        val device = deviceMap[to] ?: bluetoothAdapter?.getRemoteDevice(to)
        
        if (device == null) {
            throw Exception("Device not found: $to")
        }

        val myName = bluetoothAdapter?.name ?: "Unknown"
        val prefix = if (isBroadcast) "BRD:" else "PRV:$myName|"
        val payload = "$prefix$message"
        
        val startTime = System.currentTimeMillis()

        return suspendCancellableCoroutine { continuation ->
            var isResumed = false
            var mtuRequested = false
            
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        try {
                            // Request larger MTU first to avoid truncation
                            println("GATT: Connected, requesting MTU 512")
                            mtuRequested = gatt.requestMtu(512)
                            if (!mtuRequested) {
                                // Fallback: if MTU request fails, discover services directly
                                gatt.discoverServices()
                            }
                        } catch (e: SecurityException) {
                            if (!isResumed) { isResumed = true; continuation.resumeWithException(e) }
                            gatt.close()
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (!isResumed) {
                            isResumed = true
                            continuation.resumeWithException(Exception("Disconnected unexpectedly"))
                        }
                        gatt.close()
                    }
                }
                
                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    super.onMtuChanged(gatt, mtu, status)
                    println("GATT: MTU changed to $mtu, status=$status")
                    // Now discover services with larger MTU
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        if (!isResumed) { isResumed = true; continuation.resumeWithException(e) }
                        gatt.close()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(MESSAGE_SERVICE_UUID)
                        val characteristic = service?.getCharacteristic(MESSAGE_CHAR_UUID)
                        
                        if (characteristic != null) {
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            characteristic.value = payload.toByteArray(Charset.forName("UTF-8"))
                            println("GATT: Writing payload (${payload.length} bytes): $payload")
                            try {
                                val writeResult = gatt.writeCharacteristic(characteristic)
                                println("GATT writeCharacteristic initiated: $writeResult")
                            } catch (e: SecurityException) {
                                if (!isResumed) { isResumed = true; continuation.resumeWithException(e) }
                                gatt.disconnect()
                            }
                        } else {
                            if (!isResumed) { isResumed = true; continuation.resumeWithException(Exception("Service not found")) }
                            gatt.disconnect()
                        }
                    } else {
                        if (!isResumed) { isResumed = true; continuation.resumeWithException(Exception("Service discovery failed")) }
                        gatt.disconnect()
                    }
                }
                
                override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    val latency = System.currentTimeMillis() - startTime
                    println("GATT onCharacteristicWrite: status=$status, latency=${latency}ms")
                    if (!isResumed) {
                        isResumed = true
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            continuation.resume(latency)
                        } else {
                            continuation.resumeWithException(Exception("GATT write failed with status: $status"))
                        }
                    }
                    gatt.disconnect()
                }
            }

            try {
                device.connectGatt(context, false, gattCallback)
            } catch (e: SecurityException) {
                 if (!isResumed) { isResumed = true; continuation.resumeWithException(e) }
            }
        }
    }

    override suspend fun saveMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }
    
    override fun refreshPeers() {
        if (!hasPermissions()) return
        
        try {
            // Stop current scan
            scanner?.stopScan(scanCallback)
            
            // Clear old peers (keep only history ones with rssi=0 for later)
            foundDevicesMap.clear()
            _nearbyPeers.value = emptyList()
            
            // Restart scanning
            startScanning()
            println("BLE: Refreshed peer list, restarted scan")
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
