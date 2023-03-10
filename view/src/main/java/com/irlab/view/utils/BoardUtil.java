package com.irlab.view.utils;

import android.util.Pair;

import com.irlab.view.models.Board;
import com.irlab.view.models.Point;

public class BoardUtil {
    /**
     * 由检测到的二维平面坐标转化为棋盘坐标
     */
    public static String getPositionByIndex(int x, int y) {
        String position = "";
        int cnt = 1;
        for (char c = 'A'; c <= 'T'; c ++ ) {
            if (c == 'I') continue;
            if (cnt == y) {
                position += c;
                break;
            }
            cnt ++;
        }
        position += 20 - x;
        return position;
    }

    public static String genPlayCmd(Point move) {
        return "play_genmove " + (move.getGroup().getOwner().getIdentifier() == Board.BLACK_STONE ? "B" : "W") + " " + getPositionByIndex(move.getX(), move.getY());
    }

    public static Pair<Integer, Integer> transformIndex(String index) {
        String alpha = index.substring(0, 1);
        String number = index.substring(1);
        int cnt = 1;
        for (char c = 'A'; c <= 'T'; c ++) {
            if (c == 'I') continue;
            if (String.valueOf(c).equals(alpha)) break;
            cnt ++;
        }
        return new Pair<>(20 - Integer.parseInt(number), cnt);
    }
}
