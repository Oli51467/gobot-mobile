package com.irlab.view.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of stones on the board.
 */
public class Group {

    private int color;
    private Set<Position> positions;
    private Set<Position> liberties;

    public Group(Group group) {
        this.positions = new HashSet<Position>(group.positions);
        this.liberties = new HashSet<Position>(group.liberties);
        this.color = group.color;
    }

    public Group(int color) {
        this.color = color;
        positions = new HashSet<>();
        liberties = new HashSet<>();
    }

    public Group(Position point, int color) {
        positions = new HashSet<Position>();
        positions.add(point);
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public Set<Position> getPositions() {
        return positions;
    }

    public void addPosition(Position position) {
        positions.add(position);
    }

    public void add(Group group, Position playedStone) {
        this.positions.addAll(group.positions);
        this.liberties.addAll(group.liberties);
        // remove played stone from liberties
        this.liberties.remove(playedStone);
    }

    public Set<Position> getLiberties() {
        return liberties;
    }

    public void addLiberty(Position liberdade) {
        liberties.add(liberdade);
    }

    public void removeLiberty(Position liberdade) { liberties.remove(liberdade); }

    public boolean isInAtari() {
        return liberties.size() == 1;
    }

    public boolean isCapturedBy(Move move) {
        return move.color != color && isInAtari() && liberties.contains(move.getPosition());
    }

    public boolean hasNoLiberties() {
        return liberties.size() == 0;
    }

    public void die(Board board) {
        for (Position rollingStone : this.positions) {
            rollingStone.setGroup(null);
            Set<Group> adjacentGroups = board.getGroupsAdjacentToNotNull(rollingStone);
            for (Group group : adjacentGroups) {
                group.liberties.add(rollingStone);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Group)) return false;

        Group otherGroup = (Group)other;
        return color == otherGroup.color
            && positions.equals(otherGroup.positions)
            && liberties.equals(otherGroup.liberties);
    }

    @Override
    public int hashCode() {
        return color + positions.hashCode() + liberties.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append("Group of color " + color + ": ");
        for (Position position : positions) {
            string.append(position);
        }
        return string.toString();
    }

}
