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

    // Returns a move for the current player.
    public int[] getMove(boolean isRandom) throws GameException {
        if (isRandom) {
            return getRandomMove();
        } else {
            return findBestMove(board, board.turn);
        }
    }

    // Chooses and returns, at random, a valid move for the current player.
    private int[] getRandomMove() throws GameException {
        int[][] validMoves = board.getValidMovesGlobal();
        if (validMoves.length < 1) {
            throw new GameException("No more valid moves");
        }
        return validMoves[rand.nextInt(validMoves.length)];
    }

    //Finds and returns the ‘best’ move available to the specified player.
    public static int[] findBestMove(Board board, String player) throws GameException {
        // Create a map to store the board states and their scores
        HashMap<Board, Double> moveMap = new HashMap<>();

        // Generate game tree
        GameTree gameTree = new GameTree(new GameNode(board));
        GameTree.generateGameTree(board, player, gameTree.root, 1);

        // Evaluate terminal nodes
        for (GameNode terminalNode: gameTree.getTerminalNodes(gameTree.root)) {
            double boardScore = Evaluate.evaluateGlobal(terminalNode.board, player);
            moveMap.put(terminalNode.board, boardScore);
        }

        Object[] keys = moveMap.values().toArray();
        Arrays.sort(keys);

        // Find best move from best board state
        for (Map.Entry<Board, Double> entry: moveMap.entrySet()) {
            if (entry.getValue() == keys[keys.length-1]) {
                return entry.getKey().lastMove;
            }
        }

        throw new GameException("No valid moves found");
    }
}
