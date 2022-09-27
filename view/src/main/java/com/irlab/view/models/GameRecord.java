package com.irlab.view.models;

import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.Stack;

public class GameRecord implements Serializable {

    protected final Stack<GameTurn> preceding;

    private final Stack<GameTurn> following;

    public GameRecord(int width, int height) {
        preceding = new Stack<>();
        following = new Stack<>();
        GameTurn first = new GameTurn(width, height);
        apply(first);
    }

    public void apply(GameTurn turn) {
        preceding.push(turn);
        following.clear();
    }

    public boolean hasPreceding() {
        return preceding.size() > 1;
    }

    public int nbrPreceding() { return preceding.size() - 1; }

    public void undo() throws EmptyStackException {
        if (preceding.size() > 1) {
            following.push(preceding.pop());
        } else {
            throw new EmptyStackException();
        }
    }

    public void redo() throws EmptyStackException {
        preceding.push(following.pop());
    }

    public Iterable<GameTurn> getTurns() {
        return preceding;
    }

    public GameTurn getLastTurn() {
        if (!preceding.empty()) {
            return preceding.peek();
        }
        else return new GameTurn(20, 20);
    }

    public void pop() {
        if (!preceding.empty()) preceding.pop();
    }

    public int getSize() {
        return preceding.size();
    }
}
