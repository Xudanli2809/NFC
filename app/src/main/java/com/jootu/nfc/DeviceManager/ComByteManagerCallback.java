package com.jootu.nfc.DeviceManager;

/**
 * Created by lochy on 16/1/21.
 */
public abstract class ComByteManagerCallback {
    public void onRcvBytes(boolean isSuc, byte[] rcvBytes) {}
}
