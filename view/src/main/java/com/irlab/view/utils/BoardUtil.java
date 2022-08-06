package com.irlab.view.utils;

public class BoardUtil {
    /**
     * 由检测到的二维平面坐标转化为棋盘坐标
     */
    public static String getPositionByIndex(int x, int y) {
        String position = "";
        int cnt = 0;
        for (char c = 'A'; c <= 'Z'; c ++ ) {
            if (c == 'I') continue;
            if (cnt == x) {
                position += c;
                break;
            }
            cnt ++;
        }
        position += " " + (y + 1);
        return position;
    }
}
