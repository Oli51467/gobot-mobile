package com.irlab.view.bean;

public class UserResponse {
    private int code;
    private String status;
    private String data;
    private String message;

    public int getCode() { return code; }

    public void setCode(int code) { this.code = code; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status;}

    public String getData() { return data;}

    public void setData(String data) { this.data = data; }

    public String getMessage() { return message;}

    public void setMessage(String message) { this.message = message;}
}
