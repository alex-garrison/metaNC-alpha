import java.util.TreeSet;

public class Board {
    protected final String[][][] board;
    protected String turn;
    protected int[] lastMove;
    protected Win[] localBoardWins;
    protected final TreeSet<Integer> wonBoards;
    protected final TreeSet<Integer> openBoards;

    public boolean isWon;
    public String winner;

    public Board() {
        this.board = new String[9][3][3];
        this.localBoardWins = new Win[9];
        this.wonBoards = new TreeSet<>();
        this.openBoards = new TreeSet<>();

        this.emptyBoard();
        for (int i = 0; i < 9; i++) {
            localBoardWins[i] = new Win();
        }

        isWon = false;
        winner = "";
    }

    public static String invertPlayer(String player) {
        if (player.equals("X")) {return "O";} else {return "X";}
    }

    public void setStarter(String starter) {
        turn = starter;
    }

    public void turn(String player, int[] location) throws GameException {
            if (player == null) {
                throw new GameException("No player for this clientID");
            }
            if (player.equals(turn)) {
                if (isValidMove(location) && !isInWonBoard(location) && isInCorrectLocalBoard(location)) {
                    board[location[0]][location[1]][location[2]] = player;
                    lastMove = location;
                } else {
                    throw new GameException("Move not valid");
                }

                turn = invertPlayer(turn);

            } else if (turn.isEmpty()) {
                throw new GameException("Starter not set");
            } else {
                throw new GameException("Not your turn");
            }
    }

    public void resetBoard() {
        wonBoards.clear();
        openBoards.clear();

        emptyBoard();
        for (int i = 0; i < 9; i++) {
            localBoardWins[i] = new Win();
        }

        isWon = false;
        winner = "";
    }

    public boolean isInCorrectLocalBoard(int[] location) {
        int inverseResolvedLoc = lastMove[1] * 3 + lastMove[2];
        if (lastMove[0] == -1) {
            return true;
        } else if (wonBoards.contains(inverseResolvedLoc)) {
            return true;
        }
        return (location[0]==inverseResolvedLoc);
    }

    public int getCorrectLocalBoard() {
        int inverseResolvedLoc = lastMove[1] * 3 + lastMove[2];
        if (isWonBoard(inverseResolvedLoc)) {
            return -1;
        } else if (inverseResolvedLoc < 0) {
            return -1;
        } else {
            return inverseResolvedLoc;
        }
    }

    public boolean isInWonBoard(int[] location) {
        return (wonBoards.contains(location[0]));
    }

    public boolean isWonBoard(int boardIndex) {
        return (wonBoards.contains(boardIndex));
    }

    public boolean isValidMove(int[] location) {
        return (board[location[0]][location[1]][location[2]].equals(""));
    }

    public int getNumberOfValidMovesAI() {
        int numberOfValidMoves = 0;

        int correctLocalBoard = getCorrectLocalBoard();
        if (correctLocalBoard == -1) {
            for (Integer openBoard: openBoards) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int[] newLoc = new int[]{openBoard, row, col};
                        if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectLocalBoard(newLoc)) {
                            numberOfValidMoves++;
                        }
                    }
                }
            }
        } else {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int[] newLoc = new int[]{correctLocalBoard, row, col};
                    if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectLocalBoard(newLoc)) {
                        numberOfValidMoves++;
                    }
                }
            }
        }
        return numberOfValidMoves;
    }

    public int getNumberOfValidMoves(int boardIndex) {
        int numberOfValidMoves = 0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (isValidMove(new int[]{boardIndex, row, col})) {numberOfValidMoves++;}
            }
        }

        return numberOfValidMoves;
    }

    public int[][] getValidMovesAI() {
        int numberOfValidMoves = getNumberOfValidMovesAI();
        int correctLocalBoard = getCorrectLocalBoard();

        int[][] validMoves = new int[numberOfValidMoves][2];
        int counter = 0;

        if (correctLocalBoard == -1) {
            for (Integer openBoard: openBoards) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int[] newLoc = new int[]{openBoard, row, col};
                        if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectLocalBoard(newLoc)) {
                            validMoves[counter] = newLoc;
                            counter++;
                        }
                    }
                }
            }
        } else {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int[] newLoc = new int[]{correctLocalBoard, row, col};
                    if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectLocalBoard(newLoc)) {
                        validMoves[counter] = newLoc;
                        counter++;
                    }
                }
            }
        }
        return validMoves;
    }

    public int[] getLastMove() {
        return lastMove;
    }

    public Win[] getLocalBoardWins() {
        for (String player : new String[]{"X","O"}) {
            boardLoop: for (int i = 0; i < board.length; i++) {
                if (wonBoards.contains(i)) {
                    continue;
                }
                Win win = new Win();
                for (int j = 0; j < board[i].length; j++) {
                    if (board[i][j][0].equals(player) && board[i][j][1].equals(player) && board[i][j][2].equals(player)) {
                        win.setWinConditions(i,true, "win", player);
                        localBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue boardLoop;
                    } else if (board[i][0][j].equals(player) && board[i][1][j].equals(player) && board[i][2][j].equals(player)){
                        win.setWinConditions(i,true, "win", player);
                        localBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue boardLoop;
                    }
                }
                if ((board[i][0][0].equals(player) && board[i][1][1].equals(player) && board[i][2][2].equals(player))) {
                    win.setWinConditions(i,true, "win", player);
                    localBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue;
                } else if ((board[i][0][2].equals(player) && board[i][1][1].equals(player) && board[i][2][0].equals(player))) {
                    win.setWinConditions(i,true, "win", player);
                    localBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue;
                }
                if (getNumberOfValidMoves(i) == 0) {
                    win.setWinConditions(i,true, "draw", "D");
                    localBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i);
                }
            }
        }
        return localBoardWins;
    }

    public boolean isWin() {
        localBoardWins = getLocalBoardWins();

        for (String player : new String[]{"X", "O"}) {
            for (int i = 0; i < 9; i = i + 3) {
                if (localBoardWins[i].getWinner().equals(player) && localBoardWins[i+1].getWinner().equals(player) && localBoardWins[i+2].getWinner().equals(player)) {
                    isWon = true; winner = player;
                    return true;
                }
            }
            for (int j = 0; j < 3; j++) {
                if (localBoardWins[j].getWinner().equals(player) && localBoardWins[j+3].getWinner().equals(player) && localBoardWins[j+6].getWinner().equals(player)) {
                    isWon = true; winner = player;
                    return true;
                }
            }
            if (    (localBoardWins[0].getWinner().equals(player) && localBoardWins[4].getWinner().equals(player) && localBoardWins[8].getWinner().equals(player))
                    || (localBoardWins[2].getWinner().equals(player) && localBoardWins[4].getWinner().equals(player) && localBoardWins[6].getWinner().equals(player))) {
                isWon = true; winner = player;
                return true;
            }
        }
        if (getNumberOfValidMovesAI() < 1) {
            isWon = true; winner = "D"; return true;
        }
        return false;
    }

    public void emptyBoard() {
        for (int i = 0; i < board.length; i++) {
            openBoards.add(i);
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = new String[] {"", "", ""};
            }
        }
        lastMove = new int[]{-1,-1,-1};
    }

    public boolean isEmptyBoard() {
        return (lastMove[0] == -1);
    }

    public String whoseTurn() {
        return turn;
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        String[] outputArr = new String[27];
        int counter = 0;

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 9; i++) {
                for (int k = 0; k < 3; k++) {
                    String boardChar = board[i][j][k];
                    output.append(boardChar).append(" ");
                }
                outputArr[counter] = output.toString();
                counter++;
                output = new StringBuilder();
            }
        }

        output = new StringBuilder();
        output.append("\n");
        for (int i = 0; i < 9; i=i+3) {
            for (int j = i; j < 27; j=j+9) {
                for (int k = j; k < (j+3); k++) {
                    output.append(outputArr[k].strip()).append(" ");
                    if (k<(j+2)) output.append("| ");
                }
                output.append(" \n");
            }
            if (i<6) output.append("---------------------\n");
        }

        return output.toString();
    }

    public String[][][] getBoard() {
        return board;
    }
}

