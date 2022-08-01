package com.irlab.view.models;

import java.io.Serializable;
import java.util.Stack;

public class GameRecord implements Serializable {

    private final Stack<GameTurn> preceding;

    private final Stack<GameTurn> following;

    public GameRecord(int width, int height, int handicap) {
        preceding = new Stack<>();
        following = new Stack<>();
        GameTurn first = new GameTurn(width, height);
        apply(first);
    }

    public void apply(GameTurn turn) {
        preceding.push(turn);
        following.clear();
    }

    public Iterable<GameTurn> getTurns() {
        return preceding;
    }

    public GameTurn getLastTurn() {
        return preceding.peek();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;

        GameRecord castedObj = (GameRecord) obj;

        if (preceding.size() != castedObj.preceding.size() | following.size() != castedObj.following.size()) return false;

        for (int i = 0; i < preceding.size(); i++) {
            if (!preceding.get(i).equals(castedObj.preceding.get(i))) return false;
        }
        for (int i = 0; i < following.size(); i++) {
            if (!following.get(i).equals(castedObj.following.get(i))) return false;
        }

        return true;
    }
}
