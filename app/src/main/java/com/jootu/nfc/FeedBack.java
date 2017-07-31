package com.jootu.nfc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/7/22.
 */

public class FeedBack extends Activity{
    private Button submit;
    private EditText pressure;
    private RadioButton good;
    private RadioButton bad;
    private EditText problem;
    private EditText deal;
    private TextView respon;

    private RadioGroup appearance;
    private String mpressure;
    private String mproblem;
    private String mdeal;
    private int alarmid;
    private int userid;
    String status;
    int temp=1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        ////使背景图和状态栏融合到一起，这个功能只有API21(android 5.0)及以上版本才支持
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();//拿到当前活动的DecorView
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);//表示当前活动的布局会显示在状态栏上面
            getWindow().setStatusBarColor(Color.TRANSPARENT);//将状态栏设置成透明色
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

        //获取布局对象
        pressure=(EditText)findViewById(R.id.et_pressure);

        appearance=(RadioGroup)findViewById(R.id.apper_result);
        good=(RadioButton)findViewById(R.id.good);
        bad=(RadioButton) findViewById(R.id.bad);
        problem=(EditText)findViewById(R.id.et_problem);
        deal=(EditText)findViewById(R.id.et_dealway);
        submit=(Button)findViewById(R.id.submit);
        respon=(TextView)findViewById(R.id.respon);


        //拿到灭火器id

        Bundle bd=getIntent().getExtras();
        alarmid=bd.getInt("alarm_id");
        userid=bd.getInt("user_id");
        Toast.makeText(FeedBack.this,"用户id： "+userid,Toast.LENGTH_SHORT).show();
        Log.e("alarm",String.valueOf(alarmid));

        appearance.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if(FeedBack.this.bad.getId()==checkedId){
                    temp=0;
                }else if(FeedBack.this.good.getId()==checkedId){
                    temp=1;
                }
                Log.e("temp",String.valueOf(temp));
            }
        });



        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String address="http://fc.jootu.tech/api/admin/add_fire";
                feedOkHttpRequest(address);
                if (status!="ok"){
                    Toast.makeText(FeedBack.this,"提交成功",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(FeedBack.this,MainActivity.class);
                    intent.putExtra("userid",userid);
                    startActivity(intent);
                }else {
                    Toast.makeText(FeedBack.this,"提交失败",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    //post方式向服务器请求
    public void feedOkHttpRequest(final String address) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    mpressure = pressure.getText().toString().trim();
                   /* mapper = apper.getText().toString().trim();*/
                    mproblem = problem.getText().toString().trim();
                    mdeal = deal.getText().toString().trim();
                    String nowdate=getNowDateTime("yyyy-MM-dd|HH:mm:ss");

                    //请求体
                    RequestBody requestBody = new FormBody.Builder()
                            .add("fire_id", String.valueOf(alarmid))
                            .add("pressure_test", mpressure)
                            .add("appearance_test", String.valueOf(temp))
                            .add("handling",mdeal)
                            .add("detection_date",nowdate)
                            .add("user_id", String.valueOf(userid))
                            .add("problem_remarks",mproblem)
                            .build();

                    Request request = new Request.Builder()
                            .url(address)
                            .post(requestBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    showResponse(responseData);
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        status = jsonObject.getString("status");
                        Log.e("jhdgshdg:", "status=" + status);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

      public void showResponse(final String response){
        //调用runOnUiThread方法，把更新ui的代码创建在Runnable中，Runnable对像就能在ui程序中被调用。
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //在这里进行UI操作，将结果显示在屏幕上
                respon.setText(response);
            }
        });
    }

    private String getNowDateTime(String strFormat){
        if(strFormat==""){
            strFormat="yyyy-MM-dd HH:mm:ss";
        }
        Date now = new Date();
        SimpleDateFormat df = new SimpleDateFormat(strFormat);//设置日期格式
        return df.format(now); // new Date()为获取当前系统时间
    }
}
