package players;

import game.AbstractPlayer;
import game.BoardSquare;
import game.Move;
import game.OthelloGame;

import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
    int[][] zorbistNumber;
    Lock[][] hashTable;
    int[] hashCounter;
    int[] hashCursor;
    private GameStage gameStage;

    private int playCounter;
    private Move best;
    private long temp;
    private Move killerMove;

    // isMPC
    int MPCDepth;
    boolean isMPC;
    static int MAX_STAGE = 2;
    static int MAX_HEIGHT = 5;
    static int NUM_TRY = 2;

    private Param[][][] paramss;
    private Param theParam;
    private LearningParams learningParams;

    public ParsianPlayer(int depth) {

        super(4);
        learningParams = new LearningParams();
        //// Learning
        String fileName = System.getProperty("user.dir")
//                + "/out/production/Othello"
                + "/param" + depth + ".txt";
        System.out.println(fileName);
        // This will reference one line at a time
        String line = null;

        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader =
                    new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =
                    new BufferedReader(fileReader);

            int counter = 0;
            while((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                learningParams.weights[counter] = Double.valueOf(line);
                counter++;
                if (counter >= learningParams.weights.length) counter = 0;
            }

            // Always close files.
            bufferedReader.close();

            for (double d : learningParams.weights) {
                System.out.println("RUN WITH :" + d);
            }
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            fileName + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + fileName + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }

        // Learning


        best = null;
        transportTable = new HashMap<>();
        zorbistNumber = new int[64][2];
        hashTable = new Lock[20000][10]; // Key - Lock
        hashCounter = new int[20000];
        hashCursor = new int[20000];
        Random rand = new Random();
        for (int i = 0; i < zorbistNumber.length; i++) {
            for (int j = 0; j < zorbistNumber[i].length; j++) {
                zorbistNumber[i][j] = rand.nextInt(1000);
            }
        }
        for (int[] a : zorbistNumber) {
            for (int b : a) {
                System.out.println(b);
            }
        }

        gameStage = GameStage.NO_STAGE;
        playCounter = 0;
        theParam = new Param();
        theParam.d = getDepth()/2;
        theParam.t = 6;
        theParam.a = 1;
        theParam.b = 0;
        theParam.s = 1000;
//        for (int i = 0; i < params.length; i++) {
//            for (int j = 0; j < params[i].length; j++) {
//                for (int k = 0; k < params[i][j].length; k++) {
//                    params[i][j][k].d = 4;
//                    params[i][j][k].t = 1;
//                    params[i][j][k].a = 1;
//                    params[i][j][k].b = 1;
//                    params[i][j][k].s = 1;
//                }
//            }
//        }
        isMPC = false;
    }

    private int zorbist(int[][] node){
        int key = 0;
        for(int i=0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (node[i][j] == 1) key ^= zorbistNumber[i*8+j][0];
                else if (node[i][j] == -1) key ^= zorbistNumber[i*8+j][1];
            }
        }
        if (key >= 20000) {
            return 20000 - 1;
        }
        return key;
    }

    private Lock lookup(Entity node) {
        int key = zorbist(node.getKey());
        if (key > 20000) key = 20000 -1;
        for (int i = 0; i < hashCounter[key];i++) {
            if (hashTable[key][i] == null) continue;
            if (hashTable[key][i].node.equals(node)) {
                return hashTable[key][i];
            }
        }
        return new Lock(null, 0.0, null);
    }

    private void store(Lock lock) {
        int key = zorbist(lock.node.getKey());
        if (key > 20000) key = 20000 -1;
        for (int i = 0; i < hashCounter[key];i++) {
            if (hashTable[key][i].node.equals(lock.node)) {
                hashTable[key][i] = lock;
                return;
            }
        }
        hashCounter[key]++;
        hashCursor[key]++;
        if (hashCounter[key] >= 10) hashCounter[key] = 9;
        if (hashCursor[key] >= 10) hashCursor[key] = 1;
        hashTable[key][hashCursor[key]-1] = lock;
    }

    @Override
    public BoardSquare play(int[][] tab) {

        if (getMyBoardMark() == 1) {
            learningParams.weights[0] = 318;
            learningParams.weights[1] = 639;
            learningParams.weights[2] = 181;
            learningParams.weights[3] = 935;
            learningParams.weights[4] = 345;
            learningParams.weights[5] = 253;
//[318, 639, 181, 953, 345, 253]
        }

        OthelloGame jogo = new OthelloGame();
        playCounter++;
        evalGameState();
        System.out.println("Game Stage : " + gameStage.name());
        best = null;
        double f;

//        if (killerCondition(false, tab) && getMyBoardMark() == 1) return killerMove.getBardPlace();

        System.out.println("Branch: " + jogo.getValidMoves(tab, getMyBoardMark()).size());
        System.out.println("Branch: " + jogo.getValidMoves(tab, getOpponentBoardMark()).size());
        System.out.println("TAB: " + temp);
        if (getMyBoardMark() == 1) f = alphaBeta(new Entity(tab), 0, 1,getDepth(), false);
        else f = alphaBeta(new Entity(tab), 0, 1,getDepth(), false);//f = MTDF(new Entity(tab), 5000, getDepth());
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

    private int checkWin(Entity node) {
        OthelloGame o = new OthelloGame();
        int diff = 0;
        if (o.noSpace(node.getKey())) {
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    diff += node.getKey()[i][j];
            diff *= getMyBoardMark();
            if (diff > 0) return 1;
            if (diff < 0) return -1;
        }
        return 0;
    }

    private void evalGameState() {
        if (playCounter < 5) {
            gameStage = GameStage.OPENING;
        } else if (playCounter < 10) {
            gameStage = GameStage.EARLY;
        } else if (playCounter < 22.5) {
            gameStage = GameStage.MID;
        } else if (playCounter < 25) {
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

    public  double PVS(Entity root, double alpha, double beta, int depth, boolean maxPlayer) {
        if (depth == 0) {
            return eval(root.getKey(), false);
        }
        OthelloGame othelloGame = new OthelloGame();

        double score = 0.0;
        boolean bSearchPV = true;
        int mark = 0;
        if (maxPlayer) mark = getMyBoardMark();
        else mark = getOpponentBoardMark();
        Comparator<Move> comp = (Move a, Move b) -> (int) (eval(b.getBoard(),true) - eval(a.getBoard(), true));
        List<Move> temp = othelloGame.getValidMoves(root.getKey(), mark);
        temp.sort(comp);
        for (Move m : temp) {
            Entity e = new Entity(m.getBoard());
            e.setMove(m);
            if (bSearchPV) {
                score = -PVS(e, -beta, -alpha, depth - 1, !maxPlayer);
            } else {
                score = -PVS(e, -alpha - 1, -alpha, depth - 1, !maxPlayer);
                if (score > alpha)
                    score = -PVS(e, -beta, -alpha, depth - 1, !maxPlayer);  /// research
            }

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
                if (depth == getDepth() && maxPlayer) best = m;
                bSearchPV = false;
            }
        }

        return alpha;
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
        Lock v = lookup(root);
        if (v.node != null) {
            if (v.depth == 0 && depth != getDepth()) {
                return v.value;
            }
            if (v.depth >= depth) {
                if (v.alpha > alpha) alpha = v.value;
                if (v.beta < beta) beta = v.beta;
                return v.value;
            }

        }
        int cw = checkWin(root);
        if (cw != 0) {
            v.node = root;
            v.move = null;
            v.alpha = alpha;
            v.beta = beta;
            v.value = cw*(Double.MAX_VALUE - 1);
            v.depth = 0;
            store(v);
            temp++;
            return v.value;
        }

        if (depth == 0) {
            temp++;
            v.node = root;
            v.move = null;
            v.alpha = alpha;
            v.beta = beta;
            v.value = eval(root.getKey(), false);
            v.depth = depth;
            store(v);
            return v.value;
        }
        if (othelloGame.getValidMoves(root.getKey(), getMyBoardMark()).isEmpty() && maxPlayer) return eval(root.getKey(), true);
        else if (othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark()).isEmpty() && !maxPlayer) return eval(root.getKey(), true);
        v.node = root;
        if (maxPlayer) {
            v.value = -Double.MAX_VALUE + 1;
            for (final Move m : othelloGame.getValidMoves(root.getKey(), getMyBoardMark())) {
                v.move = m;
                alpha = Math.max(alpha, alphaBetaWithMemory(new Entity(m.getBoard()), alpha, beta, depth - 1, false));
                if (alpha > v.value) {
                    v.value = alpha;
                    v.depth = depth;
                    v.alpha = alpha;
                    v.beta  = beta;
                    if (depth == getDepth())
                        best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v.value = Double.MAX_VALUE;
            for (final Move m : othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark())) {
                v.move = m;
                beta = Math.min(beta, alphaBetaWithMemory(new Entity(m.getBoard()), alpha, beta, depth - 1, true));
                if (beta < v.value) {
                    v.value = beta;
                    v.depth = depth;
                    v.alpha = alpha;
                    v.beta  = beta;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        }
        store(v);
        temp++;
        return v.value;
    }

    private double alphaBetaCore(Entity root, double alpha, double beta, int depth, boolean maxPlayer) {
        OthelloGame othelloGame = new OthelloGame();
        double v;
        if (depth == 0) {
            return eval(root.getKey(), false);
        }
        if (endGame(root.getKey(), maxPlayer)) {
            return eval(root.getKey(), true);
        }
        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            for (Move m : othelloGame.getValidMoves(root.getKey(), getMyBoardMark())) {

                Entity e = new Entity(m.getBoard());
                alpha = Math.max(alpha, alphaBetaCore(e, alpha, beta, depth - 1, false));
                if (alpha > v) {
                    v = alpha;
                    if (depth == getDepth() || (isMPC && depth == MPCDepth)) best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v = Double.MAX_VALUE;
            for (Move m : othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark())) {
                Entity e = new Entity(m.getBoard());
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

    private double alphaBetaCoreSort(Entity root, double alpha, double beta, int depth, boolean maxPlayer) {
        OthelloGame othelloGame = new OthelloGame();
        double v;
        if (depth == 0) {
            return eval(root.getKey(), false);
        }
        if (endGame(root.getKey(), maxPlayer)) {
            return eval(root.getKey(), true);
        }
        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            List<Move> moves = othelloGame.getValidMoves(root.getKey(), getMyBoardMark());
            Comparator<Move> comp = (Move a, Move b) -> (int) (eval(b.getBoard(),false) - eval(a.getBoard(), false));
            moves.sort(comp);
            for (Move m : moves) {

                Entity e = new Entity(m.getBoard());
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
            List<Move> moves = othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark());
            Comparator<Move> comp = (Move a, Move b) -> (int) (eval(a.getBoard(),false) - eval(b.getBoard(), false));
            moves.sort(comp);
            for (Move m : moves) {
                Entity e = new Entity(m.getBoard());
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
        isMPC = true;

        if (height == 0) {
            return eval(tab.getKey(), false);
        }
        if (endGame(tab.getKey(), maxPlayer)) {
            return eval(tab.getKey(), true);
        }

        if (height == MAX_HEIGHT) {
//            for (int i = 0; i < NUM_TRY; i++) {
                double bound;
                Param pa = theParam;
//                if (pa.d < 0) break;
                MPCDepth = pa.d;
                bound = Math.round((pa.t*pa.s + beta - pa.b)/pa.a);
                if (MPC(tab, bound - 1, bound, pa.d, false) >= bound) {
                    System.out.println("Cut B");

                    return beta;
                }

                bound = Math.round((-pa.t*pa.s + alpha - pa.b)/pa.a);
                if (MPC(tab, bound, bound + 1, pa.d, true) <= bound) {
                    System.out.println("Cut A");

                    return alpha;
                }
//            }
        }

        OthelloGame othelloGame = new OthelloGame();
        double v;

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

//        double evaluate = 10 * diff
//                + 801.724 * cor_occ
//                + 382.026 * cor_close
//                + 200.922 * mob
//                + 74.396 * frontier
//                + 10 * disc;
        double evaluate = learningParams.weights[0] * diff
                + learningParams.weights[1] * cor_occ
                + learningParams.weights[2] * cor_close
                + learningParams.weights[3] * mob
                + learningParams.weights[4] * frontier
                + learningParams.weights[5] * disc;

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

        int[][] tmp1 = new int[8][8];
        int[][] tmp2 = new int[8][8];
        int[][] tmp3 = new int[8][8];
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

class LearningParams{
    public LearningParams() {
        weights = new double[6];
        weights[0] = 10;
        weights[1] = 801.724;
        weights[2] = 382.026;
        weights[3] = 200.922;
        weights[4] = 74.396 ;
        weights[5] = 10     ;
    }
    double[] weights;
}

class Param{
    public Param() {

    }
    int d; // shallow depth
    double t; // threshold
    double a,b,s; // slope, offset, std.dev
}

class Lock{
    public Lock(Entity _node, double val){
        node = _node;
        value = val;
    }
    public Lock(Entity _node, double val, Move _move){
        this(_node, val);
        move = _move;
    }

    public Lock(Entity _node, double val, Move _move, int _mode){
        this(_node, val);
        move = _move;
        depth = _mode;
    }

    public Lock(){
        this(null,0.0,null, -1);

    }
    public double value;
    public Entity node;
    public Move move;
    public int depth;
    public double alpha;
    public double beta;

}

class Entity {
    Entity(int[][] _node) {
        key = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                key[i][j]=_node[i][j];
            }
        }
    }

    Entity(int[][] _node, Move _move) {
        key = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                key[i][j]=_node[i][j];
            }
        }
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
        Entity ent = (Entity) obj;
        boolean equal = true;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (this.key[i][j] != ent.key[i][j]) {
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