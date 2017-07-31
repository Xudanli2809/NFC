package com.jootu.nfc.Json;

/**
 * Created by Administrator on 2017/7/22.
 */

public class Feed {
    public int fire_id;
    public String pressure_test;
    public int  appearance_test;
    public String handling;
    public String detection_date;
    public int user_id;
    public String problem_remarks;


    public int getAppearance_test() {
        return appearance_test;
    }

    public void setAppearance_test(int appearance_test) {
        this.appearance_test = appearance_test;
    }

    public String getDetection_date() {
        return detection_date;
    }

    public void setDetection_date(String detection_date) {
        this.detection_date = detection_date;
    }

    public String getHangling() {
        return handling;
    }

    public void setHangling(String hangling) {
        this.handling = hangling;
    }

    public int getFire_id() {
        return fire_id;
    }

    public void setFire_id(int fire_id) {
        this.fire_id = fire_id;
    }

    public String getPressure_test() {
        return pressure_test;
    }

    public void setPressure_test(String pressure_test) {
        this.pressure_test = pressure_test;
    }

    public String getProblem_remarks() {
        return problem_remarks;
    }

    public void setProblem_remarks(String problem_remarks) {
        this.problem_remarks = problem_remarks;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}
