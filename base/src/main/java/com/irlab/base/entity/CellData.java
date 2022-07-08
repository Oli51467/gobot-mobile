package com.irlab.base.entity;

import java.io.Serializable;

public class CellData implements Serializable {

    int id;

    String playerBlack;

    String playerWhite;

    int rule;

    String desc;

    public CellData(String playerBlack, String playerWhite, String desc, int id, int rule) {
        this.playerBlack = playerBlack;
        this.playerWhite = playerWhite;
        this.desc = desc;
        this.rule = rule;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRule() {
        return rule;
    }

    public void setRule(int rule) {
        this.rule = rule;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
