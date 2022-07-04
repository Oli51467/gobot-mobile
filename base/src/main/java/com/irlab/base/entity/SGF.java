package com.irlab.base.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SGF {

    @PrimaryKey(autoGenerate = true)
    private int id;

    String title;

    String code;

    String desc;

    String result;

    String playerBlack;

    String playerWhite;

    public int getId() {
        return id;
    }

    public void setId(int id) { this.id = id; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getPlayerBlack() {
        return playerBlack;
    }

    public void setPlayerBlack(String playerBlack) {
        this.playerBlack = playerBlack;
    }

    public String getPlayerWhite() {
        return playerWhite;
    }

    public void setPlayerWhite(String playerWhite) {
        this.playerWhite = playerWhite;
    }

    @Override
    public String toString() {
        return "SGF{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", desc='" + desc + '\'' +
                ", result='" + result + '\'' +
                ", playerBlack='" + playerBlack + '\'' +
                ", playerWhite='" + playerWhite + '\'' +
                '}';
    }
}
