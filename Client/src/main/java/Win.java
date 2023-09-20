public class Win {
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
    public int getLocalBoard() {
        return localBoard;
    }

    public boolean isWin() {
        return isWin;
    }

    public String getWinner() {
        return winner;
    }

    public String getWinType() {
        return winType;
    }
    public void setWinConditions(int localBoard, Boolean isWin) {
        this.localBoard = localBoard;
        this.isWin = isWin;
    }

    public void setWinConditions(int localBoard, Boolean isWin, String winType) {
        this.localBoard = localBoard;
        this.isWin = isWin;
        this.winType = winType;
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
