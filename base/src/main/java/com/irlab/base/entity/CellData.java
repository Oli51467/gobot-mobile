package com.irlab.base.entity;

import java.io.Serializable;

public class CellData implements Serializable {

    Long id;

    String playerBlack;

    String playerWhite;

    String desc;

    String engine;

    int rule;

    int komi;

    public CellData(Long id, String playerBlack, String playerWhite, String engine, String desc, int komi, int rule) {
        this.id = id;
        this.playerBlack = playerBlack;
        this.playerWhite = playerWhite;
        this.engine = engine;
        this.desc = desc;
        this.komi = komi;
        this.rule = rule;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    @Override
    public String toString() {
        return "CellData{" +
                "id=" + id +
                ", playerBlack='" + playerBlack + '\'' +
                ", playerWhite='" + playerWhite + '\'' +
                ", desc='" + desc + '\'' +
                ", engine='" + engine + '\'' +
                ", rule=" + rule +
                ", komi=" + komi +
                '}';
    }
}
