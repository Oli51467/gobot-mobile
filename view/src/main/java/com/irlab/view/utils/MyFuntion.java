package com.irlab.view.utils;

public class MyFuntion {
    private String name;
    private int imageId;

    public MyFuntion(String name1, int imageId1) {
        name = name1;
        imageId = imageId1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }
}
