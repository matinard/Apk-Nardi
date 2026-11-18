package com.nardiobd.app.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdBluetoothManager {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    suspend fun connectToPlxDevice(deviceNameFilter: String = "PLXDevices"): Boolean = withContext(Dispatchers.IO) {
        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val targetDevice = pairedDevices?.find { it.name.contains(deviceNameFilter, ignoreCase = true) }
                ?: return@withContext false

            bluetoothSocket = targetDevice.createRfcommSocketToServiceRecord(sppUuid)
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()

            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            sendCommand("AT Z")
            sendCommand("AT E0")
            sendCommand("AT L0")
            sendCommand("AT SP 0")
            true
        } catch (e: Exception) {
            Log.e("NardiOBD", "Error conectando al OBD: ${e.message}")
            false
        }
    }

    suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(cmd.toByteArray())
            outputStream?.flush()

            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer) ?: 0
            return@withContext String(buffer, 0, bytes)
        } catch (e: Exception) {
            "ERROR"
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {}
    }
}
