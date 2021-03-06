package com.jootu.nfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jootu.nfc.BleManager.*;
import com.jootu.nfc.BleManager.BleManager;
import com.jootu.nfc.BleManager.ScannerCallback;
/*import com.jootu.nfc.BleNfcDeviceService;*/
import com.jootu.nfc.DeviceManager.BleNfcDevice;
import com.jootu.nfc.DeviceManager.ComByteManager;
import com.jootu.nfc.DeviceManager.DeviceManager;
import com.jootu.nfc.DeviceManager.DeviceManagerCallback;
import com.jootu.nfc.Exception.DeviceNoResponseException;

import com.jootu.nfc.Tool.StringTool;
import com.jootu.nfc.Card.Mifare;
import com.jootu.nfc.Util.HttpUtil;
import com.jootu.nfc.Util.Utility;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

//读取nfc卡信息界面
public class MainActivity extends Activity {
    BleNfcDeviceService mBleNfcDeviceService;
    private BleNfcDevice bleNfcDevice;
    private Scanner mScanner;
    private Button searchButton = null;
    private EditText msgText = null;
    private ProgressDialog readWriteDialog = null;
    private AlertDialog.Builder alertDialog = null;
    private StringBuffer msgBuffer;
    private BluetoothDevice mNearestBle = null;
    private Lock mNearestBleLock = new ReentrantLock();// 锁对象
    private int lastRssi = -100;
    private CharSequence[] items = null;
    private Button user;
    private TextView id;
    private int userid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userid=getIntent().getIntExtra("userid",userid);
        Toast.makeText(MainActivity.this,"userid:"+userid,Toast.LENGTH_SHORT).show();

        msgBuffer = new StringBuffer();

        user=(Button)findViewById(R.id.user);

        searchButton = (Button)findViewById(R.id.searchButton);
        Button sendButton = (Button) findViewById(R.id.sendButton);
        Button changeBleNameButton = (Button) findViewById(R.id.changeBleNameButton);
        msgText = (EditText)findViewById(R.id.msgText);
        Button clearButton = (Button) findViewById(R.id.clearButton);
        Button openBeepButton = (Button) findViewById(R.id.openBeepButton);
        Button closeBeepButton = (Button) findViewById(R.id.closeBeepButton);
        Button openAntiLostButton = (Button) findViewById(R.id.openAntiLostButton);
        Button closeAntiLostButton = (Button) findViewById(R.id.closeAntiLostButton);
        Button openAutoSearchCard = (Button)findViewById(R.id.openAutoSearchCard);
        Button closeAutoSearchCard = (Button)findViewById(R.id.closeAutoSearchCard);


        ////使背景图和状态栏融合到一起，这个功能只有API21(android 5.0)及以上版本才支持
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();//拿到当前活动的DecorView
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);//表示当前活动的布局会显示在状态栏上面
            getWindow().setStatusBarColor(Color.TRANSPARENT);//将状态栏设置成透明色
        }


        readWriteDialog = new ProgressDialog(MainActivity.this);
        readWriteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // 设置ProgressDialog 标题
        readWriteDialog.setTitle("请稍等");
        // 设置ProgressDialog 提示信息
        readWriteDialog.setMessage("正在读写数据……");
        // 设置ProgressDialog 标题图标
        readWriteDialog.setIcon(R.drawable.ic_launcher);

        clearButton.setOnClickListener(new claerButtonListener());
        searchButton.setOnClickListener(new StartSearchButtonListener());
        sendButton.setOnClickListener(new SendButtonListener());
        changeBleNameButton.setOnClickListener(new changeBleNameButtonListener());
        openBeepButton.setOnClickListener(new OpenBeepButtonListener());
        closeBeepButton.setOnClickListener(new closeBeepButtonListener());
        openAntiLostButton.setOnClickListener(new OpenAntiLostButtonListener());
        closeAntiLostButton.setOnClickListener(new CloseAntiLostButtonListener());
        openAutoSearchCard.setOnClickListener(new OpenAutoSearchCardButtonListener());
        closeAutoSearchCard.setOnClickListener(new CloseAutoSearchCardButtonListener());



        //ble_nfc服务初始化
        Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        msgText.setText("Jootu_NFC v1.0.0");



        user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,Login.class);
                startActivity(intent);
            }
        });
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleNfcDeviceService mBleNfcDeviceService = ((BleNfcDeviceService.LocalBinder) service).getService();
            bleNfcDevice = mBleNfcDeviceService.bleNfcDevice;
            mScanner = mBleNfcDeviceService.scanner;
            mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
            mBleNfcDeviceService.setScannerCallback(scannerCallback);

            //开始搜索设备
            searchNearestBleDevice();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleNfcDeviceService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (mBleNfcDeviceService != null) {
            mBleNfcDeviceService.setScannerCallback(scannerCallback);
            mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (readWriteDialog != null) {
            readWriteDialog.dismiss();
        }
        unbindService(mServiceConnection);
    }

    //Scanner 回调
    private ScannerCallback scannerCallback = new ScannerCallback() {
        @Override
        public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            super.onReceiveScanDevice(device, rssi, scanRecord);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //StringTool.byteHexToSting(scanRecord.getBytes())
                System.out.println("Activity搜到设备：" + device.getName()
                        + " 信号强度：" + rssi
                        + " scanRecord：" + StringTool.byteHexToSting(scanRecord));
            }
            //搜索蓝牙设备并记录信号强度最强的设备
            if ( (scanRecord != null) && (StringTool.byteHexToSting(scanRecord).contains("017f5450"))) {  //从广播数据中过滤掉其它蓝牙设备
                msgBuffer.append("搜到设备：").append(device.getName()).append(" 信号强度：").append(rssi).append("\r\n");
                handler.sendEmptyMessage(0);
                if (mNearestBle != null) {
                    if (rssi > lastRssi) {
                        mNearestBleLock.lock();
                        try {
                            mNearestBle = device;
                        }finally {
                            mNearestBleLock.unlock();
                        }
                    }
                }
                else {
                    mNearestBleLock.lock();
                    try {
                        mNearestBle = device;
                    }finally {
                        mNearestBleLock.unlock();
                    }
                    lastRssi = rssi;
                }
            }
        }
        @Override
        public void onScanDeviceStopped() {
            super.onScanDeviceStopped();
        }
    };

    //设备操作类回调
    private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback() {
        @Override
        public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
            super.onReceiveConnectBtDevice(blnIsConnectSuc);
            if (blnIsConnectSuc) {
                System.out.println("Activity设备连接成功");
                msgBuffer.delete(0, msgBuffer.length());
                msgBuffer.append("设备连接成功!\r\n");
                if (mNearestBle != null) {
                    msgBuffer.append("设备名称：").append(bleNfcDevice.getDeviceName()).append("\r\n");
                }
                msgBuffer.append("信号强度：").append(lastRssi).append("dB\r\n");
                msgBuffer.append("SDK版本：" + BleNfcDevice.SDK_VERSIONS + "\r\n");

                //连接上后延时500ms后再开始发指令
                try {
                    Thread.sleep(500L);
                    handler.sendEmptyMessage(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
            super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
            System.out.println("Activity设备断开链接");
            msgBuffer.delete(0, msgBuffer.length());
            msgBuffer.append("设备断开链接!");
            handler.sendEmptyMessage(0);
        }

        @Override
        public void onReceiveConnectionStatus(boolean blnIsConnection) {
            super.onReceiveConnectionStatus(blnIsConnection);
            System.out.println("Activity设备链接状态回调");
        }

        @Override
        public void onReceiveInitCiphy(boolean blnIsInitSuc) {
            super.onReceiveInitCiphy(blnIsInitSuc);
        }

        @Override
        public void onReceiveDeviceAuth(byte[] authData) {
            super.onReceiveDeviceAuth(authData);
        }

        @Override
        //寻到卡片回调
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
            super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
            final Mifare mifare = (Mifare) bleNfcDevice.getCard();

            if (!blnIsSus || cardType == BleNfcDevice.CARD_TYPE_NO_DEFINE) {
                return;
            }
            /*System.out.println("Activity寻到Mifare卡回调：UID->" + StringTool.byteHexToSting(bytCardSn) + " ATS->" + StringTool.byteHexToSting(bytCarATS));*/

            if (DeviceManager.CARD_TYPE_MIFARE == cardType) {  //如果是Mifare卡，则提取UID ：bytCardSn表示UID

                System.out.println("Activity寻到Mifare卡回调：UID->" + StringTool.byteHexToSting(bytCardSn) + " ATS->" + StringTool.byteHexToSting(bytCarATS));


            }
            final int cardTypeTemp = cardType;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isReadWriteCardSuc;
                    try {
                        if (bleNfcDevice.isAutoSearchCard()) {
                            //如果是自动寻卡的，寻到卡后，先关闭自动寻卡
                            bleNfcDevice.stoptAutoSearchCard();
                            Intent intent=new Intent(MainActivity.this,Detail.class);
                           /* intent.putExtra("uid",(mifare.uidToString()));*/
                            Bundle bd=new Bundle();
                            bd.putInt("user_id",userid);
                            bd.putString("uid",(mifare.uidToString()));
                            intent.putExtras(bd);
                            startActivity(intent);
                            isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

                            //读卡结束，重新打开自动寻卡
                            startAutoSearchCard();
                        }
                        else {
                            isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

                            //如果不是自动寻卡，读卡结束,关闭天线
                            bleNfcDevice.closeRf();
                        }

                        //打开蜂鸣器提示读卡完成
                        if (isReadWriteCardSuc) {
                            bleNfcDevice.openBeep(50, 50, 3);  //读写卡成功快响3声
                        }
                        else {
                            bleNfcDevice.openBeep(100, 100, 2); //读写卡失败慢响2声
                        }
                    } catch (DeviceNoResponseException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
            super.onReceiveRfmSentApduCmd(bytApduRtnData);

            System.out.println("Activity接收到APDU回调：" + StringTool.byteHexToSting(bytApduRtnData));
        }

        @Override
        public void onReceiveRfmClose(boolean blnIsCloseSuc) {
            super.onReceiveRfmClose(blnIsCloseSuc);
        }

        @Override
        //按键返回回调
        public void onReceiveButtonEnter(byte keyValue) {
            if (keyValue == DeviceManager.BUTTON_VALUE_SHORT_ENTER) { //按键短按
                System.out.println("Activity接收到按键短按回调");
                msgBuffer.append("按键短按\r\n");
                handler.sendEmptyMessage(0);
            }
            else if (keyValue == DeviceManager.BUTTON_VALUE_LONG_ENTER) { //按键长按
                System.out.println("Activity接收到按键长按回调");
                msgBuffer.append("按键长按\r\n");
                handler.sendEmptyMessage(0);
            }
        }
    };

    //搜索按键监听
    private class StartSearchButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) ) {
                bleNfcDevice.requestDisConnectDevice();
                return;
            }

            searchNearestBleDevice();
//            if (!mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
//                //开始搜索设备
//                msgBuffer.delete(0, msgBuffer.length());
//                msgBuffer.append("正在搜索设备...");
//                handler.sendEmptyMessage(7);
//                mScanner.startScan(new Scanner.onReceiveScannerListener() {
//                    @Override
//                    public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
//                        if ( (scanRecord != null) && (StringTool.byteHexToSting(scanRecord).contains("017f5450"))) {  //从广播数据中过滤掉其它蓝牙设备
//                            msgBuffer.delete(0, msgBuffer.length());
//                            handler.sendEmptyMessage(7);
//                        }
//                    }
//                });
//            }
        }
    }

    //读卡按键监听
    public class SendButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            //寻卡一次
            bleNfcDevice.requestRfmSearchCard(ComByteManager.ISO14443_P4);
        }
    }

    //修改蓝牙名称按键监听
    private class changeBleNameButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }

            final EditText inputEditText = new EditText(MainActivity.this);
            //inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            //inputEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("修改蓝牙名称")
                    .setMessage("请输入新名称")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(inputEditText)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final String bleName = inputEditText.getText().toString();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //开始验证
                                        if (bleNfcDevice.changeBleName(bleName)) {
                                            msgBuffer.delete(0, msgBuffer.length());
                                            msgBuffer.append("蓝牙名称修改成功！重启设备后生效。").append("\r\n");
                                            handler.sendEmptyMessage(0);
                                        }
                                        else {
                                            msgBuffer.delete(0, msgBuffer.length());
                                            msgBuffer.append("蓝牙名称修改失败！").append("\r\n");
                                            handler.sendEmptyMessage(0);
                                        }
                                    } catch (DeviceNoResponseException e) {
                                        e.printStackTrace();
                                        msgBuffer.delete(0, msgBuffer.length());
                                        msgBuffer.append("蓝牙名称修改失败！").append("\r\n");
                                        handler.sendEmptyMessage(0);
                                    }
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    //清空显示按键监听
    private class claerButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            msgBuffer.delete(0, msgBuffer.length());
            handler.sendEmptyMessage(0);

//            byte[] sendBytes = new byte[]{
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99,
//                    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99};
//            StringBuffer stringBuffer = new StringBuffer();
//            for (int i=0; i<sendBytes.length; i++) {
//                stringBuffer.append(String.format("%02x", sendBytes[i]));
//            }
//
//            final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
//            Date curDate =  new Date(System.currentTimeMillis());
//            msgBuffer.append(String.format("发送时间：" + formatter.format(curDate) + "\r\n"));
//            msgBuffer.append(String.format("发送数据长度：%d\r\n", sendBytes.length));
//            msgBuffer.append("发送的数据：\r\n" + stringBuffer);
//            bleNfcDevice.requestPalTestChannel(sendBytes,
//                    new DeviceManager.onReceivePalTestChannelListener() {
//                        @Override
//                        public void onReceivePalTestChannel(byte[] returnData) {
//                            Date curDate =  new Date(System.currentTimeMillis());
//                            msgBuffer.append(String.format("\r\n发送完成！\r\n"));
//                            StringBuffer stringBuffer = new StringBuffer();
//                            for (int i=0; i<returnData.length; i++) {
//                                stringBuffer.append(String.format("%02x", returnData[i]));
//                            }
//                            System.out.println(stringBuffer);
//                            msgBuffer.append("开始接收数据：\r\n" + stringBuffer);
//                            msgBuffer.append(String.format("\r\n接收数据长度：%d\r\n", returnData.length));
//                            msgBuffer.append(String.format("结束时间：" + formatter.format(curDate) + "\r\n"));
//                            handler.sendEmptyMessage(0);
//                        }
//                    });
        }
    }

    //打开蜂鸣器监听
    private class OpenBeepButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            bleNfcDevice.requestOpenBeep(50, 50, 255, new DeviceManager.onReceiveOpenBeepCmdListener() {
                @Override
                public void onReceiveOpenBeepCmd(boolean isSuc) {
                    if (isSuc) {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("打开蜂鸣器成功");
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    //关闭蜂鸣器监听
    private class closeBeepButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            bleNfcDevice.requestOpenBeep(50, 50, 0, new DeviceManager.onReceiveOpenBeepCmdListener() {
                @Override
                public void onReceiveOpenBeepCmd(boolean isSuc) {
                    if (isSuc) {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("关闭蜂鸣器成功");
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    //打开防丢器功能监听
    private class OpenAntiLostButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            bleNfcDevice.requestAntiLostSwitch(true, new DeviceManager.onReceiveAntiLostSwitchListener() {
                @Override
                public void onReceiveAntiLostSwitch(boolean isSuc) {
                    if (isSuc) {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("打开防丢器功能成功");
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    //关闭防丢器功能监听
    private class CloseAntiLostButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            bleNfcDevice.requestAntiLostSwitch(false, new DeviceManager.onReceiveAntiLostSwitchListener() {
                @Override
                public void onReceiveAntiLostSwitch(boolean isSuc) {
                    if (isSuc) {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("关闭防丢器功能成功");
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    //打开自动寻卡按键监听
    private class OpenAutoSearchCardButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //打开/关闭自动寻卡，200ms间隔，寻M1/UL卡
                        boolean isSuc = bleNfcDevice.startAutoSearchCard((byte) 20, ComByteManager.ISO14443_P4);
                        if (isSuc) {
                            msgBuffer.delete(0, msgBuffer.length());
                            msgBuffer.append("自动寻卡已打开！\r\n");
                            handler.sendEmptyMessage(0);
                        }
                        else {
                            msgBuffer.delete(0, msgBuffer.length());
                            msgBuffer.append("自动寻卡已关闭！\r\n");
                            handler.sendEmptyMessage(0);
                        }
                    } catch (DeviceNoResponseException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    //关闭自动寻卡按键监听
    private class CloseAutoSearchCardButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            //打开/关闭自动寻卡，100ms间隔，寻M1/UL卡、CPU卡
            bleNfcDevice.requestRfmAutoSearchCard(false, (byte) 20, ComByteManager.ISO14443_P4, new DeviceManager.onReceiveAutoSearchCardListener() {
                @Override
                public void onReceiveAutoSearchCard(boolean isSuc) {
                    if (isSuc) {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("自动寻卡已打开！\r\n");
                        handler.sendEmptyMessage(0);
                    }
                    else {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("自动寻卡已关闭！\r\n");
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    /***********************************************************************************************************************************/

    //开始自动寻卡
    private boolean startAutoSearchCard() throws DeviceNoResponseException {
        //打开自动寻卡，200ms间隔，寻M1/UL卡
        boolean isSuc = false;
        int falseCnt = 0;
        do {
            isSuc = bleNfcDevice.startAutoSearchCard((byte) 20, ComByteManager.ISO14443_P4);
        }while (!isSuc && (falseCnt++ < 10));
        if (!isSuc){
            //msgBuffer.delete(0, msgBuffer.length());
            msgBuffer.append("不支持自动寻卡！\r\n");
            handler.sendEmptyMessage(0);
        }
        return isSuc;
    }

    /*****************************************************************************************************/






    //读写卡Demo
    public boolean readWriteCardDemo(int cardType) {
        switch(cardType){
        case DeviceManager.CARD_TYPE_MIFARE:
        final Mifare mifare = (Mifare) bleNfcDevice.getCard();


                if (mifare != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("寻到Mifare卡->UID:").append(mifare.uidToString()).append("\r\n");
                    handler.sendEmptyMessage(0);

























/****************************************************************************************************************/



                    /*msgBuffer.append("开始验证第1块密码\r\n");

                    byte[] key = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
                    try {
                        boolean anth = mifare.authenticate((byte) 1, Mifare.MIFARE_KEY_TYPE_A, key);
                        if (anth) {
                            msgBuffer.append("验证密码成功\r\n");
                            msgBuffer.append("写00112233445566778899001122334455到块1\r\n");
                            handler.sendEmptyMessage(0);
                            boolean isSuc = mifare.write((byte)1, new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55});
                            if (isSuc) {
                                msgBuffer.append("写成功！\r\n");
                                msgBuffer.append("读块1数据\r\n");
                                handler.sendEmptyMessage(0);
                                byte[] readDataBytes = mifare.read((byte) 1);
                                msgBuffer.append("块1数据:").append(StringTool.byteHexToSting(readDataBytes)).append("\r\n");
                                handler.sendEmptyMessage(0);
                            } else {
                                msgBuffer.append("写失败！\r\n");
                                handler.sendEmptyMessage(0);
                                return false;
                            }
                        }
                        else {
                            msgBuffer.append("验证密码失败\r\n");
                            handler.sendEmptyMessage(0);
                            return false;
                        }
                    } catch (CardNoResponseException e) {
                        e.printStackTrace();
                        return false;
                    }*/
                }
                break;
        }
        return true;
    }

    //搜索最近的设备并连接
    private void searchNearestBleDevice() {
        msgBuffer.delete(0, msgBuffer.length());
        msgBuffer.append("正在搜索设备...");
        handler.sendEmptyMessage(0);
        if (!mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        mScanner.startScan(0);
                        mNearestBleLock.lock();
                        try {
                            mNearestBle = null;
                        }finally {
                            mNearestBleLock.unlock();
                        }
                        lastRssi = -100;

                        int searchCnt = 0;
                        while ((mNearestBle == null)
                                && (searchCnt < 10000)
                                && (mScanner.isScanning())
                                && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                            searchCnt++;
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mScanner.stopScan();
                            mNearestBleLock.lock();
                            try {
                                if (mNearestBle != null) {
                                    mScanner.stopScan();
                                    msgBuffer.delete(0, msgBuffer.length());
                                    msgBuffer.append("正在连接设备...");
                                    handler.sendEmptyMessage(0);
                                    bleNfcDevice.requestConnectBleDevice(mNearestBle.getAddress());
                                } else {
                                    msgBuffer.delete(0, msgBuffer.length());
                                    msgBuffer.append("未找到设备！");
                                    handler.sendEmptyMessage(0);
                                }
                            }finally {
                                mNearestBleLock.unlock();
                            }
                        } else {
                            mScanner.stopScan();
                        }
                    }
                }
            }).start();
        }
    }

    //发送读写进度条显示Handler
    private void showReadWriteDialog(String msg, int rate) {
        Message message = new Message();
        message.what = 4;
        message.arg1 = rate;
        message.obj = msg;
        handler.sendMessage(message);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            msgText.setText(msgBuffer);

            if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) || ((bleNfcDevice.isConnection() == BleManager.STATE_CONNECTING)) ) {
                searchButton.setText("断开连接");
            }
            else {
                searchButton.setText("搜索设备");
            }

            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                byte versions = bleNfcDevice.getDeviceVersions();
                                msgBuffer.append("设备版本:").append(String.format("%02x", versions)).append("\r\n");
                                handler.sendEmptyMessage(0);
                                double voltage = bleNfcDevice.getDeviceBatteryVoltage();
                                msgBuffer.append("设备电池电压:").append(String.format("%.2f", voltage)).append("\r\n");
                                if (voltage < 3.61) {
                                    msgBuffer.append("设备电池电量低，请及时充电！");
                                } else {
                                    msgBuffer.append("设备电池电量充足！");
                                }
                                handler.sendEmptyMessage(0);
                                boolean isSuc = bleNfcDevice.androidFastParams(true);
                                if (isSuc) {
                                    msgBuffer.append("\r\n蓝牙快速传输参数设置成功!");
                                }
                                else {
                                    msgBuffer.append("\n不支持快速传输参数设置!");
                                }
                                handler.sendEmptyMessage(0);

                                msgBuffer.append("\n开启自动寻卡...\r\n");
                                handler.sendEmptyMessage(0);
                                //开始自动寻卡
                                startAutoSearchCard();
                            } catch (DeviceNoResponseException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    break;

                case 4:   //读写进度条
                    if ((msg.arg1 == 0) || (msg.arg1 == 100)) {
                        readWriteDialog.dismiss();
                        readWriteDialog.setProgress(0);
                    } else {
                        readWriteDialog.setMessage((String) msg.obj);
                        readWriteDialog.setProgress(msg.arg1);
                        if (!readWriteDialog.isShowing()) {
                            readWriteDialog.show();
                        }
                    }
                    break;
                case 7:  //搜索设备列表
//                    items = mScanner.getDeviceNames();
//                    if (alertDialog == null) {
//                        alertDialog = new AlertDialog.Builder(MainActivity.this)
//                                .setTitle("请选择设备连接")
//                                .setItems(items, new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        // TODO Auto-generated method stub
//                                        mScanner.stopScan();
//                                        mScanner.setOnReceiveScannerListener(null);
//                                        msgBuffer.delete(0, msgBuffer.length());
//                                        msgBuffer.append("正在连接设备...");
//                                        handler.sendEmptyMessage(0);
//                                        bleNfcDevice.requestConnectBleDevice(mScanner.getDeviceList().get(which).getAddress());
//                                    }
//                                })
//                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        mScanner.stopScan();
//                                        mScanner.setOnReceiveScannerListener(null);
//                                    }
//                                });
//                        alertDialog.show();
//                    }
//                    else if (!alertDialog.create().isShowing()) {
//                        alertDialog.show();
//                    }
//                    else {
//                        alertDialog.setItems(items, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                // TODO Auto-generated method stub
//                                mScanner.stopScan();
//                                mScanner.setOnReceiveScannerListener(null);
//                                msgBuffer.delete(0, msgBuffer.length());
//                                msgBuffer.append("正在连接设备...");
//                                handler.sendEmptyMessage(0);
//                                bleNfcDevice.requestConnectBleDevice(mScanner.getDeviceList().get(which).getAddress());
//                            }
//                        });
//                        alertDialog.create().show();
//                    }
                    break;
            }
        }
    };
}
