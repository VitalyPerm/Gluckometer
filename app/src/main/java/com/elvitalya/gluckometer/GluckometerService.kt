package com.elvitalya.gluckometer

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.elvitalya.gluckometer.MainActivity.Companion.TAG
import com.elvitalya.gluckometer.MainActivity.Companion.logError
import java.util.*

@SuppressLint("MissingPermission")
class GlucometerService : Service() {

    private lateinit var bluetoothGatt: BluetoothGatt

    val bgService: UUID by lazy { UUID.fromString("00001808-0000-1000-8000-00805f9b34fb") }
    val glucoseMeasurementCharacteristic: UUID by lazy { UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb") }
    val clientCharacteristicConfig: UUID by lazy { UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") }

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
                logError(e)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "onServicesDiscovered: ")
            val service = gatt.getService(bgService) ?: return
            val glucoseCharacteristic =
                service.getCharacteristic(glucoseMeasurementCharacteristic) ?: return
            gatt.setCharacteristicNotification(glucoseCharacteristic, true)
            // This is specific to Glucose Measurement.
            if (glucoseMeasurementCharacteristic == glucoseCharacteristic.uuid) {
                val descriptor = glucoseCharacteristic.getDescriptor(clientCharacteristicConfig)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt.writeDescriptor(descriptor)
            }
        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicRead: ")
            readData(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged: ")
            readData(characteristic)
        }
    }

    private fun readData(characteristic: BluetoothGattCharacteristic) {
        if (glucoseMeasurementCharacteristic == characteristic.uuid) {
            val gtb = GlucoseReadingRx(characteristic.value)
            Log.d(TAG, "Result: $gtb")
            val result = gtb.toStringFormatted()
            Log.d(TAG, "Result formatted: $result")
        }
    }


    override fun onBind(intent: Intent?): IBinder = TonometerBinder()


    @RequiresApi(Build.VERSION_CODES.M)
    fun connectDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectDevice: connectDevice called")
        try {
            bluetoothGatt =
                device.connectGatt(this, true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            logError(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt.disconnect()
        bluetoothGatt.close()
    }

    inner class TonometerBinder : Binder() {
        val service: GlucometerService
            get() = this@GlucometerService
    }

}