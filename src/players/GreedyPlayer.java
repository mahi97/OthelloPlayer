package players;

import game.AbstractPlayer;
import game.BoardSquare;
import game.Move;

import java.util.List;

public class GreedyPlayer extends AbstractPlayer {
    public GreedyPlayer(int depth) {
        super(depth);
    }

    @Override
    public BoardSquare play(int[][] board) {
        List<Move> moves = getGame().getValidMoves(board, getMyBoardMark());

        BoardSquare bestMove = new BoardSquare(-1, -1); // No Move
        int bestMoveMarks = -1;

        for (Move move : moves) {
            int[][] boardCopy = copy(board);
            getGame().do_move(boardCopy, move.getBardPlace(), this);
            if (countMyMarks(boardCopy) > bestMoveMarks) {
                bestMoveMarks = countMyMarks(boardCopy);
                bestMove = move.getBardPlace();
            }
        }

        return bestMove;
    }

    private int[][] copy(int[][] board) {
        board = board.clone();
        for (int i = 0; i < board.length; i++) {
            board[i] = board[i].clone();
        }
        return board;
    }

    private int countMyMarks(int[][] board) {
        int nMarks = 0;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (board[r][c] == getMyBoardMark())
                    nMarks++;
            }
        }
        return nMarks;
    }
}