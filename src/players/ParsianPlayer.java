package players;

import game.AbstractPlayer;
import game.BoardSquare;
import game.Move;
import game.OthelloGame;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
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

    double[][] coeffs = new double[3][6];
    int[][][] opening = new int[59][17][2];
    int[] openingCount = new int[59];
     private void fill_coeff(){

         /// Early
         coeffs[0][0] = 5;      //piece diff
         coeffs[0][1] = 950.724; //corner occupancy
         coeffs[0][2] = 500.026; //corner closeness
         coeffs[0][3] = 78.922;  //mobility
         coeffs[0][4] = 74.396;  //frontier discs
         coeffs[0][5] = 15;      //disc squares

         /// Middle
         coeffs[1][0] = 10;
         coeffs[1][1] = 801.724;
         coeffs[1][2] = 382.026;
         coeffs[1][3] = 78.922;
         coeffs[1][4] = 74.396;
         coeffs[1][5] = 10;

         /// Pre-end
         coeffs[2][0] = 50;
         coeffs[2][1] = 501.724;
         coeffs[2][2] = 132.026;
         coeffs[2][3] = 78.922;
         coeffs[2][4] = 74.396;
         coeffs[2][5] = 5;
     }

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

    private int myDepth;

    public ParsianPlayer(int depth) {

        super(depth);
        myDepth = depth;
        learningParams = new LearningParams();

        fill_coeff();
        loadOpeningBook();

//        initLearn(depth);

        best = null;
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

    private void initLearn(int index) {

        //// Learning
        String fileName = System.getProperty("user.dir")
//                + "/out/production/Othello"
                + "/param" + index + ".txt";
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

        OthelloGame jogo = new OthelloGame();
        playCounter++;
        evalGameState();
        System.out.println("Game Stage : " + gameStage.name());
        best = null;

//        if (killerCondition(false, tab) && getMyBoardMark() == 1) return killerMove.getBardPlace();

//        if (getMyBoardMark() == 1) {
            for (int i = 1; i < 8; i++){
                myDepth = i;
                alphaBeta(new Entity(tab), 0, 1,i, false);
            }
//        }
//        else {
//            for (int i = 1; i < getDepth(); i++){
//                myDepth = i;
//                alphaBeta(new Entity(tab), 0, 1,getDepth(), true);
//            }
//        }

        System.out.println("Finished: " + myDepth);

        if (best == null) {
            System.out.println("MISSED" + jogo.getValidMoves(tab, getMyBoardMark()) + "  " + getMyBoardMark());
            Random r = new Random();
            List<Move> jogadas = jogo.getValidMoves(tab, getMyBoardMark());
            if (jogadas.size() > 0) {
                return jogadas.get(r.nextInt(jogadas.size())).getBardPlace();
            } else {
                return new BoardSquare(-1, -1);
            }
        }
        return best.getBardPlace();

    }

    private boolean killerCondition(List<Move> moves) {
        OthelloGame othelloGame = new OthelloGame();
        // Corner Killer
        for (Move m : moves) {
            if (m.getBardPlace().getCol() == 0 || m.getBardPlace().getCol() == 7) {
                if (m.getBardPlace().getRow() == 0 || m.getBardPlace().getRow() == 7) {
                    killerMove = m;
                    return true;
                }
            }
        }
        //
        return false;
    }

    private int checkWin(Entity node) {
        int a = 0,b = 0, c = 0;
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                if (node.getKey()[i][j] == 1) a++;
                else if (node.getKey()[i][j] == -1) b++;
        c = 64 - a - b;
        if (a == 0) return getOpponentBoardMark();
        else if (b == 0) return getMyBoardMark();
        else if (c == 0) if (a > b) return getMyBoardMark(); else if (b > a) return getOpponentBoardMark();
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

    private double nextGuess() {
        return 0.0;
    }
    public double BNS(Entity node, double alpha, double beta) {
        OthelloGame othelloGame = new OthelloGame();
        List<Move> moveList = othelloGame.getValidMoves(node.getKey(), getMyBoardMark());
        int betterCounter = 0;
        Move bestNode = null;
        double bestVal = 0;
        do {
            double test = nextGuess();
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
        OthelloGame othelloGame = new OthelloGame();

        List<Move> moves;
        if (maxPlayer) moves = othelloGame.getValidMoves(root.getKey(), getMyBoardMark());
        else moves = othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark());
        if (depth == 0) {
            return eval(root.getKey(), moves.size(), maxPlayer,false);
        }

        double score = 0.0;
        boolean bSearchPV = true;
        Comparator<Move> comp = (Move a, Move b) -> (int) (eval(b.getBoard(), moves.size(), maxPlayer, true) - eval(a.getBoard(), moves.size(), maxPlayer,true));
        moves.sort(comp);
        for (Move m : moves) {
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
            if (v.iter + v.depth >= depth + myDepth) {
                if (v.depth == 0 && depth == 0) {
                    return v.value;
                }
                if (v.depth + v.iter >= depth + myDepth) {
                    if (v.alpha > alpha) alpha = v.value;
                    if (v.beta < beta) beta = v.beta;
//                return v.value;
                }
            }
        } else {
            v.iter = myDepth;
        }

        int cw = checkWin(root);
        if (cw != 0) {
            v.node = root;
            v.move = null;
            v.alpha = alpha;
            v.beta = beta;
            v.value = cw*(Double.MAX_VALUE - 1);
            v.depth = 0;
            v.iter = myDepth;
            store(v);
            return v.value;
        }

        List<Move> moves;
        if (maxPlayer) moves = othelloGame.getValidMoves(root.getKey(), getMyBoardMark());
        else moves = othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark());

        boolean iter = false;
        if (v.iter != myDepth) {
            if (moves.contains(v.move)) {
                moves.remove(v.move);
                iter = true;
            }
        } else if (killerCondition(moves) && getMyBoardMark() == 1){
            moves.remove(killerMove);
            v.move = killerMove;
            iter = true;
        }

        if (depth == 0) {
            v.node = root;
            v.move = null;
            v.alpha = alpha;
            v.beta = beta;
            v.value = eval(root.getKey(), moves.size(), maxPlayer,false);
            v.depth = 0;
            v.iter = myDepth;
            store(v);
            return v.value;
        }

        if (moves.isEmpty()) {
            v.value = eval(root.getKey(), moves.size(), maxPlayer,true);
            v.alpha = alpha;
            v.beta = beta;
            v.node = root;
            v.depth = 0;
            v.iter = myDepth;
            v.move = null;
            return v.value;
        }
        if (moves.size() == 1 && depth == myDepth) {
            v.value = 0;
            v.depth = depth;
            v.node = root;
            v.alpha = alpha;
            v.beta = beta;
            v.move = moves.get(0);
            best = v.move;
            v.iter = myDepth;
//            store(v);
            return 0.0;
        }

        v.node = root;
        if (maxPlayer) {
            v.value = -Double.MAX_VALUE + 1;
            if (iter) {
                alpha = Math.max(alpha, alphaBetaWithMemory(new Entity(v.move.getBoard()), alpha, beta, depth - 1, false));
                if (alpha > v.value) {
                    v.value = alpha;
                    v.depth = depth;
                    v.alpha = alpha;
                    v.beta  = beta;
                    v.iter = myDepth;
                    if (depth == myDepth)
                        best = v.move;
                }
                if (beta <= alpha) {
                    store(v);
                    return v.value;
                }
            }
            for (final Move m : moves) {
                v.move = m;
                alpha = Math.max(alpha, alphaBetaWithMemory(new Entity(m.getBoard()), alpha, beta, depth - 1, false));
                if (alpha > v.value) {
                    v.value = alpha;
                    v.depth = depth;
                    v.alpha = alpha;
                    v.beta  = beta;
                    v.iter  = myDepth;
                    if (depth == myDepth)
                        best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v.value = Double.MAX_VALUE;
            if (iter) {
                beta = Math.min(beta, alphaBetaWithMemory(new Entity(v.move.getBoard()), alpha, beta, depth - 1, true));
                if (beta < v.value) {
                    v.value = beta;
                    v.depth = depth;
                    v.alpha = alpha;
                    v.beta  = beta;
                    v.iter = myDepth;
                }
                if (beta <= alpha) {
                    store(v);
                    return v.value;
                }
            }

            for (final Move m : moves) {
                v.move = m;
                beta = Math.min(beta, alphaBetaWithMemory(new Entity(m.getBoard()), alpha, beta, depth - 1, true));
                if (beta < v.value) {
                    v.value = beta;
                    v.depth = depth;
                    v.alpha = alpha;
                    v.beta  = beta;
                    v.iter = myDepth;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        }
        store(v);
        return v.value;
    }

    private double alphaBetaCore(Entity root, double alpha, double beta, int depth, boolean maxPlayer) {
        OthelloGame othelloGame = new OthelloGame();
        double v;

        int cw = checkWin(root);
        if (cw != 0) {
            return cw*(Double.MAX_VALUE - 1);
        }

        List<Move> moves;
        if (maxPlayer) moves = othelloGame.getValidMoves(root.getKey(), getMyBoardMark());
        else moves = othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark());

        if (depth == 0) {
            return eval(root.getKey(), moves.size(), maxPlayer,false);
        }

        if (moves.isEmpty()) return eval(root.getKey(), moves.size(), maxPlayer,true);
        if (moves.size() == 1 && depth == myDepth) {
            best = moves.get(0);
            return 0.0;
        }

        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            for (final Move m : moves) {
                alpha = Math.max(alpha, alphaBetaCore(new Entity(m.getBoard()), alpha, beta, depth - 1, false));
                if (alpha > v) {
                    v = alpha;
                    if (depth == myDepth || (isMPC && depth == MPCDepth)) best = m;
                }
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            v = Double.MAX_VALUE;
            for (final Move m : moves) {
                beta = Math.min(beta, alphaBetaCore(new Entity(m.getBoard()), alpha, beta, depth - 1, true));
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
        List<Move> moves;
        if (maxPlayer) {
            moves = othelloGame.getValidMoves(root.getKey(), getMyBoardMark());

        } else {
            moves = othelloGame.getValidMoves(root.getKey(), getOpponentBoardMark());
        }
        double v;
        if (depth == 0) {
            return eval(root.getKey(), moves.size(), maxPlayer,false);
        }
        if (checkWin(root) != 0) {
            return eval(root.getKey(), moves.size(), maxPlayer,true);
        }
        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            Comparator<Move> comp = (Move a, Move b) -> (int) (eval(b.getBoard(),moves.size(), maxPlayer,false) - eval(a.getBoard(),moves.size(),maxPlayer ,false));
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
            Comparator<Move> comp = (Move a, Move b) -> (int) (eval(a.getBoard(), moves.size(),maxPlayer,false) - eval(b.getBoard(),moves.size(),maxPlayer, false));
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


    public double MPC(Entity tab, double alpha, double beta, int height, boolean maxPlayer) {
        isMPC = true;
        List<Move> moves;
        OthelloGame othelloGame = new OthelloGame();
        if (maxPlayer) {
            moves = othelloGame.getValidMoves(tab.getKey(), getMyBoardMark());
        } else {
            moves = othelloGame.getValidMoves(tab.getKey(), getOpponentBoardMark());
        }
        if (height == 0) {
            return eval(tab.getKey(), moves.size(), maxPlayer, false);
        }
        if (checkWin(tab) != 0) {
            return eval(tab.getKey(), moves.size(), maxPlayer,true);
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

        double v;

        if (maxPlayer) {
            v = -Double.MAX_VALUE + 1;
            for (Move m : moves) {

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
            for (Move m : moves) {
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

    private double eval(int[][] node, int moveSize, boolean maxPlayer, boolean end) {

        double diff = pieceDiff(node);
        if (end || true) return diff*10
//                + cor_occ * 801.724
//                + cor_close * 382.026
        ;
        double cor_occ = cornerOccupancy(node);
        double cor_close = cornerCloseness(node);
        double mob = mobility(node, moveSize, maxPlayer);
        double frontier = frontierDices(node);
        double disc = disc_squares(node);

        int index = 1;
        if(gameStage == GameStage.EARLY || gameStage == GameStage.OPENING)
            index = 0;
        else if(gameStage == GameStage.MID)
            index = 1;
        else if(gameStage == GameStage.PRE_END || gameStage == GameStage.END)
            index = 2;

        index = 1;
        double evaluate = coeffs[index][0] * diff
                + coeffs[index][1] * cor_occ
                + coeffs[index][2] * cor_close
                + coeffs[index][3] * mob
                + coeffs[index][4] * frontier
                + coeffs[index][5] * disc;

//        double evaluate = learningParams.weights[0] * diff
//                + learningParams.weights[1] * cor_occ
//                + learningParams.weights[2] * cor_close
//                + learningParams.weights[3] * mob
//                + learningParams.weights[4] * frontier
//                + learningParams.weights[5] * disc;

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
                B_cor_close+=2;
            } else if (node[0][1] == getOpponentBoardMark()) {
                R_cor_close+=2;
            }

            if (node[1][0] == getMyBoardMark()) {
                B_cor_close+=2;
            } else if (node[1][0] == getOpponentBoardMark()) {
                R_cor_close+=2;
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
                B_cor_close+=2;
            } else if (node[0][6] == getOpponentBoardMark()) {
                R_cor_close+=2;
            }

            if (node[1][7] == getMyBoardMark()) {
                B_cor_close+=2;
            } else if (node[1][7] == getOpponentBoardMark()) {
                R_cor_close+=2;
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
                B_cor_close+=2;
            } else if (node[6][0] == getOpponentBoardMark()) {
                R_cor_close+=2;
            }

            if (node[7][1] == getMyBoardMark()) {
                B_cor_close+=2;
            } else if (node[7][1] == getOpponentBoardMark()) {
                R_cor_close+=2;
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
                B_cor_close+=2;
            } else if (node[6][7] == getOpponentBoardMark()) {
                R_cor_close+=2;
            }

            if (node[7][6] == getMyBoardMark()) {
                B_cor_close+=2;
            } else if (node[7][6] == getOpponentBoardMark()) {
                R_cor_close+=2;
            }

            if (node[6][6] == getMyBoardMark()) {
                B_cor_close++;
            } else if (node[6][6] == getOpponentBoardMark()) {
                R_cor_close++;
            }
        }

        return 12.5 * (R_cor_close - B_cor_close);

    }

    private double nomove = 2;
    private double mobility(int[][] node, int moveSize, boolean maxPlayer) {
        OthelloGame g = new OthelloGame();
        int R_moves;
        int B_moves;
        if (maxPlayer) {
            B_moves = moveSize;
            R_moves = g.getValidMoves(node, getOpponentBoardMark()).size();
        }
        else {
            R_moves = moveSize;
            B_moves = g.getValidMoves(node, getMyBoardMark()).size();
        }
        if (B_moves == 0) return -100*nomove;
        else if (R_moves == 0) return 100*nomove;
        double mob = 0;
        if (B_moves > R_moves) {
            mob = 100 * ((double) B_moves / (B_moves + R_moves));
        } else if (B_moves < R_moves) {
            mob = -100 * ((double) R_moves / (B_moves + R_moves));
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

    private void loadOpeningBook(){
        String line = null;
        String fileName =  System.getProperty("user.dir")+"/OpeningBook.txt";
//        System.out.println(fileName);
        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            int lineCount = 0, index = 0;
            while((line = bufferedReader.readLine()) != null) {
                String[] l = line.split(" ");
                index = 0;
                for(String str : l){
                    int ij = Integer.parseInt(str);
                    opening[lineCount][index][0] = ij/10;
                    opening[lineCount][index][1] = ij%10;
                    index++;
                }
                openingCount[lineCount] = index;
                lineCount++;
            }

            // Always close files.
            bufferedReader.close();
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

//        for (int i = 0; i <50 ; i++) {
//            for (int j = 0; j <openingCount[i] ; j++) {
//                System.out.print(opening[i][j][0]+" : "+opening[i][j][1]+"    ");
//            }
//            System.out.println();
//        }
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
        this(_node, val, _move);
        depth = _mode;
    }

    public Lock(Entity _node, double val, Move _move, int _mode, int _iter){
        this(_node, val, _move, _mode);
        iter = _iter;
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
    public int iter;

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