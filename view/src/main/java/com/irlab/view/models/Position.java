package com.irlab.view.models;

import java.util.ArrayList;
import java.util.List;

public class Position {
    public int row;
    public int column;
    public Group group;

    public Position(int row, int column, Group group) {
        this.row = row;
        this.column = column;
        this.group = group;
    }

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public boolean isEmpty() {
        return group == null;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public String toString() {
        int l = 'a' + add1ToJumpLetterI(row);
        int c = 'a' + add1ToJumpLetterI(column);
        return "[" + (char)l + (char)c + "]";
    }

    private char add1ToJumpLetterI(int index) {
        final int I_INDEX = 8;
        return (char)(index + (index >= I_INDEX ? 1 : 0));
    }

    @Override
    public boolean equals(Object position) {
        if (!(position instanceof Position)) return false;
        return row == ((Position)position).row
            && column == ((Position)position).column;
    }

    @Override
    public int hashCode() {
        return row * 39 + column;
    }

    public List<Position> getEmptyNeighbors(Position position, Board board) {
        int x = position.column, y = position.row;

        List<Position> emptyNeighbors = new ArrayList<Position>();

        int[] dx = { -1, 0, 1, 0}, dy = { 0, -1, 0, 1};

        for (int i = 0; i < dx.length; i++) {
            int newX = x + dx[i];
            int newY = y + dy[i];
            if (board.getGroupAt(newX, newY) == null ) {
                Position adjPoint = new Position(newY, newX);
                emptyNeighbors.add(adjPoint);
            }
        }

        return emptyNeighbors;
    }
}
