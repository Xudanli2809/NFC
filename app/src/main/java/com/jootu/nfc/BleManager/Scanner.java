package com.jootu.nfc.BleManager;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/7/19.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Scanner {

    private BluetoothAdapter mBAdapter = BluetoothAdapter.getDefaultAdapter();
    public static List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    private boolean mScanning;
    public onReceiveScannerListener mOnReceiveScannerListener;

    private Context mContext;
    private ScannerCallback mScannerCallback = null;

    public interface onReceiveScannerListener{
        void onReceiveScanDevice(final BluetoothDevice device, final int rssi, final byte[] scanRecord);
    }

    public void setOnReceiveScannerListener(onReceiveScannerListener listener) {
        mOnReceiveScannerListener = listener;
    }

    /*初始化Scanner类，callback为搜索回调函数，调用了ScannerCallback类*/
    public Scanner(Context context, ScannerCallback callback) {
        mContext = context;
        mScannerCallback = callback;
        initialize();
    }

    public void setScannerCallback(ScannerCallback scannerCallback) {
        this.mScannerCallback = scannerCallback;
    }

    /*蓝牙BLE开始搜索，调用此函数后会一直搜索蓝牙直到搜到设备为止。*/
    public void startScan() {
        deviceList.clear();
        scanLeDevice(true, 0);
    }

    public void startScan(onReceiveScannerListener listener) {
        mOnReceiveScannerListener = listener;
        startScan();
    }

    /*蓝牙BLE开始搜索，带超时参数，scanPeriod参数表搜索多少秒后停止搜索。*/
    public void startScan(long scanPeriod) {
        scanLeDevice(true, scanPeriod);
    }

    /*蓝牙BLE停止搜索。*/
    public void stopScan() {
        scanLeDevice(false, 0);
        if (mScannerCallback != null) {
            mScannerCallback.onScanDeviceStopped();
        }
    }


    /*获取蓝牙BLE搜索的状态，如果正在搜索蓝牙则返回 true，否则返回 false*/
    public boolean isScanning() {
        return mScanning;
    }

    // Device scan callback.设备搜索回调
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //搜到设备回调
            deviceList.add(device);
            mScannerCallback.onReceiveScanDevice(device, rssi, scanRecord);
            if (mOnReceiveScannerListener != null) {
                mOnReceiveScannerListener.onReceiveScanDevice(device, rssi, scanRecord);
            }
        }
    };

    private boolean initialize() {
        //检测手机是否支持BLE
        if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(mContext, "此手机不支持BLE", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 判断蓝牙是否打开
        if ((mBAdapter != null) && !mBAdapter.isEnabled()) {
            mBAdapter.enable();
        }

        return true;
    }

    private Handler mHandler = new Handler();
    //搜索BLE设备
    private void scanLeDevice(final boolean enable, final long scanPeriod) {
        if (enable) {
            if (scanPeriod > 0) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mScanning) {
                            mScanning = false;
                            mBAdapter.stopLeScan(mLeScanCallback);
                            mBAdapter.cancelDiscovery();
                            //停止搜索回调
                            if (mScannerCallback != null) {
                                mScannerCallback.onScanDeviceStopped();
                            }
                        }
                    }
                }, scanPeriod);
            }

            mScanning = true;
            mBAdapter.startLeScan(mLeScanCallback);
            //mBAdapter.startLeScan(null);
        } else {
            mScanning = false;
            mBAdapter.stopLeScan(mLeScanCallback);
        }
    }

    //获取本次搜索到的蓝牙设备列表
    public List<BluetoothDevice> getDeviceList() {
        return deviceList;
    }

    //获取本次搜索到的蓝牙设备的名称
    public String[] getDeviceNames() {
        if (deviceList == null) {
            return null;
        }
        String[] strings = new String[deviceList.size()];
        for (int i=0; i<deviceList.size(); i++) {
            strings[i] = deviceList.get(i).getName();
        }

        return strings;
    }

}
