package com.irlab.view.utils;

import com.irlab.view.models.Board;
import com.irlab.view.models.Point;

public class BoardUtil {
    /**
     * 由检测到的二维平面坐标转化为棋盘坐标
     */
    public static String getPositionByIndex(int x, int y) {
        String position = "";
        int cnt = 0;
        for (char c = 'A'; c <= 'T'; c ++ ) {
            if (c == 'I') continue;
            if (cnt == y) {
                position += c;
                break;
            }
            cnt ++;
        }
        position += (x + 1);
        return position;
    }

    public static String genPlayCmd(Point move) {
        return "play " + (move.getGroup().getOwner().getIdentifier() == Board.BLACK_STONE ? "B" : "W") + " " + getPositionByIndex(move.getX(), move.getY());
    }
}
