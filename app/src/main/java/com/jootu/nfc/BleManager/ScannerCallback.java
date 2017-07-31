package com.jootu.nfc.BleManager;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Administrator on 2017/7/19.
 */

public class ScannerCallback {

    public void onReceiveScanDevice(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
    }

    public void onScanDeviceStopped() {
    }
}
