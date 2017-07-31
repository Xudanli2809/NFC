package com.jootu.nfc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jootu.nfc.Json.Fire;
import com.jootu.nfc.Util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.jootu.nfc.Util.HttpUtil.sendOkHttpRequest;


/**
 * A simple {@link Fragment} subclass.
 */
public class Detail extends Activity {
    private TextView mid;
    private TextView mfire_type_id;
    private TextView maddress;
    private TextView mlocation;
    private TextView mcreated_at;

    private TextView respon;

    private TextView mtype_id;
    private TextView mtype;
    private TextView mfire_level;
    private TextView magent;
    private TextView mtemperature;
    private TextView mspecifications;

    private ScrollView muid_layout;

    private String muid;
    private Button gofeed;
    public String nid;
    private int userid;






    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details);

        //初始化各种控件
        mid=(TextView)findViewById(R.id.id);

        mfire_type_id=(TextView)findViewById(R.id.fire_type_id);
        maddress=(TextView)findViewById(R.id.address);
        mlocation=(TextView)findViewById(R.id.location);
        mcreated_at=(TextView)findViewById(R.id.created_at);

        mtype_id=(TextView)findViewById(R.id.type_id);
        mtype=(TextView)findViewById(R.id.type);
        mfire_level=(TextView)findViewById(R.id.fire_level);
        magent=(TextView)findViewById(R.id.agent);
        mtemperature=(TextView)findViewById(R.id.temperature);
        mspecifications=(TextView)findViewById(R.id.specifications);

        respon=(TextView)findViewById(R.id.respon);
        gofeed=(Button)findViewById(R.id.btn_gofeed);
        muid_layout=(ScrollView)findViewById(R.id.uid_layout);

        //根据uid，请求uid对应的灭火器信息

        /*muid=getIntent().getStringExtra("uid");*/
        Bundle bd=getIntent().getExtras();
        userid=bd.getInt("user_id");
        muid=bd.getString("uid");
        Toast.makeText(Detail.this,"aaa"+userid,Toast.LENGTH_SHORT).show();
        muid_layout.setVisibility(View.VISIBLE);

        requestUid(muid);


        gofeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Detail.this,FeedBack.class);
                Bundle bd=new Bundle();
                bd.putInt("user_id",userid);
                bd.putInt("alarm_id",Integer.valueOf(nid).intValue());
                intent.putExtras(bd);
                startActivity(intent);
            }
        });


    }


    //根据uid请求灭火器具体信息
    public void requestUid(final String fire_uid){
        //拼接处一个接口地址
        String address="http://fc.jootu.tech/api/admin/query_fire?fire_uid="+fire_uid;
        Log.e("address:",address);

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Detail.this,"获取灭火器信息失败",Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String responseText=response.body().string();//获取服务器返回的数据的具体内容
                /*showResponse(responseText);*/


                //再将当前线程切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /*final String  fire= *//*Utility.*/handleIdResponse(responseText); //将获取的数据转化为fire对象，并进行解析
                        /*if(fire!=null){
                            SharedPreferences.Editor editor= PreferenceManager.getDefaultSharedPreferences(Detail.this).edit();
                            editor.putInt("fireid",nid);
                            editor.apply();
                            Toast.makeText(Detail.this,"获取灭火器对应信息成功",Toast.LENGTH_SHORT).show();
                            //将内容显示出来
                            *//*showUidInfo(fire);*//*
                        }else{
                            Toast.makeText(Detail.this,"获取灭火器对应信息失败",Toast.LENGTH_SHORT).show();
                        }*/
                    }
                });

            }
        });

    }


   /* public void showResponse(final String response){
        //调用runOnUiThread方法，把更新ui的代码创建在Runnable中，Runnable对像就能在ui程序中被调用。
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //在这里进行UI操作，将结果显示在屏幕上
                respon.setText(response);
            }
        });
    }*/

    public String handleIdResponse(String response){

        try{

            JSONArray jsonArray=new JSONArray(response);
            for(int i=0;i<jsonArray.length();i++){
                JSONObject jsonObject=jsonArray.getJSONObject(i);
                nid=jsonObject.getString("id");
                String nuid=jsonObject.getString("uid");

                String nfire_type_id=jsonObject.getString("fire_type_id");
                String naddress=jsonObject.getString("address");
                String nlocation=jsonObject.getString("location");
                String ncreated_at=jsonObject.getString("created_at");
                String ntype_id=jsonObject.getString("type_id");
                String ntype=jsonObject.getString("type");
                String nfire_level=jsonObject.getString("fire_level");
                String nagent=jsonObject.getString("agent");
                String ntemperature=jsonObject.getString("temperature");
                String nspecifications=jsonObject.getString("specifications");

                //将解析出来的数据，放入控件中
                mid.setText(nid);
                /*mmuid.setText(nuid);*/
                mfire_type_id.setText(nfire_type_id);
                maddress.setText(naddress);
                mlocation.setText(nlocation);
                mcreated_at.setText(ncreated_at);
                mtype_id.setText(ntype_id);
                mtype.setText(ntype);
                mfire_level.setText(nfire_level);
                magent.setText(nagent);
                mtemperature.setText(ntemperature);
                mspecifications.setText(" "+nspecifications);
                muid_layout.setVisibility(View.VISIBLE);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }




}
