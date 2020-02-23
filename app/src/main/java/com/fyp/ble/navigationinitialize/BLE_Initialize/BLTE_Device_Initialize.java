package com.fyp.ble.navigationinitialize.BLE_Initialize;

import android.bluetooth.BluetoothDevice;

public class BLTE_Device_Initialize {
    private BluetoothDevice bluetoothDevice;
    private int rssi;

    public BLTE_Device_Initialize(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getRSSI() {
        return rssi;
    }
}
