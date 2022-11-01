package com.irlab.view.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 棋盘
public class Board implements Serializable {
    public final static int BLACK_STONE = 1;
    public final static int WHITE_STONE = 2;

    private final int width;
    private final int height;
    public final Point[][] points;
    public List<Point> recordPoints;
    public Set<Point> capturedStones;
    private final int initialHandicap;
    public final GameRecord gameRecord;

    private Player P1, P2, actualPlayer;
    private int handicap;

    public Board(int width, int height, int handicap) {
        this.width = width;
        this.height = height;
        this.initialHandicap = handicap;
        this.points = new Point[width + 1][height + 1];
        this.gameRecord = new GameRecord(width, height);
        this.recordPoints = new ArrayList<>();
        initBoard();
    }

    private void initBoard() {
        // 初始化对局双方
        P1 = new Player(1);
        P2 = new Player(2);
        actualPlayer = P1;

        // 初始化棋盘
        for (int x = 1; x <= this.width; x++) {
            for (int y = 1; y <= this.height; y++) {
                points[x][y] = new Point(this, x, y);
            }
        }
        handicap = 0;
    }

    public boolean isInBoard(int x, int y) {
        return (x > 0 && x < width && y > 0 && y < height);
    }

    public boolean isInBoard(Point Point) {
        int x = Point.getX();
        int y = Point.getY();
        return isInBoard(x, y);
    }

    public Point getPoint(int x, int y) {
        if (isInBoard(x, y)) {
            return points[x][y];
        } else {
            return null;
        }
    }

    public int getHandicap() {
        return initialHandicap;
    }

    public boolean play(Point point, Player player, boolean handleKo) {
        GameTurn currentTurn = null;
        boolean ko = false;

        // 棋子应该在棋盘内
        if (!isInBoard(point)) return false;

        // 棋子不能重叠
        if (point.getGroup() != null) return false;

        // 为判断打劫 要记录吃掉的棋子和吃掉的组
        Set<Point> capturedStones = null;
        Set<Group> capturedGroups = null;
        if (handleKo) {
            capturedStones = new HashSet<>();
            capturedGroups = new HashSet<>();
        }

        Set<Group> adjGroups = point.getAdjacentGroups();
        Group newGroup = new Group(point, player);
        point.setGroup(newGroup);
        for (Group group : adjGroups) {
            if (group.getOwner() == player) {
                newGroup.add(group, point);
            } else {
                group.removeLiberty(point);
                if (group.getLiberties().size() == 0) {
                    if (handleKo) {
                        capturedStones.addAll(group.getStones());
                        capturedGroups.add(new Group(group));
                    }
                    group.die();
                }
            }
        }

        if (handleKo) {
            currentTurn = gameRecord.getLastTurn().toNext(point.getX(), point.getY(), player.getIdentifier(), getHandicap(), capturedStones);
            for (GameTurn turn : gameRecord.getTurns()) {
                if (turn.equals(currentTurn)) {
                    ko = true;
                    break;
                }
            }
            // 判断打劫
            if (ko) {
                for (Group chain : capturedGroups) {
                    chain.getOwner().removeCapturedStones(chain.getStones().size());
                    for (Point stone : chain.getStones()) {
                        stone.setGroup(chain);
                    }
                }
            }
        }

        // 不能自杀
        if (newGroup.getLiberties().size() == 0 || ko) {
            for (Group chain : point.getAdjacentGroups()) {
                chain.getLiberties().add(point);
            }
            point.setGroup(null);
            return false;
        }
        // 落子有效
        for (Point stone : newGroup.getStones()) {
            stone.setGroup(newGroup);
        }
        if (handleKo) {
            gameRecord.apply(currentTurn);
        }
        recordPoints.add(point);
        return true;
    }

    public boolean play(int x, int y, Player player) {
        Point point = getPoint(x, y);
        if (point == null) {
            System.out.println("落子超出棋盘范围了 请重新落子！");
            return false;
        }
        return play(point, player, true);
    }

    public Player getPlayer() {
        return actualPlayer;
    }

    public boolean nextPlayer() {
        return changePlayer(false);
    }

    public boolean precedentPlayer() {
        return changePlayer(true);
    }

    public boolean changePlayer(boolean undo) {
        if (handicap < initialHandicap && !undo) {
            handicap++;
            return false;
        } else if (undo && this.gameRecord.nbrPreceding() < initialHandicap) {
            handicap--;
            return false;
        } else {
            if (actualPlayer == P1) {
                actualPlayer = P2;
                System.out.println("Changing player for P2");
            } else {
                actualPlayer = P1;
                System.out.println("Changing player for P1");
            }
            return true;
        }
    }

    public boolean undo() {
        if (gameRecord.hasPreceding()) {
            GameTurn current = gameRecord.getLastTurn();
            gameRecord.undo();
            GameTurn last = gameRecord.getLastTurn();
            takeGameTurn(last, P1, P2);
            actualPlayer.removeCapturedStones(current.getCountCapturedStones());
            precedentPlayer();
            return true;
        } else {
            return false;
        }
    }

    public boolean redo() {
        if (gameRecord.hasFollowing()) {
            gameRecord.redo();
            GameTurn next = gameRecord.getLastTurn();
            takeGameTurn(next, P1, P2);
            nextPlayer();
            actualPlayer.addCapturedStones(gameRecord.getLastTurn().getCountCapturedStones());
            return true;
        } else {
            return false;
        }
    }

    public void takeGameTurn(GameTurn gameTurn, Player one, Player two) {
        this.freeIntersections();
        int[][] boardState = gameTurn.getBoardState();
        for (int x = 1; x <= width; x++) {
            for (int y = 1; y <= height; y++) {
                int state = boardState[x][y];
                if (state == BLACK_STONE) {
                    play(getPoint(x, y), one, false);
                } else if (state == WHITE_STONE) {
                    play(getPoint(x, y), two, false);
                }
            }
        }
    }

    public void freeIntersections() {
        for (int i = 1; i < width; i++) {
            for (int j = 1; j < height; j++) {
                Point point = getPoint(i, j);
                point.setGroup(null);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder board = new StringBuilder();
        for (int i = 1; i < width; i++) {
            for (int j = 1; j < height; j++) {
                Point cross = points[i][j];
                if (cross.getGroup() == null) {
                    board.append("· ");
                } else {
                    board.append(cross.getGroup().getOwner().getIdentifier() == 1 ? '1' : '2').append(" ");
                }
            }
            board.append("\n");
        }
        return board.toString();
    }
}
