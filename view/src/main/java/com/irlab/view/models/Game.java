package com.irlab.view.models;

import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a complete game, with the sequence of boards and moves that were made.
 */
public class Game implements Serializable {

    private String blackPlayer;
    private String whitePlayer;
    private String komi;
    private int boardDimension;
    private List<Move> moves;
    private List<Board> boards;

    private int numberOfUndoes;

    public Game(int boardDimension, String blackPlayer, String whitePlayer, String komi) {
        this.boardDimension = boardDimension;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.komi = komi;
        moves = new ArrayList<>();
        boards = new ArrayList<>();

        Board emptyBoard = new Board(boardDimension);
        boards.add(emptyBoard);
        numberOfUndoes = 0;
    }

    public void copy(Game game) {
        boardDimension = game.boardDimension;
        blackPlayer = game.blackPlayer;
        whitePlayer = game.whitePlayer;
        komi = game.komi;
        moves = game.moves; // I think there's no problem just copying the reference here
        boards = game.boards;
        numberOfUndoes = game.numberOfUndoes;
    }

    public int getBoardDimension() {
        return boardDimension;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public String getWhitePlayer() {
        return whitePlayer;
    }

    public boolean addMoveIfItIsValid(Board board) {
        // 获得落子的这一步
        Move playedMove = board.getDifferenceTo(getLastBoard());
        // 如果未落子 或者盘面与之前的盘面相同(三劫循环有争议) 或者 非交替落子时 落子无效
        if (playedMove == null || repeatsPreviousState(board) || !canNextMoveBe(playedMove.color)) {
            return false;
        }
        /* ######### 规则判断
        Set<Position> capturedStones = new HashSet<>();
        Set<Group> capturedGroups = new HashSet<>();
        // 先获取这枚棋子的位置
        Position curPosition = playedMove.getPosition();
        // 得到和这个棋子相连的所有组 得到的集合中不包含空组
        Set<Group> adjGroups = board.getGroupsAdjacentToNotNull(curPosition);
        // 为新落的子新创建一个组
        Group curPlayedGroup = new Group(curPosition, playedMove.color);
        // 设置该点的组为新建立的组
        curPosition.setGroup(curPlayedGroup);
        // 遍历所有组 在每个组中将这枚棋子加进去
        for (Group adjGroup : adjGroups) {
            // 只有和自己是同色时才加入该组
            if (adjGroup.getColor() == playedMove.color) {
                // 该组加入棋子
                curPlayedGroup.add(adjGroup, curPosition);
            }
            else {
                //  将这口气移除
                adjGroup.removeLiberty(curPosition);
                // 对手没气
                if (adjGroup.hasNoLiberties()) {
                    // 处理打劫
                    capturedStones.addAll(adjGroup.getPositions());
                    capturedGroups.add(new Group(adjGroup));
                    adjGroup.die(board);
                }
            }
        }
        // Preventing suicide or ko and re-adding liberty
        if (curPlayedGroup.hasNoLiberties()) {
            for (Group group : board.getGroupsAdjacentToNotNull(curPosition)) {
                group.getLiberties().add(curPosition);
            }
            curPosition.setGroup(null);
            return false;
        }

        // Move is valid, applying changes
        for (Position position : curPlayedGroup.getPositions()) {
            position.setGroup(curPlayedGroup);
        }*/
        boards.add(board);
        moves.add(playedMove);
        Log.i("Recorder", "Adding board " + board + " (move " + playedMove.sgf() + ") to the game.");
        return true;
    }

    /**
     * Returns true if newBoard repeats any previous boards of the game (superko rule).
     */
    private boolean repeatsPreviousState(Board newBoard) {
        for (Board board : boards) {
            if (board.equals(newBoard)) return true;
        }
        return false;
    }

    public boolean canNextMoveBe(int color) {
        if (color == Board.BLACK_STONE)
            return isFirstMove() || wereOnlyBlackStonesPlayed() || wasLastMoveWhite();
        else if (color == Board.WHITE_STONE)
            return wasLastMoveBlack();
        return false;
    }

    private boolean isFirstMove() {
        return moves.isEmpty();
    }

    /**
     * This method is used to check if handicap stones are being placed.
     */
    private boolean wereOnlyBlackStonesPlayed() {
        for (Move move : moves) {
            if (move.color == Board.WHITE_STONE) return false;
        }
        return true;
    }

    private boolean wasLastMoveWhite() {
        return !isFirstMove() && getLastMove().color == Board.WHITE_STONE;
    }

    private boolean wasLastMoveBlack() {
        return !isFirstMove() && getLastMove().color == Board.BLACK_STONE;
    }

    public Move getLastMove() {
        if (moves.isEmpty()) return null;
        return moves.get(moves.size() - 1);
    }

    public Board getLastBoard() {
        return boards.get(boards.size() - 1);
    }

    // 不考虑最后一步
    public Move undoLastMove() {
        if (moves.isEmpty()) return null;
        boards.remove(boards.size() - 1);
        Move lastMove = moves.remove(moves.size() - 1);
        numberOfUndoes++;
        return lastMove;
    }

    public int getNumberOfMoves() {
        return moves.size();
    }

    // 旋转
    public void rotate(int direction) {
        if (direction != -1 && direction != 1) return;

        List<Board> rotatedBoards = new ArrayList<>();
        for (Board board : boards) {
            rotatedBoards.add(board.rotate(direction));
        }

        List<Move> rotatedMoves = new ArrayList<>();
        for (int i = 1; i < rotatedBoards.size(); ++i) {
            Board last = rotatedBoards.get(i);
            Board secondToLast = rotatedBoards.get(i - 1);
            rotatedMoves.add(last.getDifferenceTo(secondToLast));
        }

        boards = rotatedBoards;
        moves = rotatedMoves;
    }

     // SGF methods should be extracted to a SgfBuilder class that receives a Game as parameter
     // 输出文件到sgf Reference: http://www.red-bean.com/sgf/
    public String sgf() {
        StringBuilder sgf = new StringBuilder();
        writeHeader(sgf);
        for (Move move : moves) {
            Log.i("Recorder", "Building SGF - move " + move.sgf());
            sgf.append(move.sgf());
        }
        sgf.append(")");
        return sgf.toString();
    }

    private void writeHeader(StringBuilder sgf) {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String date = sdf.format(new Date(c.getTimeInMillis()));

        sgf.append("(;");
        writeProperty(sgf, "FF", "4");     // SGF version
        writeProperty(sgf, "GM", "1");     // Type of game (1 = Go)
        writeProperty(sgf, "CA", "UTF-8");
        writeProperty(sgf, "SZ", "" + getLastBoard().getDimension());
        writeProperty(sgf, "DT", date);
        writeProperty(sgf, "KM", komi);
        writeProperty(sgf, "PW", whitePlayer);
        writeProperty(sgf, "PB", blackPlayer);
        writeProperty(sgf, "Z1", "" + getNumberOfMoves());
        writeProperty(sgf, "Z2", "" + numberOfUndoes);
    }

    private void writeProperty(StringBuilder sgf, String property, String value) {
        sgf.append(property);
        sgf.append("[");
        sgf.append(value);
        sgf.append("]");
    }

}
