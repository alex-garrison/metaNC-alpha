import java.io.Serializable;

public class Win implements Serializable {
    private boolean isWin;
    private String winner;
    private String winType;
    private int subBoard;

    public Win() {
        this.subBoard = -1;
        this.isWin = false;
        this.winner = "No winner";
        this.winType = "No win";
    }

    public String getWinner() {
        return winner;
    }

    public void setWinConditions(int subBoard, Boolean isWin, String winType, String winner) {
        this.subBoard = subBoard;
        this.winner = winner;
        this.isWin = isWin;
        this.winType = winType;
    }
}
