import java.util.Random;
public class AiAgent {
    Board board;
    Random rand;

    public AiAgent(Board board) {
        this.board = board;
        rand = new Random();
    }

    public int[] getMove() throws GameException {
        return getRandomMove();
    }

    private int[] getRandomMove() throws GameException {
        int[][] validMoves = board.getValidMovesAI();
        if (validMoves.length < 1) {
            throw new GameException("No more valid moves. Draw.");
        }
        return validMoves[rand.nextInt(validMoves.length)];
    }
}
