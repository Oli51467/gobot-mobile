package com.irlab.base.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Config {

    @PrimaryKey(autoGenerate = true)
    private int id;

    String playerBlack;

    String playerWhite;

    String title;

    String desc;

    int rule;

    int T;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getRule() {
        return rule;
    }

    public void setRule(int rule) {
        this.rule = rule;
    }

    public int getT() {
        return T;
    }

    public void setT(int t) {
        this.T = t;
    }

    @Override
    public String toString() {
        return "PlayConfiguration{" +
                "id=" + id +
                ", playerBlack='" + playerBlack + '\'' +
                ", playerWhite='" + playerWhite + '\'' +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", rule='" + rule + '\'' +
                ", T='" + T + '\'' +
                '}';
    }
}
