package com.jootu.nfc.Json;

/**
 * Created by Administrator on 2017/7/21.
 */

public class Login {
    public int id;//巡检员id
    public String name;
    public String password;

    public Login(int id,String name, String password) {
        this.id=id;
        this.name = name;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
