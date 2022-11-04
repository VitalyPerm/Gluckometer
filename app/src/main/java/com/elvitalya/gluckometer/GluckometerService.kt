package com.elvitalya.gluckometer

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.elvitalya.gluckometer.MainActivity.Companion.TAG
import com.elvitalya.gluckometer.MainActivity.Companion.logError
import java.util.*

@SuppressLint("MissingPermission")
class GlucometerService : Service() {

    private lateinit var bluetoothGatt: BluetoothGatt

    val glucoseMeasurementCharacteristicCBUUID by lazy { convertFromInteger(0x2A18) }
    val glucoseServiceCBUUID by lazy { convertFromInteger(0x1808) }
    val glucoseMeasurementContextCharacteristicCBUUID by lazy { convertFromInteger(0x2A34) }
    val glucoseFeatureCharacteristicCBUUID by lazy { convertFromInteger(0x2A51) }
    val recordAccessControlPointCharacteristicCBUUID by lazy { convertFromInteger(0x2A52) }


    // new
    val bgService = "00001808-0000-1000-8000-00805f9b34fb"
    val glucoseMeasurementCharacteristic = "00002a18-0000-1000-8000-00805f9b34fb"
    val bgMeasurement = "00002a18-0000-1000-8000-00805f9b34fb"
    val clientCharacteristicConfig = "00002902-0000-1000-8000-00805f9b34fb"

    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
    ) {
        bluetoothGatt.setCharacteristicNotification(characteristic, true)

        // This is specific to Glucose Measurement.
        if (UUID.fromString(bgMeasurement) == characteristic.uuid) {
            val descriptor =
                characteristic.getDescriptor(UUID.fromString(clientCharacteristicConfig))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt.writeDescriptor(descriptor)
        }
    }

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bluetoothGatt.close()
                    bluetoothGatt.disconnect()
                }
            } catch (e: Exception) {
                MainActivity.logError(e)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "onServicesDiscovered: ")
            val service = gatt.getService(UUID.fromString(bgService))
            if (service != null) {
                val glucoseCharacteristic =
                    service.getCharacteristic(UUID.fromString(glucoseMeasurementCharacteristic))
                setCharacteristicNotification(glucoseCharacteristic)
            }
        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            readData(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            readData(characteristic)
        }
    }

    private fun readData(characteristic: BluetoothGattCharacteristic) {
        if (UUID.fromString(bgMeasurement) == characteristic.uuid) {
            val gtb = GlucoseReadingRx(characteristic.value)
            Log.d(TAG, "Result: $gtb")
            val result = gtb.toStringFormatted()
            Log.d(TAG, "Result formatted: $result")
        } else {
            // For all other profiles, writes the data formatted in HEX.
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
                Log.d(TAG, String.format("Received", stringBuilder))
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder = TonometerBinder()


    fun connectDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectDevice: connectDevice called")
        try {
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun uuidFromShortString(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    private fun convertFromInteger(i: Int): UUID {
        val msb = 0x0000000000001000L
        val lsb = -0x7fffff7fa064cb05L
        val value = (i and -0x1).toLong()
        return UUID(msb or (value shl 32), lsb)
    }

    inner class TonometerBinder : Binder() {
        val service: GlucometerService
            get() = this@GlucometerService
    }

}