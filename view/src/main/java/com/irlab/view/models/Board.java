package com.irlab.view.models;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import onion.w4v3xrmknycexlsd.lib.sgfcharm.BuildConfig;

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

    public boolean play(Point point, Player player) {
        // 判断该局部是否是打劫
        boolean ko = false;
        GameTurn currentTurn;

        // 棋子应该在棋盘内
        if (!isInBoard(point)) return false;

        // 棋子不能重叠
        if (point.getGroup() != null) return false;

        // 为判断打劫 要记录吃掉的棋子和吃掉的组
        capturedStones = new HashSet<>();
        Set<Group> capturedGroups = new HashSet<>();

        Set<Group> adjGroups = point.getAdjacentGroups();
        Group newGroup = new Group(point, player);
        point.setGroup(newGroup);
        for (Group group : adjGroups) {
            if (group.getOwner() == player) {
                newGroup.add(group, point);
            } else {
                group.removeLiberty(point);
                if (group.getLiberties().size() == 0) {
                    capturedStones.addAll(group.getStones());
                    capturedGroups.add(new Group(group));
                    group.die();
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
        recordPoints.add(point);
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

    public void takeGameTurn(GameTurn gameTurn, Player one, Player two) {
        this.freeIntersections();
        int[][] boardState = gameTurn.getBoardState();
        for (int x = 1; x <= width; x++) {
            for (int y = 1; y <= height; y++) {
                int state = boardState[x][y];
                if (state == 1) {
                    play(getPoint(x, y), one);
                } else if (state == 2) {
                    play(getPoint(x, y), two);
                }
            }
        }
    }

    public void freeIntersections() {
        for (int i = 1; i < width; i ++ ) {
            for (int j = 1; j < height; j ++ ) {
                Point point = getPoint(i, j);
                point.setGroup(null);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder board = new StringBuilder();
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
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

    public String generateSgf(String blackPlayer, String whitePlayer, String komi) {
        StringBuilder sgf = new StringBuilder();
        writeHeader(sgf, blackPlayer, whitePlayer, komi);
        for (Point move : recordPoints) {
            sgf.append(move.sgf());
        }
        sgf.append(")");
        return sgf.toString();
    }

    private void writeHeader(StringBuilder sgf, String blackPlayer, String whitePlayer, String komi) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String date = sdf.format(new Date(c.getTimeInMillis()));

        sgf.append("(;");
        writeProperty(sgf, "FF", "4");     // SGF version
        writeProperty(sgf, "GM", "1");     // Type of game (1 = Go)
        writeProperty(sgf, "CA", "UTF-8");
        writeProperty(sgf, "SZ", "" + width);
        writeProperty(sgf, "DT", date);
        writeProperty(sgf, "AP", "Kifu Recorder v" + BuildConfig.VERSION_NAME);
        writeProperty(sgf, "KM", komi);
        writeProperty(sgf, "PW", whitePlayer);
        writeProperty(sgf, "PB", blackPlayer);
        writeProperty(sgf, "Z1", "" + recordPoints.size());
    }

    private void writeProperty(StringBuilder sgf, String property, String value) {
        sgf.append(property);
        sgf.append("[");
        sgf.append(value);
        sgf.append("]");
    }
}
