package com.mrksvt.nyongngene.data.repository

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class RealLoRaRepository(
    private val context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : LoRaRepository, SerialInputOutputManager.Listener {

    private val ACTION_USB_PERMISSION = "com.mrksvt.nyongngene.USB_PERMISSION"
    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    // Mock SNR for now, real implementation needs parsing from LoRa packet
    private val _lastPacketSnr = MutableStateFlow(0)
    override val lastPacketSnr: StateFlow<Int> = _lastPacketSnr

    init {
        registerUsbReceiver()
        scanAndConnect()
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
             context.registerReceiver(usbReceiver, filter)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> scanAndConnect()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> disconnect()
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            scanAndConnect()
                        }
                    }
                }
            }
        }
    }

    private fun scanAndConnect() {
        if (_isConnected.value) return

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        
        if (availableDrivers.isEmpty()) return

        val driver = availableDrivers[0]
        val device = driver.device
        
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
            return
        }

        val connection = usbManager.openDevice(device)
        if (connection == null) return

        try {
            usbSerialPort = driver.ports[0]
            usbSerialPort?.open(connection)
            usbSerialPort?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            usbIoManager = SerialInputOutputManager(usbSerialPort, this)
            usbIoManager?.start()

            _isConnected.value = true
        } catch (e: IOException) {
            disconnect()
        }
    }

    private fun disconnect() {
        usbIoManager?.stop()
        usbIoManager = null
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            // Ignore
        }
        usbSerialPort = null
        _isConnected.value = false
    }

    override fun sendBroadcast(data: ByteArray) {
        if (!_isConnected.value) return
        try {
            // AT+SEND command format logic should be here
            // For now, raw write
            usbSerialPort?.write(data, 1000)
        } catch (e: IOException) {
            _isConnected.value = false
        }
    }

    override fun onNewData(data: ByteArray?) {
        data?.let {
            // Parse LoRa packet here
            // Update SNR, etc.
            val str = String(it)
            // println("LoRa RECV: $str")
        }
    }

    override fun onRunError(e: Exception?) {
        disconnect()
    }
}
