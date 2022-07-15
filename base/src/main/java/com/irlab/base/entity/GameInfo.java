package com.irlab.base.entity;

import java.io.Serializable;
import java.util.Date;

public class GameInfo implements Serializable {
    Long id;

    String playInfo;

    String result;

    String code;

    String createTime;

    public GameInfo(Long id, String playInfo, String result, String code, String createTime) {
        this.id = id;
        this.playInfo = playInfo;
        this.result = result;
        this.code = code;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlayInfo() {
        return playInfo;
    }

    public void setPlayInfo(String playInfo) {
        this.playInfo = playInfo;
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
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "GameInfo{" +
                "id=" + id +
                ", playInfo='" + playInfo + '\'' +
                ", result='" + result + '\'' +
                ", code='" + code + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
