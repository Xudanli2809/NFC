package com.jootu.nfc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Login extends Activity {

    private EditText user;
    private EditText pwd;
    private TextView respon;
    private String username;
    private String password;
    int userid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        //使背景图和状态栏融合到一起，这个功能只有API21(android 5.0)及以上版本才支持
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();//拿到当前活动的DecorView
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);//表示当前活动的布局会显示在状态栏上面
            getWindow().setStatusBarColor(Color.TRANSPARENT);//将状态栏设置成透明色
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        user = (EditText) findViewById(R.id.username);
        pwd = (EditText) findViewById(R.id.password);
        respon=(TextView)findViewById(R.id.respon);
        Button btn=(Button)findViewById(R.id.login_btn);
        final String address = "http://fc.jootu.tech/api/admin/api_login";
        //登录按钮点击事件
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userOkHttpRequest(address);
            }
        });
    }

    //post方式向服务器请求
    public void userOkHttpRequest(final String address){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client=new OkHttpClient();
                    username = user.getText().toString().trim();
                    password = pwd.getText().toString().trim();
                    RequestBody requestBody=new FormBody.Builder()
                            .add("name",username)
                            .add("password",password)
                            .build();
                    Request request=new Request.Builder()
                            .url(address)
                            .post(requestBody)
                            .build();
                        Response response=client.newCall(request).execute();
                        String responseText = response.body().string();
                        /*showResponse(responseText);*/
                    try {
                        JSONObject jsonObject = new JSONObject(responseText);
                        userid= jsonObject.getInt("id");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.e("userid",String.valueOf(userid));

                //根据请求之后服务器返回结果，判断是否登录成功
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //在这里进行UI操作，将结果显示在屏幕上
                        if(userid!=0){
                            Toast.makeText(Login.this,"登录成功",Toast.LENGTH_SHORT).show();
                            Intent intent=new Intent(Login.this,MainActivity.class);
                            intent.putExtra("userid",userid);
                            startActivity(intent);
                        }else{
                            Toast.makeText(Login.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}
