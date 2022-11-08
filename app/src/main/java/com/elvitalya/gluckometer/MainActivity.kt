package com.elvitalya.gluckometer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.elvitalya.gluckometer.ui.theme.GluckometerTheme
import java.util.*


@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "check___"

        fun logError(e: Exception) {
            Log.d(TAG, "logError: ${e.message}")
        }
    }



    private val bluetoothAdapter by lazy {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager.adapter

    }

    private val mBleReceivedServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "onServiceDisconnected: ")
        }
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected: ")
            (service as GlucometerService.TonometerBinder).service.connectDevice(btDevice)
        }
    }

    private lateinit var btDevice: BluetoothDevice

    private val deviceName = "Contour"

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    ) else arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.e(TAG, "${it.key} = ${it.value}")
                val allGranted = permissions.values.find { !it } ?: true
            }
        }

    private val bleScanCallback: ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                try {
                    Log.d(TAG, "onScanResult: advertiseFlags ${result?.scanRecord?.advertiseFlags}")
                    result?.device?.let { device ->
                        device.name?.let { name ->
                            Log.d(TAG, "onScanResult: name - $name")
                            if (name.contains(deviceName)) {
                                btDevice = device
                                stopScan()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logError(e)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GluckometerTheme {
                MainScreen(
                    onClickStart = { startScan() }
                )
            }
        }
        if (checkPermissions().not()) permissionLauncher.launch(bluetoothPermissions)
    }

    private fun startScan() {
        val scanSettings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00001808-0000-1000-8000-00805f9b34fb")).build()
        try {
            if (!bluetoothAdapter.isEnabled) bluetoothAdapter.enable()
            bluetoothAdapter.bluetoothLeScanner.startScan(
                listOf(filter),
                scanSettings,
                bleScanCallback
            )
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun stopScan() {
        try {
            bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
            if (btDevice.bondState == BluetoothDevice.BOND_NONE) btDevice.createBond()
            startService()
            Log.d(TAG, "stopScan")
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun startService() {
        Log.d(TAG, "startService: ")
        bindService(
            Intent(this, GlucometerService::class.java),
            mBleReceivedServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun checkPermissions(): Boolean {
        bluetoothPermissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(
                    application,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return true
    }
}


@Composable
fun MainScreen(
    onClickStart: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Cyan.copy(alpha = 0.5f))
    ) {
        IconButton(
            onClick = onClickStart,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(30.dp)
        ) {
            Icon(imageVector = Icons.Default.AccountBox, contentDescription = null, modifier = Modifier
            .size(70.dp))
        }
    }

}