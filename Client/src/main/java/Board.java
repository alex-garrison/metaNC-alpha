import java.io.Serializable;
import java.util.TreeSet;

public class Board implements Serializable {
    protected String[][][] board;
    protected String turn;
    protected int[] lastMove;
    protected Win[] subBoardWins;
    protected TreeSet<Integer> wonBoards;
    protected TreeSet<Integer> openBoards;

    public boolean isWon;
    public String winner;

    public Board() {
        this.board = new String[9][3][3];
        this.subBoardWins = new Win[9];
        this.wonBoards = new TreeSet<>();
        this.openBoards = new TreeSet<>();

        this.emptyBoard();
        for (int i = 0; i < 9; i++) {
            subBoardWins[i] = new Win();
        }

        isWon = false;
        winner = "";
    }

    // Inverts the player
    public static String invertPlayer(String player) {
        if (player.equals("X")) {return "O";} else {return "X";}
    }

    public void setStarter(String starter) {
        turn = starter;
    }

    // Called every time the user or AI wants to make a turn on the board.
    public void turn(String player, int[] location) throws GameException {
            if (player == null) {
                throw new GameException("No player for this clientID");
            }
            if (player.equals(turn)) {
                // Validates that the player is able to make the move (turn checking and move validation)
                if (isValidMove(location) && !isInWonBoard(location) && isInCorrectSubBoard(location)) {
                    // Makes the move
                    board[location[0]][location[1]][location[2]] = player;
                    lastMove = location;
                } else {
                    throw new GameException("Move not valid");
                }

                // Updates whose turn it is
                turn = invertPlayer(turn);

            } else if (turn.isEmpty()) {
                throw new GameException("Starter not set");
            } else {
                throw new GameException("Not your turn");
            }
    }

    // Resets the board to its initial state
    public void resetBoard() {
        wonBoards.clear();
        openBoards.clear();

        emptyBoard();
        for (int i = 0; i < 9; i++) {
            subBoardWins[i] = new Win();
        }

        isWon = false;
        winner = "";
    }

    // Checks if the move is in the correct sub-board
    public boolean isInCorrectSubBoard(int[] location) {
        int inverseResolvedLoc = lastMove[1] * 3 + lastMove[2];
        if (lastMove[0] == -1) {
            return true;
        } else if (wonBoards.contains(inverseResolvedLoc)) {
            return true;
        }
        return (location[0]==inverseResolvedLoc);
    }

    // Returns the sub-board that the next move should be made in
    public int getCorrectSubBoard() {
        int inverseResolvedLoc = lastMove[1] * 3 + lastMove[2];
        if (isWonBoard(inverseResolvedLoc)) {
            return -1;
        } else if (inverseResolvedLoc < 0) {
            return -1;
        } else {
            return inverseResolvedLoc;
        }
    }

    // Checks if the move is within a won sub-board
    public boolean isInWonBoard(int[] location) {
        return (wonBoards.contains(location[0]));
    }

    // Checks if the sub-board is won
    public boolean isWonBoard(int boardIndex) {
        return (wonBoards.contains(boardIndex));
    }

    // Checks if the move can be made
    public boolean isValidMove(int[] location) {
        return (board[location[0]][location[1]][location[2]].equals(""));
    }

    // Returns the number of valid moves for the global board
    public int getNumberOfValidMovesGlobal() {
        int numberOfValidMoves = 0;

        int correctSubBoard = getCorrectSubBoard();
        if (correctSubBoard == -1) {
            for (Integer openBoard: openBoards) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int[] newLoc = new int[]{openBoard, row, col};
                        if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectSubBoard(newLoc)) {
                            numberOfValidMoves++;
                        }
                    }
                }
            }
        } else {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int[] newLoc = new int[]{correctSubBoard, row, col};
                    if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectSubBoard(newLoc)) {
                        numberOfValidMoves++;
                    }
                }
            }
        }
        return numberOfValidMoves;
    }

    // Returns the number of valid moves for a sub-board
    public int getNumberOfValidMovesSub(int boardIndex) {
        int numberOfValidMoves = 0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (isValidMove(new int[]{boardIndex, row, col})) {numberOfValidMoves++;}
            }
        }

        return numberOfValidMoves;
    }

    // Returns an array of all valid moves available to the current player
    public int[][] getValidMovesGlobal() {
        // Get the number of valid moves and the correct sub-board
        int numberOfValidMoves = getNumberOfValidMovesGlobal();
        int correctSubBoardBoard = getCorrectSubBoard();

        int[][] validMoves = new int[numberOfValidMoves][2];
        int counter = 0;

        // If the is no correct sub-board, then moves are found for all open sub-boards
        if (correctSubBoardBoard == -1) {
            for (Integer openBoard: openBoards) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int[] newLoc = new int[]{openBoard, row, col};
                        if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectSubBoard(newLoc)) {
                            validMoves[counter] = newLoc;
                            counter++;
                        }
                    }
                }
            }
        }
        // If there is a correct sub-board, then moves are found for that sub-board
        else {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int[] newLoc = new int[]{correctSubBoardBoard, row, col};
                    if (isValidMove(newLoc) && !isInWonBoard(newLoc) && isInCorrectSubBoard(newLoc)) {
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

    // Returns an array of Win classes for each sub-board
    public Win[] getSubBoardWins() {
        // Checks for wins for each player in each sub-board
        for (String player : new String[]{"X","O"}) {
            boardLoop: for (int i = 0; i < board.length; i++) {
                if (wonBoards.contains(i)) {
                    continue;
                }
                Win win = new Win();

                // Checks for wins or draws in rows and columns and diagonals
                // If so, the sub-board is added to the set of wonBoards and removed from the set of openBoards
                // and the win is added to the subBoardWins array

                for (int j = 0; j < board[i].length; j++) {
                    if (board[i][j][0].equals(player) && board[i][j][1].equals(player) && board[i][j][2].equals(player)) {
                        win.setWinConditions(i,true, "win", player);
                        subBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue boardLoop;
                    } else if (board[i][0][j].equals(player) && board[i][1][j].equals(player) && board[i][2][j].equals(player)){
                        win.setWinConditions(i,true, "win", player);
                        subBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue boardLoop;
                    }
                }
                if ((board[i][0][0].equals(player) && board[i][1][1].equals(player) && board[i][2][2].equals(player))) {
                    win.setWinConditions(i,true, "win", player);
                    subBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue;
                } else if ((board[i][0][2].equals(player) && board[i][1][1].equals(player) && board[i][2][0].equals(player))) {
                    win.setWinConditions(i,true, "win", player);
                    subBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i); continue;
                }
                if (getNumberOfValidMovesSub(i) == 0) {
                    win.setWinConditions(i,true, "draw", "D");
                    subBoardWins[i] = win; wonBoards.add(i); openBoards.remove(i);
                }
            }
        }
        return subBoardWins;
    }

    // Checks and returns whether or not the global board is won
    // If so, it will update the board information relating to the board
    public boolean isWin() {
        subBoardWins = getSubBoardWins();

        // Checks for wins in rows and columns and diagonals on the global board
        for (String player : new String[]{"X", "O"}) {
            for (int i = 0; i < 9; i = i + 3) {
                if (subBoardWins[i].getWinner().equals(player) && subBoardWins[i+1].getWinner().equals(player) && subBoardWins[i+2].getWinner().equals(player)) {
                    isWon = true; winner = player;
                    return true;
                }
            }
            for (int j = 0; j < 3; j++) {
                if (subBoardWins[j].getWinner().equals(player) && subBoardWins[j+3].getWinner().equals(player) && subBoardWins[j+6].getWinner().equals(player)) {
                    isWon = true; winner = player;
                    return true;
                }
            }
            if (    (subBoardWins[0].getWinner().equals(player) && subBoardWins[4].getWinner().equals(player) && subBoardWins[8].getWinner().equals(player))
                    || (subBoardWins[2].getWinner().equals(player) && subBoardWins[4].getWinner().equals(player) && subBoardWins[6].getWinner().equals(player))) {
                isWon = true; winner = player;
                return true;
            }
        }

        // If there are no valid moves left, the game is a draw
        if (getNumberOfValidMovesGlobal() < 1) {
            isWon = true; winner = "D"; return true;
        }
        return false;
    }

    // Empties the board
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

    // Returns the fill factor (amount of the cells played) of the board, out of 81
    public int getFillFactor() {
        int fillFactor = 0;
        for (int boardIndex = 0; boardIndex < board.length; boardIndex++) {
            for (int row = 0; row < board[boardIndex].length; row++) {
                for (int col = 0; col < board[boardIndex][row].length; col++) {
                    if (board[boardIndex][row][col].equals("X") || board[boardIndex][row][col].equals("O")) {
                        fillFactor++;
                    }
                }
            }
        }
        return fillFactor;
    }

    // Returns a formatted string of the board
    public String toString() {
        StringBuilder output = new StringBuilder();
        String[] outputArr = new String[27];
        int counter = 0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                for (int boardIndex = 0; boardIndex < 3; boardIndex++) {
                    String boardChar = board[col][row][boardIndex];
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
                    output.append(outputArr[k]).append(" ");
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

