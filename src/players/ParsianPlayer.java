package players;

import game.AbstractPlayer;
import game.BoardSquare;
import game.Move;
import game.OthelloGame;
import javafx.util.Pair;

import java.math.BigInteger;
import java.util.List;
import java.util.Vector;

public class ParsianPlayer extends AbstractPlayer {

    public ParsianPlayer(int depth) {
        super(depth);
    }

    @Override
    public BoardSquare play(int[][] tab) {
        int[][] a = BNS(tab, 0, 1);
        return null;
    }

    public int[][] BNS(int[][] node, double alpha, double beta) {
        OthelloGame othelloGame = new OthelloGame();
        List<Move> moveList = othelloGame.getValidMoves(node, getMyBoardMark());
        int betterCounter = 0;
//        not((β - α < 2) or (betterCount = 1))
//        test := NextGuess(α, β, subtreeCount)
//        betterCount := 0
//        foreach child of node
//        bestVal := -AlphaBeta(child, -test, -(test - 1))
//        if bestVal ≥ test
//        betterCount := betterCount + 1
//        bestNode := child
//        //update number of sub-trees that exceeds separation test value
//        //update alpha-beta range
        while (!((beta - alpha < 2) || betterCounter == 1)) {

        }
        return null;
    }

    private double eval(int[][] node) {
        double diff = pieceDiff(node);
        double cor_occ = cornerOccupancy(node);
        double cor_close = cornerCloseness(node);
        double mob = mobility(node);
        double frontier = frontierDices(node);
        double disc = disc_squares(node);

        double evaluate = 10 * diff + 801.724 * cor_occ + 382.026 * cor_close + 78.922 * mob + 74.396 * frontier + 10 * disc;

        return evaluate;
    }

    private double pieceDiff(int[][] node) {
        int B_piece = 0, R_piece = 0;

        double diff;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] == getMyBoardMark()) B_piece++;
                else if (node[i][j] == getBoardMark()) R_piece++;
            }
        }

        if (B_piece > R_piece) {
            diff = 100 * ((double) B_piece / (B_piece + R_piece));
        } else if (B_piece < R_piece) {
            diff = -100 * ((double) R_piece / (B_piece + R_piece));
        } else {
            diff = 0;
        }

        return diff;
    }

    private double cornerOccupancy(int[][] node) {
        int B_cor = 0, R_cor = 0;

        if (node[0][0] == getMyBoardMark()) {
            B_cor++;
        } else if (node[0][0] == getBoardMark()) {
            R_cor++;
        }

        if (node[0][7] == getMyBoardMark()) {
            B_cor++;
        } else if (node[0][7] == getBoardMark()) {
            R_cor++;
        }

        if (node[7][0] == getMyBoardMark()) {
            B_cor++;
        } else if (node[7][0] == getBoardMark()) {
            R_cor++;
        }

        if (node[7][7] == getMyBoardMark()) {
            B_cor++;
        } else if (node[7][7] == getBoardMark()) {
            R_cor++;
        }

        return 25 * (B_cor - R_cor);
    }

    private double cornerCloseness(int[][] node) {
        int B_cor_close = 0, R_cor_close = 0;

        //// up left
        if (node[0][0] == 0) {
            if (node[0][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[0][1] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[1][0] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][0] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[1][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][1] == getBoardMark()) {
                R_cor_close++;
            }
        }

        //// up right
        if (node[0][7] == 0) {
            if (node[0][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[0][6] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[1][7] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][7] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[1][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][6] == getBoardMark()) {
                R_cor_close++;
            }
        }

        //// down left
        if (node[7][0] == 0) {
            if (node[6][0] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][0] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[7][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[7][1] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[6][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][1] == getBoardMark()) {
                R_cor_close++;
            }
        }

        //// down right
        if (node[7][7] == 0) {
            if (node[6][7] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][7] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[6][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][6] == getBoardMark()) {
                R_cor_close++;
            }

            if (node[7][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[7][6] == getBoardMark()) {
                R_cor_close++;
            }
        }

        return 6.25 * (R_cor_close - B_cor_close);

    }

    private double mobility(int[][] node) {
        OthelloGame g = new OthelloGame();
        int B_moves = g.getValidMoves(node, 1).size();
        int R_moves = g.getValidMoves(node, 2).size();

        double mob;

        if (B_moves > R_moves) {
            mob = 100 * ((double) B_moves / (B_moves + R_moves));
        } else if (B_moves < R_moves) {
            mob = -100 * ((double) R_moves / (B_moves + R_moves));
        } else {
            mob = 0;
        }

        return mob;
    }

    private double frontierDices(int[][] node) {
        double front = 0;

        int B_front = 0, R_front = 0;
        boolean found = false;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] != 0) {
                    for (int dx = -1; dx < 2; dx++) {
                        for (int dy = -1; dy < 2; dy++) {
                            if ((dx != 0 || dy != 0) && (i + dx > -1 && i + dx < 8) && (j + dy > -1 && j + dy < 8)) {
                                if (node[i + dx][j + dy] == 0) {
                                    if (node[i][j] == getMyBoardMark()) {
                                        B_front++;
                                    } else {
                                        R_front++;
                                    }
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            found = false;
                            break;
                        }
                    }
                }
            }
        }

        if (B_front > R_front) {
            front = 100 * ((double) B_front / (B_front + R_front));
        } else if (B_front < R_front) {
            front = -100 * ((double) R_front / (B_front + R_front));
        } else {
            front = 0;
        }

        return front;
    }

    private double disc_squares(int[][] node) {
        int[][] v = {{20, -3, 11, 8, 8, 11, -3, 20},
                {-3, -7, -4, 1, 1, -4, -7, -3},
                {11, -4, 2, 2, 2, 2, -4, 11},
                {8, 1, 2, -3, -3, 2, 1, 8}};


        double res = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] == getMyBoardMark()) {
                    res += v[i][j];
                } else if (node[i][j] == getMyBoardMark()) {
                    res -= v[i][j];
                }
            }
        }

        return res;

    }

    private int zobristHash(int[][] node) {
        BigInteger big, res;

        Vector<Integer> val = new Vector<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] != 0) {
                    val.add(node[i][j] * (8 * i + j + 1));
                }
            }
        }
        int a, result;
        a = val.elementAt(0);
        result = val.elementAt(1);
        result = result ^ a;
        for (int i = 2; i < val.size(); i++) {
            a = val.elementAt(i);
            result = result ^ a;
        }

        return result;
    }
}