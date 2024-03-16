import java.io.IOException;
import java.util.ArrayList;

public class GameTree {
    GameNode root;

    public GameTree(GameNode root) {
        this.root = root;
    }

    // Returns a list of the terminal (or leaf) nodes of the game tree, using recursion
    public ArrayList<GameNode> getTerminalNodes(GameNode currRoot) {
        ArrayList<GameNode> terminalNodes = new ArrayList<>();

        if (currRoot.childBoards.size() == 0) {
            terminalNodes.add(currRoot);
        } else {
            for (GameNode child: currRoot.childBoards) {
                terminalNodes.addAll(getTerminalNodes(child));
            }
        }
        return terminalNodes;
    }

    // Generates and returns a game tree of user-specified depth
    public static void generateGameTree(Board board, String currentPlayer, GameNode root, int depth) {
        if (board.isWin()) {
            return;
        }

        // Finds all possible moves for the root board
        int[][] possibleMoves = board.getValidMovesGlobal();

        for (int[] move : possibleMoves) {
            Board newBoard;

            // Creates a deep copy of the board
            try {
                newBoard = DeepCopy.deepCopy(board);
            } catch (IOException | ClassNotFoundException e) {
                ClientGUI.frame.printToLog("Error generating board copy.");
                return;
            }

            // Attempts to make the move
            try {
                newBoard.turn(currentPlayer, move);
            } catch (GameException e) {
                continue;
            }

            // Recursively generates the game tree for this node until the specified depth is reached
            if (depth > 1) {
                generateGameTree(newBoard, currentPlayer.equals("X") ? "O" : "X", root, depth - 1);
            }

            GameNode childNode = new GameNode(newBoard);
            root.childBoards.add(childNode);
        }
    }
}
