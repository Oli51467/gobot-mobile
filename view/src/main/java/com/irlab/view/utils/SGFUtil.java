package com.irlab.view.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.irlab.view.models.Point;

public class SGFUtil {
    public static List<Point> parseSGF(String content) {
        String s = "abcdefghijklmnopqrstuvwxyz";
        // 落子集合
        List<Point> moves = new ArrayList<>();
        // 取出所有的黑棋落子
        List<String> blackMoves = getStrContainData(content, ";B\\[", "\\]");
        int i = 0;
        for (String str : blackMoves) {
            int x = s.indexOf(str.charAt(0)) + (str.charAt(0) > i ? 1 : 0);
            int y = s.indexOf(str.charAt(1)) + (str.charAt(1) > i ? 1 : 0);
            Point point = new Point("black", x, y, i * 2 + 1);
            moves.add(point);
            i++;
        }

        // 取出所有的白棋落子
        int j = 0;
        List<String> whiteMoves = getStrContainData(content, ";W\\[", "\\]");
        for (String str : whiteMoves) {
            int x = s.indexOf(str.charAt(0)) + (str.charAt(0) > i ? 1 : 0);
            int y = s.indexOf(str.charAt(1)) + (str.charAt(1) > i ? 1 : 0);
            Point point = new Point("white", x, y, j * 2 + 2);
            moves.add(point);
            j++;
        }

        moves.sort((o1, o2) -> {
            if (o1.getStep() > o2.getStep()) {
                return 1;
            } else {
                return -1;
            }
        });
        return moves;
    }

    public static List<String> getStrContainData(String str, String start, String end) {
        List<String> result = new ArrayList<>();
        String regex = start + "(.*?)" + end;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!key.contains(start) && !key.contains(end)) {
                result.add(key);
            }
        }
        return result;
    }
}