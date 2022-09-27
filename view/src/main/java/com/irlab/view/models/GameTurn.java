package com.irlab.view.models;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

public class GameTurn implements Serializable {
    public final int[][] boardState;
    public final int x, y;
    private final int hashCode;
    private int passCount;
    private final int countCapturedStones;

    public GameTurn(int width, int height) {
        boardState = new int[width + 1][height + 1];
        passCount = 0;
        countCapturedStones = 0;

        x = -1;
        y = -1;

        hashCode = Arrays.deepHashCode(boardState);
    }

    private GameTurn(GameTurn prev, int X, int Y, int playerId, int handicap, Set<Point> freedPoint) {
        int width = prev.boardState.length;
        int height = prev.boardState[0].length;

        boardState = new int[width][height];
        for (int i = 0; i < width; i++) {
            boardState[i] = prev.boardState[i].clone();
        }
        x = X;
        y = Y;

        if (x >= 0 && y >= 0) {
            boardState[x][y] = playerId;
            passCount = 0;
        } else {
            passCount = prev.passCount + 1;
        }

        for (Point freedpoint : freedPoint) {
            boardState[freedpoint.getX()][freedpoint.getY()] = 0;
        }
        countCapturedStones = freedPoint.size();
        hashCode = Arrays.deepHashCode(boardState);
    }

    public GameTurn toNext(int X, int Y, int playerId, int handicap, Set<Point> freedPoint) {
        return new GameTurn(this, X, Y, playerId, handicap, freedPoint);
    }

    public int getCountCapturedStones() {
        return countCapturedStones;
    }

    public int[][] getBoardState() {
        return boardState;
    }

    public int getPassCount() { return passCount; }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GameTurn castedObj = (GameTurn) obj;

        return hashCode == castedObj.hashCode && Arrays.deepEquals(this.boardState, castedObj.boardState);
    }
}
