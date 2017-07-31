package com.jootu.nfc.Exception;

/**
 * Created by Administrator on 2017/7/19.
 */

public class CardNoResponseException extends Exception{
    public CardNoResponseException(){
    }

    //卡片超时无响应异常
    public CardNoResponseException(String detailMessage){
        super(detailMessage);
    }

}
