package com.irlab.view.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 描述可以放置棋子位置的类 与棋盘关联
public class Point implements Serializable {

    private final Board board;

    private final int x, y;

    // 描述该点是否在一个组内或属于哪个组
    private Group Group;

    public Point(Board board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
        this.Group = null;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Group getGroup() {
        return Group;
    }

    // Group可以为空
    public void setGroup(com.irlab.view.models.Group Group) {
        this.Group = Group;
    }

    public boolean isEmpty() {
        return Group == null;
    }

    // 拿到相邻棋子所属的组
    public Set<Group> getAdjacentGroups() {
        Set<Group> adjacentGroups = new HashSet<com.irlab.view.models.Group>();

        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        assert dx.length == dy.length : "dx and dy should have the same length";

        for (int i = 0; i < dx.length; i++) {
            int newX = x + dx[i];
            int newY = y + dy[i];

            if (board.isInBoard(newX, newY)) {
                Point adjPoint = board.getPoint(newX, newY);
                if (adjPoint.Group != null) {
                    adjacentGroups.add(adjPoint.Group);
                }
            }
        }
        return adjacentGroups;
    }

    public List<Point> getEmptyNeighbors() {
        List<Point> emptyNeighbors = new ArrayList<Point>();

        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        assert dx.length == dy.length : "dx and dy should have the same length";

        for (int i = 0; i < dx.length; i++) {
            int newX = x + dx[i];
            int newY = y + dy[i];

            if (board.isInBoard(newX, newY)) {
                Point adjPoint = board.getPoint(newX, newY);
                if (adjPoint.isEmpty()) {
                    emptyNeighbors.add(adjPoint);
                }
            }
        }
        return emptyNeighbors;
    }
}
