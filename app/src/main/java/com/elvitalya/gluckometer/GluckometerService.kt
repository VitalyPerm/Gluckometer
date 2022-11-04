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
            requestReadFirmRevision()
        }



        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicRead: ${characteristic.uuid}")

            val bytes = characteristic.value
            Log.d(TAG, "onCharacteristicRead: bytes ${bytes.joinToString()}")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            val serviceUuidString = characteristic.service.uuid.toString()
            val characteristicUuidString = characteristic.uuid.toString()
            Log.d(TAG, "onCharacteristicWrite: serviceUuidString $serviceUuidString")
            Log.d(TAG, "onCharacteristicWrite: characteristicUuidString $characteristicUuidString")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged: ")

        }
    }

    fun requestReadFirmRevision() {
        Log.d(TAG, "requestReadFirmRevision: ")
        val service = bluetoothGatt.getService(glucoseServiceCBUUID)
        Log.d(TAG, "requestReadFirmRevision: service is not null ${service != null}")
        val characteristicList = mutableListOf<BluetoothGattCharacteristic>()
        service?.let { gattSerivce ->
            gattSerivce.getCharacteristic(glucoseMeasurementCharacteristicCBUUID)?.let {
                Log.d(
                    TAG,
                    "add characteristic  glucoseMeasurementCharacteristicCBUUID.add(${it.uuid})"
                )
                characteristicList.add(it)
            }
            gattSerivce.getCharacteristic(glucoseServiceCBUUID)?.let {
                Log.d(TAG, "add characteristic  glucoseServiceCBUUID.add(${it.uuid})")
                characteristicList.add(it)
            }
            gattSerivce.getCharacteristic(glucoseMeasurementContextCharacteristicCBUUID)?.let {
                Log.d(
                    TAG,
                    "add characteristic  glucoseMeasurementContextCharacteristicCBUUID.add(${it.uuid})"
                )
                characteristicList.add(it)
            }
            gattSerivce.getCharacteristic(glucoseFeatureCharacteristicCBUUID)?.let {
                Log.d(
                    TAG,
                    "add characteristic  glucoseFeatureCharacteristicCBUUID.add(${it.uuid})"
                )
                characteristicList.add(it)
            }
            gattSerivce.getCharacteristic(recordAccessControlPointCharacteristicCBUUID)?.let {
                Log.d(
                    TAG,
                    "add characteristic  recordAccessControlPointCharacteristicCBUUID.add(${it.uuid})"
                )
                characteristicList.add(it)
            }
        }

        characteristicList.forEach { characteristic ->
            try {
                bluetoothGatt.readCharacteristic(characteristic)
            } catch (e: Exception) {
                logError(e)
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