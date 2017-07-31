package com.jootu.nfc.Exception;

/**
 * Created by Administrator on 2017/7/19.
 */

public class DeviceNoResponseException extends Exception {

    public DeviceNoResponseException(){
    }

    //设备超时无响应异常
    public DeviceNoResponseException(String detailMessage){
        super(detailMessage);
    }

}
