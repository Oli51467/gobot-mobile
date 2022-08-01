package com.irlab.view.models;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

// 棋盘
public class Board implements Serializable {
    public final static int EMPTY = 0;
    public final static int BLACK_STONE = 1;
    public final static int WHITE_STONE = 2;

    private final int width;
    private final int height;
    public final Point[][] points;
    private final int initialHandicap;
    private final GameRecord gameRecord;

    private Player P1, P2, actualPlayer;
    private int handicap;

    public Board(int width, int height, int handicap) {
        this.width = width;
        this.height = height;
        this.initialHandicap = handicap;
        this.points = new Point[width][height];
        this.gameRecord = new GameRecord(width, height, handicap);
        initBoard();
    }

    private void initBoard() {
        // 初始化对局双方
        P1 = new Player(1);
        P2 = new Player(2);
        actualPlayer = P1;

        // 初始化棋盘
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                points[x][y] = new Point(this, x, y);
            }
        }
        handicap = 0;
    }

    public boolean isInBoard(int x, int y) {
        return (x >= 0 && x < width && y >= 0 && y < height);
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

    public boolean play(Point point, Player player) {
        // 判断该局部是否是打劫
        boolean ko = false;
        GameTurn currentTurn;

        // 棋子应该在棋盘内
        if (!isInBoard(point)) return false;

        // 棋子不能重叠
        if (point.getGroup() != null) return false;

        // 为判断打劫 要记录吃掉的棋子和吃掉的组
        Set<Point> capturedStones = new HashSet<>();
        Set<Group> capturedGroups = new HashSet<>();

        Set<Group> adjGroups = point.getAdjacentGroups();
        Group newGroup = new Group(point, player);
        point.setGroup(newGroup);
        for (com.irlab.view.models.Group Group : adjGroups) {
            if (Group.getOwner() == player) {
                newGroup.add(Group, point);
            } else {
                Group.removeLiberty(point);
                if (Group.getLiberties().size() == 0) {
                    capturedStones.addAll(Group.getStones());
                    capturedGroups.add(new Group(Group));
                    Group.die();
                }
            }
        }

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
        gameRecord.apply(currentTurn);
        return true;
    }

    public boolean play(int x, int y, Player player) {
        Point point = getPoint(x, y);
        if (point == null) {
            System.out.println("落子超出棋盘范围了 请重新落子！");
            return false;
        }
        return play(point, player);
    }

    public Player getPlayer() {
        return actualPlayer;
    }

    public Player getLastPlayer() {
        if (actualPlayer == P1) return P2;
        else return P1;
    }

    public boolean nextPlayer() {
        return changePlayer();
    }

    public boolean changePlayer() {
        if (handicap < initialHandicap) {
            handicap++;
            return false;
        } else {
            if (actualPlayer == P1) {
                actualPlayer = P2;
                System.out.println("该白棋下啦");
            } else {
                actualPlayer = P1;
                System.out.println("该黑棋下啦");
            }
            return true;
        }
    }

    @Override
    public String toString() {
        String board = "";
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Point cross = points[i][j];
                if (cross.getGroup() == null) {
                    board += "· ";
                } else {
                    board += (cross.getGroup().getOwner().getIdentifier() == 1 ? '1' : '2') + " ";
                }
            }
            board += "\n";
        }
        return board;
    }
}
