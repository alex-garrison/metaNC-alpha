import java.io.Serializable;

public class Win implements Serializable {
    private boolean isWin;
    private String winner;
    private String winType;

    private int localBoard;

    public Win() {
        this.localBoard = -1;
        this.isWin = false;
        this.winner = "No winner";
        this.winType = "No win";
    }

    public String getWinner() {
        return winner;
    }

    public void setWinConditions(int localBoard, Boolean isWin, String winType, String winner) {
        this.localBoard = localBoard;
        this.winner = winner;
        this.isWin = isWin;
        this.winType = winType;
    }

    public String toString() {
        return "client.Board : " + localBoard + " client.Win : " + isWin + " client.Win type " + winType;
    }
}
