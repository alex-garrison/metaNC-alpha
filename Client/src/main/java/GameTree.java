import java.io.IOException;
import java.util.ArrayList;

public class GameTree {
    GameNode root;

    public GameTree(GameNode root) {
        this.root = root;
    }

    public ArrayList<GameNode> getTerminalNodes(GameNode currRoot) {
        ArrayList<GameNode> terminalNodes = new ArrayList<>();

        if (currRoot.childBoards.size() == 0) {
            terminalNodes.add(currRoot);
            return terminalNodes;
        } else {
            for (GameNode child: currRoot.childBoards) {
                terminalNodes.addAll(getTerminalNodes(child));
            }
            return terminalNodes;
        }
    }

    public static void generateGameTree(Board board, String currentPlayer, GameNode root) {
        if (board.isWin()) {
            return;
        }

        int[][] possibleMoves = board.getValidMovesAI();

        for (int[] move : possibleMoves) {

            Board newBoard;
            try {
                newBoard = DeepCopy.deepCopy(board);
            } catch (IOException | ClassNotFoundException e) {
                ClientGUI.frame.printToLog("Error generating board copy.");
                return;
            }

            try {
                newBoard.turn(currentPlayer, move);
            } catch (GameException e) {
                continue;
            }

            GameNode childNode = new GameNode(newBoard);
            root.childBoards.add(childNode);
        }
    }
}
