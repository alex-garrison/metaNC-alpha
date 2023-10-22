import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
public class AiAgent {
    Board board;
    Random rand;

    public AiAgent(Board board) {
        this.board = board;
        rand = new Random();
    }

    public int[] getMove() throws GameException {
        return getAIMove();
    }

    private int[] getRandomMove() throws GameException {
        int[][] validMoves = board.getValidMovesAI();
        if (validMoves.length < 1) {
            throw new GameException("No more valid moves. Draw.");
        }
        return validMoves[rand.nextInt(validMoves.length)];
    }

    private int[] getAIMove() {
        return findBestMove(board, board.turn);
    }

    public static int[] findBestMove(Board board, String player) {
        HashMap<Board, Double> moveMap = new HashMap<>();

        GameTree gameTree = new GameTree(new GameNode(board));
        GameTree.generateGameTree(board, player, 1, gameTree.root);

        for (GameNode terminalNode: gameTree.getTerminalNodes(gameTree.root)) {
            double boardScore = Evaluate.evaluateGlobal(terminalNode.board, player);
            moveMap.put(terminalNode.board, boardScore);
        }

        Object[] keys = moveMap.values().toArray();
        Arrays.sort(keys);

        for (Map.Entry<Board, Double> entry: moveMap.entrySet()) {
            if (entry.getValue() == keys[keys.length-1]) {
                return entry.getKey().lastMove;
            }
        }

        return new int[]{-1,-1};
    }
}
