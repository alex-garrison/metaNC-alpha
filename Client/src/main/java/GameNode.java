import java.util.ArrayList;

public class GameNode {
    public Board board;
    public ArrayList<GameNode> childBoards;

    public GameNode(Board board) {
        this.board = board;
        this.childBoards = new ArrayList<>();
    }
}
