package com.irlab.base.entity;

import java.io.Serializable;

public class CellData implements Serializable {

    int id;

    String title;

    String desc;

    public CellData(String title, String desc, int id) {
        this.title = title;
        this.desc = desc;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
