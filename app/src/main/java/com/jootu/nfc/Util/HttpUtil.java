package com.jootu.nfc.Util;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2017/7/22.
 */

public class HttpUtil {
        public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder().url(address).build();
            client.newCall(request).enqueue(callback);
        }

   /* public static void userOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        RequestBody requestBody=new FormBody.Builder()
                .add("name",namego)
                .add("password",str2)
                .build();
        Request request=new Request.Builder().url(address).post(requestBody).build();
        client.newCall(request).enqueue(callback);

    }*/



}
