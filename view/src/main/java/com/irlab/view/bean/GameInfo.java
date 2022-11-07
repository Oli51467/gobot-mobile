package com.irlab.view.bean;

import java.io.Serializable;

public class GameInfo implements Serializable {
    String code;
    String create_time;
    int id;
    String play_info;
    String result;
    String user_name;

    public GameInfo(int id, String play_info, String result, String code, String create_time) {
        this.id = id;
        this.play_info = play_info;
        this.result = result;
        this.code = code;
        this.create_time = create_time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayInfo() {
        return play_info;
    }

    public void setPlayInfo(String playInfo) {
        this.play_info = playInfo;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCreateTime() {
        return create_time;
    }

    public void setCreateTime(String createTime) {
        this.create_time = createTime;
    }

    public String getPlay_info() {
        return play_info;
    }

    public void setPlay_info(String play_info) {
        this.play_info = play_info;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }
}
