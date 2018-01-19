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

    private Move killerMove;

    // MPC
    static int MAX_STAGE = 2;
    static int MAX_HEIGHT = 10;
    static int NUM_TRY = 2;
    class Param{
        public Param() {

        }
        int d; // shallow depth
        double t; // threshold
        double a,b,s; // slope, offset, std.dev
    }
    private Param[][][] params;


    public ParsianPlayer(int depth) {
        super(depth);
        best = null;
        transportTable = new HashMap<>();
        gameStage = GameStage.NO_STAGE;
        playCounter = 0;
        params = new Param[MAX_STAGE+1][MAX_HEIGHT+1][NUM_TRY];
        params[0][0][0].a = 2;
        for (int i = 0; i < params.length; i++) {
            for (int j = 0; j < params[i].length; j++) {
                for (int k = 0; k < params[i][j].length; k++) {
                    params[i][j][k].d = 4;
                    params[i][j][k].t = 1;
                    params[i][j][k].a = 1;
                    params[i][j][k].b = 1;
                    params[i][j][k].s = 1;
                }
            }
        }

    }

    @Override
    public BoardSquare play(int[][] tab) {
        OthelloGame jogo = new OthelloGame();
        playCounter++;
        evalGameState();
        System.out.println("Game Stage : " + gameStage.name());
        best = null;
        double f;

        if (killerCondition(false, tab) && getMyBoardMark() == 1) return killerMove.getBardPlace();

        System.out.println("Branch: " + jogo.getValidMoves(tab, getMyBoardMark()).size());
        System.out.println("Branch: " + jogo.getValidMoves(tab, getOpponentBoardMark()).size());

        if (getMyBoardMark() == 1) f = alphaBeta(new Entity(tab), 0, 1,getDepth(), true);
        else f= MPC(new Entity(tab), -Double.MAX_VALUE, Double.MAX_VALUE, getDepth(), true); //f = alphaBeta(new Entity(tab), 0, 1,getDepth(), false);//f = MTDF(new Entity(tab), 5000, getDepth());

//            f = BNS(new Entity(tab), -Double.MAX_VALUE + 1, Double.MAX_VALUE);
        System.out.println("Point : " + f);
        if (best == null) {
            System.out.println("MISSED" + jogo.getValidMoves(tab, getMyBoardMark()) + "  " + getMyBoardMark());
            return new BoardSquare(-1, -1);
        }
        System.out.println(best.getBardPlace().toString());
        return best.getBardPlace();

    }


    private boolean killerCondition(boolean both, int[][] tab) {
        OthelloGame othelloGame = new OthelloGame();
        // Corner Killer
        for (Move m : othelloGame.getValidMoves(tab, getMyBoardMark())) {
            if (m.getBardPlace().getCol() == 0 || m.getBardPlace().getCol() == 7) {
                if (m.getBardPlace().getRow() == 0 || m.getBardPlace().getRow() == 7) {
                    killerMove = m;
                    return true;
                }
            }
        }
        if (both) {
            for (Move m : othelloGame.getValidMoves(tab, getOpponentBoardMark())) {
                if (m.getBardPlace().getCol() == 0 || m.getBardPlace().getCol() == 7) {
                    if (m.getBardPlace().getRow() == 0 || m.getBardPlace().getRow() == 7) {
                        killerMove = m;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void evalGameState() {
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
            for (Move m : moveList) {

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

    public double alphaBeta(Entity root, double alpha, double beta, int depth, boolean withMemory) {
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
                System.out.println("Ha HA :" + root.hashCode() + " - " + d);
                return d;
            } else {
                double e = eval(root.getKey(), false);
//                addRotationsToTable(root, e);
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

    public double MPC(Entity tab, double alpha, double beta, int height, boolean maxPlayer) {
        if (height <= MAX_HEIGHT) {
            for (int i = 0; i < NUM_TRY; i++) {
                double bound;
                Param pa = params[gameStage.ordinal()][height][i];
                if (pa.d < 0) break;

                bound = Math.round((pa.t*pa.s + beta - pa.b)/pa.a);
                if (alphaBeta(tab, bound - 1, bound, pa.d, false) >= bound) {
                    return beta;
                }

                bound = Math.round((-pa.t*pa.s + alpha - pa.b)/pa.a);
                if (alphaBeta(tab, bound, bound + 1, pa.d, false) <= bound) {
                    return alpha;
                }
            }
        }

        OthelloGame othelloGame = new OthelloGame();
        double v;
        if (height == 0) {
            return eval(tab.getKey(), false);
        }
        if (endGame(tab.getKey(), maxPlayer)) {
            return eval(tab.getKey(), true);
        }
        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            for (Move m : othelloGame.getValidMoves(tab.getKey(), getMyBoardMark())) {

                Entity e = new Entity(m.getBoard());
                e.setMove(m);
                alpha = Math.max(alpha, MPC(e, alpha, beta, height - 1, false));
                if (alpha > v) {
                    v = alpha;
                    if (height == getDepth()) best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v = Double.MAX_VALUE;
            for (Move m : othelloGame.getValidMoves(tab.getKey(), getOpponentBoardMark())) {
                Entity e = new Entity(m.getBoard());
                e.setMove(m);
                beta = Math.min(beta, MPC(e, alpha, beta, height - 1, true));
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

/**************** END SEARCH *****************/

    /**************** EVALUATION *****************/

    private double eval(int[][] node, boolean end) {
        double diff = pieceDiff(node);
        if (end) return diff*10
//                + cor_occ * 801.724
//                + cor_close * 382.026
        ;
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
                {20, -3, 11, 8, 8, 11, -3, 20},
                {-3, -7, -4, 1, 1, -4, -7, -3},
                {11, -4, 2, 2, 2, 2, -4, 11},
                {8, 1, 2, -3, -3, 2, 1, 8},
                {8, 1, 2, -3, -3, 2, 1, 8},
                {11, -4, 2, 2, 2, 2, -4, 11},
                {-3, -7, -4, 1, 1, -4, -7, -3},
                {20, -3, 11, 8, 8, 11, -3, 20}
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


    private double stablity(int[][] tab) {
        boolean[][] newList = new boolean[8][8];
        boolean[][] newListR = new boolean[8][8];
        boolean[][] oldList = new boolean[8][8];
        boolean[][] oldListR = new boolean[8][8];

        do {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (tab[i][j] == getMyBoardMark()) {
                        if (i == 0 && j == 0) newList[i][j] = true;
                        if (i == 0 && j == 7) newList[i][j] = true;
                        if (i == 7 && j == 7) newList[i][j] = true;
                        if (i == 7 && j == 0) newList[i][j] = true;

//                        if (leftS(i,j))

                    } else if (tab[i][j] == getOpponentBoardMark()){
                        if (i == 0 && j == 0) newListR[i][j] = true;
                        if (i == 0 && j == 7) newListR[i][j] = true;
                        if (i == 7 && j == 7) newListR[i][j] = true;
                        if (i == 7 && j == 0) newListR[i][j] = true;

                    }


                }
            }
        }while (false);
        return 0.0;
    }




    /**************** END EVALUATION *****************/

    private void addRotationsToTable(Entity ent, double eval) {
        Entity ent90, ent180, ent270;

        int [][] tmp1 = new int [8][8];
        int [][] tmp2 = new int [8][8];
        int [][] tmp3 = new int [8][8];
        for (int i = 0; i < tmp1.length; i++) {
            for (int j = 0; j < tmp1[0].length; j++) {
                tmp1[i][j] = ent.getKey()[i][j];
                tmp2[i][j] = ent.getKey()[i][j];
                tmp3[i][j] = ent.getKey()[i][j];
            }
        }

        rotateByNinetyToLeft(tmp1);
        ent90 = new Entity(tmp1);

        rotateByNinetyToLeft(tmp2);
        rotateByNinetyToLeft(tmp2);
        ent180 = new Entity(tmp1);

        rotateByNinetyToRight(tmp3);
        ent270 = new Entity(tmp1);

        transportTable.put(ent, eval);
        transportTable.put(ent90, eval);
        transportTable.put(ent180, eval);
        transportTable.put(ent270, eval);
    }

    private static void transpose(int[][] m) {
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                int x = m[i][j];
                m[i][j] = m[j][i];
                m[j][i] = x;
            }
        }
    }

    private static void swapRows(int[][] m) {
        for (int i = 0, k = m.length - 1; i < k; ++i, --k) {
            int[] x = m[i];
            m[i] = m[k];
            m[k] = x;
        }
    }

    private static void rotateByNinetyToLeft(int[][] m) {
        transpose(m);
        swapRows(m);
    }

    private static void rotateByNinetyToRight(int[][] m) {
        swapRows(m);
        transpose(m);
    }
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
        int[][] val = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] != 0) {
                    val[i][j] = ((node[i][j]+2) * (8 * i + j + 1));
                }
            }
        }
        int result;

        result = val[0][1];
        result = result ^ val[0][0];
        for (int[] aVal : val) {
            for (int j = 2; j < aVal.length; j++) {
                result = result ^ aVal[j];
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        Entity ent = (Entity)obj;
        boolean equal = true;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if(this.key[i][j] != ent.key[i][j]) {
                    equal = false;
                    return equal;
                }
            }
        }

        return equal;
    }

    @Override
    public int hashCode() {
        return zobristHash(key);
    }


}