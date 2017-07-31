package com.jootu.nfc.Json;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Administrator on 2017/7/21.
 */

public class Fire {

    public int id; //灭火器的ID
    @SerializedName("uid")
    public String fire_uid; //M1卡的uid

    public int fire_type_id;  // 灭火器型号id
    public String address;   //地址
    public String location;  //详细地址（**楼、**出口）
    public String created_at;   //添加时间

    public String type_id;  //型号（国家统一命名标准），类似于MFZ/ABC4
    public String type; //灭火器类型（干粉、二氧化碳……）
    public String fire_level;   //灭火等级（国家标准）
    public String agent;    //灭火剂成分
    public String temperature;  // 试用温度
    public float specifications;    //规格（内容物的质量KG、L）

    /*public Fire(String agent, String created_at,
                String address, String fire_level,
                int fire_type_id, int id,
                String fire_uid, String location,
                float specifications, String type,
                String temperature, String type_id) {
        this.agent = agent;
        this.created_at = created_at;
        this.address = address;
        this.fire_level = fire_level;
        this.fire_type_id = fire_type_id;
        this.id = id;
        this.fire_uid = fire_uid;
        this.location = location;
        this.specifications = specifications;
        this.type = type;
        this.temperature = temperature;
        this.type_id = type_id;
    }*/

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFire_level() {
        return fire_level;
    }

    public void setFire_level(String fire_level) {
        this.fire_level = fire_level;
    }

    public int getFire_type_id() {
        return fire_type_id;
    }

    public void setFire_type_id(int fire_type_id) {
        this.fire_type_id = fire_type_id;
    }

    public String getFire_uid() {
        return fire_uid;
    }

    public void setFire_uid(String fire_uid) {
        this.fire_uid = fire_uid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public float getSpecifications() {
        return specifications;
    }

    public void setSpecifications(float specifications) {
        this.specifications = specifications;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType_id() {
        return type_id;
    }

    public void setType_id(String type_id) {
        this.type_id = type_id;
    }
}
