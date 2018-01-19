package players;

import game.AbstractPlayer;
import game.BoardSquare;
import game.Move;
import game.OthelloGame;
import javafx.scene.input.KeyCode;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

enum GameStage {
    NO_STAGE,
    OPENING,
    EARLY,
    MID,
    PRE_END,
    END
}

public class ParsianPlayer extends AbstractPlayer {

    private HashMap<Entity, Double> transportTable;
    private GameStage gameStage;

    private int playCounter;
    private Move best;

    public ParsianPlayer(int depth) {
        super(depth);
        best = null;
        transportTable = new HashMap<>();
        gameStage = GameStage.NO_STAGE;

        playCounter = 0;
    }

    @Override
    public BoardSquare play(int[][] tab) {
        OthelloGame jogo = new OthelloGame();
        playCounter++;
        evalGameState();
        System.out.println("Game Stage : " + gameStage.name());
        best = null;
        double f;
        System.out.println("Branch: " + jogo.getValidMoves(tab, getMyBoardMark()).size());
        System.out.println("Branch: " + jogo.getValidMoves(tab, getOpponentBoardMark()).size());

        if (getMyBoardMark() == 1 || true) f = alphaBeta(new Entity(tab), 0, 1,getDepth(), false);
//        else
//            f = BNS(new Entity(tab), -Double.MAX_VALUE + 1, Double.MAX_VALUE);
        System.out.println("Point : " + f);
        if (best == null) {
            System.out.println("MISSED" + jogo.getValidMoves(tab, getMyBoardMark()) + "  " + getMyBoardMark());
            return new BoardSquare(-1,-1);
        }
        System.out.println(best.getBardPlace().toString());
        return best.getBardPlace();

    }

    public void evalGameState() {
        if (playCounter < 5) {
            gameStage = GameStage.OPENING;
        } else if (playCounter < 10) {
            gameStage = GameStage.EARLY;
        } else if (playCounter < 22.5) {
            gameStage = GameStage.MID;
        } else if (playCounter < 10) {
            gameStage = GameStage.PRE_END;
        } else {
            gameStage = GameStage.END;
        }

    }

/**************** SEARCH *****************/
    public double BNS(Entity node, double alpha, double beta) {
        OthelloGame othelloGame = new OthelloGame();
        List<Move> moveList = othelloGame.getValidMoves(node.getKey(), getMyBoardMark());
        int betterCounter = 0;
        Move bestNode = null;
        double bestVal = 0;
        do {
            double test = 0.0; // next Guess
            betterCounter = 0;
            for(Move m : moveList) {

                bestVal = -alphaBetaWithMemory(node, -test, -test + 1, getDepth(), true);
                if (bestVal >= test) {
                    betterCounter += 1;
                    bestNode = m;
                }

            }
            //        //update number of sub-trees that exceeds separation test value
            //        //update alpha-beta range

            if (bestVal > alpha) {
                alpha = bestVal;
            }
            if (bestVal < beta) {
                beta = bestVal;
            }
        } while (!((beta - alpha < 2) || betterCounter == 1));
        assert bestNode != null;
        return bestVal;
    }

    public double MTDF(Entity root, double firstGuess, int depth) {
        double goal = firstGuess;
        double upperBound = Double.MAX_VALUE;
        double lowerBound = -Double.MAX_VALUE + 1;
        while (lowerBound < upperBound) {
            double beta = Math.max(goal, lowerBound + 1);
            goal = alphaBeta(root, beta - 1, beta, depth, true);
            System.out.println("Goal " + goal);
            if (goal < beta) {
                upperBound = goal;
            } else {
                lowerBound = goal;
            }
        }
        return goal;
    }

    public double alphaBeta(Entity root, double alpha, double beta, int depth, boolean withMemory){
        if (withMemory) {
            return alphaBetaWithMemory(root, -Double.MAX_VALUE + 1, Double.MAX_VALUE, depth, true);
        } else {
            return alphaBetaCore(root, -Double.MAX_VALUE + 1, Double.MAX_VALUE, depth, true);
        }
    }

    public double alphaBetaWithMemory(Entity root, double alpha, double beta, int depth, boolean maxPlayer) {
        OthelloGame othelloGame = new OthelloGame();
        double v;
        if (depth == 0) {
            double d;
            if (transportTable.containsKey(root)) {
                d = transportTable.get(root);
                return d;
            } else {
                double e = eval(root.getKey(), false);
                transportTable.put(root, e);
                return e;
            }
        }
        if (endGame(root.getKey(), maxPlayer)) {
            double e = eval(root.getKey(), true);
            return e;
        }
        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            for (Move m : othelloGame.getValidMoves(root.getKey(), getMyBoardMark())) {
                Entity e = new Entity(m.getBoard());
                e.setMove(m);
                alpha = Math.max(alpha, alphaBetaWithMemory(new Entity(m.getBoard()), alpha, beta, depth - 1, false));
                if (alpha > v) {
                    v = alpha;
                    if (depth == getDepth()) best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v = Double.MAX_VALUE;
            for (Move m : othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark())) {
                Entity e = new Entity(m.getBoard());
                e.setMove(m);
                beta = Math.min(beta, alphaBetaWithMemory(e, alpha, beta, depth - 1, true));
                if (beta < v) {
                    v = beta;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        }

        return v;
    }

    private double alphaBetaCore(Entity root, double alpha, double beta, int depth, boolean maxPlayer) {
        OthelloGame othelloGame = new OthelloGame();
        double v;
        if (depth == 0) {
            double a = eval(root.getKey(), false);
            return a;
        }
        if (endGame(root.getKey(), maxPlayer)) {
            double a = eval(root.getKey(), true);
            return a;
        }
        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            for (Move m : othelloGame.getValidMoves(root.getKey(), getMyBoardMark())) {

                Entity e = new Entity(m.getBoard());
                e.setMove(m);
                alpha = Math.max(alpha, alphaBetaCore(e, alpha, beta, depth - 1, false));
                if (alpha > v) {
                    v = alpha;
                    if (depth == getDepth()) best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v = Double.MAX_VALUE;
            for (Move m : othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark())) {
                Entity e = new Entity(m.getBoard());
                e.setMove(m);
                beta = Math.min(beta, alphaBetaCore(e, alpha, beta, depth - 1, true));
                if (beta < v) {
                    v = beta;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        }

        return v;
    }

    private boolean endGame(int[][] tab, boolean maxPlayer) {
        OthelloGame othelloGame = new OthelloGame();
        if (maxPlayer) {
            return othelloGame.noSpace(tab)
                    || othelloGame.getValidMoves(tab, getMyBoardMark()).size() == 0;
        } else {
            return othelloGame.noSpace(tab)
                    || othelloGame.getValidMoves(tab, getOpponentBoardMark()).size() == 0;
        }
    }

/**************** END SEARCH *****************/

/**************** EVALUATION *****************/

    private double eval(int[][] node, boolean end) {
        double diff = pieceDiff(node);

        if(end)
            return diff;

        switch (gameStage) {
            case OPENING:
                break;
            case EARLY:
                break;
            case MID:
                break;
            case PRE_END:
                break;
            default:

        }
        double cor_occ = cornerOccupancy(node);
        double cor_close = cornerCloseness(node);
        double mob = mobility(node);
        double frontier = frontierDices(node);
        double disc = disc_squares(node);

        double evaluate = 10 * diff
                + 801.724 * cor_occ
                + 382.026 * cor_close
                + 78.922 * mob
                + 74.396 * frontier
                + 10 * disc;

        return evaluate;
    }

    private double pieceDiff(int[][] node) {
        int B_piece = 0, R_piece = 0;

        double diff;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] == getMyBoardMark()) B_piece++;
                else if (node[i][j] == getOpponentBoardMark()) R_piece++;
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
        } else if (node[0][0] == getOpponentBoardMark()) {
            R_cor++;
        }

        if (node[0][7] == getMyBoardMark()) {
            B_cor++;
        } else if (node[0][7] == getOpponentBoardMark()) {
            R_cor++;
        }

        if (node[7][0] == getMyBoardMark()) {
            B_cor++;
        } else if (node[7][0] == getOpponentBoardMark()) {
            R_cor++;
        }

        if (node[7][7] == getMyBoardMark()) {
            B_cor++;
        } else if (node[7][7] == getOpponentBoardMark()) {
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
            } else if (node[0][1] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[1][0] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][0] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[1][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][1] == getOpponentBoardMark()) {
                R_cor_close++;
            }
        }

        //// up right
        if (node[0][7] == 0) {
            if (node[0][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[0][6] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[1][7] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][7] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[1][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[1][6] == getOpponentBoardMark()) {
                R_cor_close++;
            }
        }

        //// down left
        if (node[7][0] == 0) {
            if (node[6][0] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][0] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[7][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[7][1] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[6][1] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][1] == getOpponentBoardMark()) {
                R_cor_close++;
            }
        }

        //// down right
        if (node[7][7] == 0) {
            if (node[6][7] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][7] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[6][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][6] == getOpponentBoardMark()) {
                R_cor_close++;
            }

            if (node[7][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[7][6] == getOpponentBoardMark()) {
                R_cor_close++;
            }
        }

        return 6.25 * (R_cor_close - B_cor_close);

    }

    private double mobility(int[][] node) {
        OthelloGame g = new OthelloGame();
        int B_moves = g.getValidMoves(node, getMyBoardMark()).size();
        int R_moves = g.getValidMoves(node, getOpponentBoardMark()).size();

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
        int[][] v = {
                {20, -3, 11,  8,  8, 11, -3, 20},
                {-3, -7, -4,  1,  1, -4, -7, -3},
                {11, -4,  2,  2,  2,  2, -4, 11},
                { 8,  1,  2, -3, -3,  2,  1,  8},
                { 8,  1,  2, -3, -3,  2,  1,  8},
                {11, -4,  2,  2,  2,  2, -4, 11},
                {-3, -7, -4,  1,  1, -4, -7, -3},
                {20, -3, 11,  8,  8, 11, -3, 20}
        };


        double res = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] == getMyBoardMark()) {
                    res += v[i][j];
                } else if (node[i][j] == getOpponentBoardMark()) {
                    res -= v[i][j];
                }
            }
        }

        return res;

    }

    private double stability(int node[][]){
        return 0;
    }


    /**************** END EVALUATION *****************/

}


class Entity {
    Entity(int[][] _node){
        key = _node;
    }
    Entity(int[][] _node, Move _move){
        key = _node;
        move = _move;
    }
    private int[][] key;
    private Move move;

    public void setMove(Move move) {
        this.move = move;
    }

    public Move getMove() {
        return move;
    }

    public void setKey(int[][] key) {
        this.key = key;
    }

    public int[][] getKey() {
        return key;
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

    @Override
    public int hashCode() { // TODO : Change it to zorbist
        return zobristHash(key);
    }
}