public class Evaluate {
    final static int WIN_SCORE = 100;
    final static int TWO_IN_A_ROW_SCORE = 16;
    final static double TWO_IN_A_ROW_WEIGHT = 1.5;
    final static int BLOCKING_SCORE = 14;
    final static int CENTER_SCORE = 6;
    final static int CORNER_SCORE = 3;

    public static int evaluateLocal(String[][] board, String player) {
        int boardScore = 0;

        for (String currPlayer : new String[]{"X","O"}) {
            for (int j = 0; j < board.length; j++) {
                if (board[j][0].equals(currPlayer) && board[j][1].equals(currPlayer) && board[j][2].equals(currPlayer)) {
                    if (currPlayer.equals(player)) {
                        return WIN_SCORE;
                    } else {
                        return -WIN_SCORE;
                    }
                } else if (board[0][j].equals(currPlayer) && board[1][j].equals(currPlayer) && board[2][j].equals(currPlayer)){
                    if (currPlayer.equals(player)) {
                        return WIN_SCORE;
                    } else {
                        return -WIN_SCORE;
                    }
                }
            }
            if ((board[0][0].equals(currPlayer) && board[1][1].equals(currPlayer) && board[2][2].equals(currPlayer))) {
                if (currPlayer.equals(player)) {
                    return WIN_SCORE;
                } else {
                    return -WIN_SCORE;
                }
            }
            else if (board[0][2].equals(currPlayer) && board[1][1].equals(currPlayer) && board[2][0].equals(currPlayer)) {
                if (currPlayer.equals(player)) {
                    return WIN_SCORE;
                } else {
                    return -WIN_SCORE;
                }
            }
        }

        if (board[1][1].equals(player)) {
            boardScore += CENTER_SCORE;
        } else if (board[1][1].equals(Board.invertPlayer(player))) {
            boardScore -= CENTER_SCORE;
        }

        if (board[0][0].equals(player)) {
            boardScore += CORNER_SCORE;
        } else if (board[0][0].equals(Board.invertPlayer(player))) {
            boardScore -= CORNER_SCORE;
        }

        if (board[0][2].equals(player)) {
            boardScore += CORNER_SCORE;
        } else if (board[0][2].equals(Board.invertPlayer(player))) {
            boardScore -= CORNER_SCORE;
        }

        if (board[2][0].equals(player)) {
            boardScore += CORNER_SCORE;
        } else if (board[2][0].equals(Board.invertPlayer(player))) {
            boardScore -= CORNER_SCORE;
        }

        if (board[2][2].equals(player)) {
            boardScore += CORNER_SCORE;
        } else if (board[2][2].equals(Board.invertPlayer(player))) {
            boardScore -= CORNER_SCORE;
        }

        boolean isBlocked;

        for (int row = 0; row < board.length; row++) {
            isBlocked = false;
            int playerCount = 0;
            int otherPlayerCount = 0;

            for (int col = 0; col < board.length; col++) {
                if (board[row][col].equals(player)) {
                    playerCount++;
                } else if (board[row][col].equals(Board.invertPlayer(player))) {
                    otherPlayerCount++;
                }
            }

            if (playerCount == 1 && otherPlayerCount == 2) {
                boardScore += BLOCKING_SCORE;
                isBlocked = true;
            } else if (playerCount == 2 && otherPlayerCount == 1) {
                boardScore -= BLOCKING_SCORE;
                isBlocked = true;
            }

            if (!isBlocked) {
                if (playerCount == 2 && otherPlayerCount == 0) {
                    boardScore += TWO_IN_A_ROW_SCORE;
                } else if (playerCount == 0 && otherPlayerCount == 2) {
                    boardScore -= TWO_IN_A_ROW_SCORE*TWO_IN_A_ROW_WEIGHT;
                }
            }
        }

        for (int row = 0; row < board.length; row++) {
            isBlocked = false;
            int playerCount = 0;
            int otherPlayerCount = 0;

            for (int col = 0; col < board.length; col++) {
                if (board[col][row].equals(player)) {
                    playerCount++;
                } else if (board[col][row].equals(Board.invertPlayer(player))) {
                    otherPlayerCount++;
                }
            }

            if (playerCount == 1 && otherPlayerCount == 2) {
                boardScore += BLOCKING_SCORE;
                isBlocked = true;
            } else if (playerCount == 2 && otherPlayerCount == 1) {
                boardScore -= BLOCKING_SCORE;
                isBlocked = true;
            }

            if (!isBlocked) {
                if (playerCount == 2 && otherPlayerCount == 0) {
                    boardScore += TWO_IN_A_ROW_SCORE;
                } else if (playerCount == 0 && otherPlayerCount == 2) {
                    boardScore -= TWO_IN_A_ROW_SCORE*TWO_IN_A_ROW_WEIGHT;
                }
            }
        }

        isBlocked = false;
        int playerCount = 0;
        int otherPlayerCount = 0;

        for (int i = 0; i < board.length; i++) {
            if (board[i][i].equals(player)) {
                playerCount++;
            } else if (board[i][i].equals(Board.invertPlayer(player))) {
                otherPlayerCount++;
            }
        }


        if (playerCount == 1 && otherPlayerCount == 2) {
            boardScore += BLOCKING_SCORE;
            isBlocked = true;
        } else if (playerCount == 2 && otherPlayerCount == 1) {
            boardScore -= BLOCKING_SCORE;
            isBlocked = true;
        }

        if (!isBlocked) {
            if (playerCount == 2 && otherPlayerCount == 0) {
                boardScore += TWO_IN_A_ROW_SCORE;
            } else if (playerCount == 0 && otherPlayerCount == 2) {
                boardScore -= TWO_IN_A_ROW_SCORE*TWO_IN_A_ROW_WEIGHT;
            }
        }

        isBlocked = false;
        playerCount = 0;
        otherPlayerCount = 0;

        for (int i = 0; i < board.length; i++) {
            if (board[i][board.length - i - 1].equals(player)) {
                playerCount++;
            } else if (board[i][board.length - i - 1].equals(Board.invertPlayer(player))) {
                otherPlayerCount++;
            }
        }

        if (playerCount == 1 && otherPlayerCount == 2) {
            boardScore += BLOCKING_SCORE;
            isBlocked = true;
        } else if (playerCount == 2 && otherPlayerCount == 1) {
            boardScore -= BLOCKING_SCORE;
            isBlocked = true;
        }

        if (!isBlocked) {
            if (playerCount == 2 && otherPlayerCount == 0) {
                boardScore += TWO_IN_A_ROW_SCORE;
            } else if (playerCount == 0 && otherPlayerCount == 2) {
                boardScore -= TWO_IN_A_ROW_SCORE*TWO_IN_A_ROW_WEIGHT;
            }
        }

        if (boardScore > 0) {
            return Math.min(boardScore, WIN_SCORE-1);
        } else if (boardScore < 0) {
            return Math.max(boardScore, -WIN_SCORE+1);
        } else {
            return boardScore;
        }
    }

    public static int winThreat(String[][] board, String player) {
        boolean playerWinThreat = false;
        boolean otherPlayerWinThreat = false;

        for (int i = 0; i < board.length; i++) {
            int playerCount = 0;
            int otherPlayerCount = 0;

            for (int j = 0; j < board.length; j++) {
                if (board[i][j].equals(player)) {
                    playerCount++;
                } else if (board[i][j].equals(Board.invertPlayer(player))) {
                    otherPlayerCount++;
                }
            }

                if (playerCount == 2 && otherPlayerCount == 0) {
                    playerWinThreat = true;
                } else if (playerCount == 0 && otherPlayerCount == 2) {
                    otherPlayerWinThreat = true;
                }
        }

        for (int i = 0; i < board.length; i++) {
            int playerCount = 0;
            int otherPlayerCount = 0;

            for (int j = 0; j < board.length; j++) {
                if (board[j][i].equals(player)) {
                    playerCount++;
                } else if (board[j][i].equals(Board.invertPlayer(player))) {
                    otherPlayerCount++;
                }
            }

                if (playerCount == 2 && otherPlayerCount == 0) {
                    playerWinThreat = true;
                } else if (playerCount == 0 && otherPlayerCount == 2) {
                    otherPlayerWinThreat = true;
                }
        }

        int playerCount = 0;
        int otherPlayerCount = 0;

        for (int i = 0; i < board.length; i++) {
            if (board[i][i].equals(player)) {
                playerCount++;
            } else if (board[i][i].equals(Board.invertPlayer(player))) {
                otherPlayerCount++;
            }
        }

            if (playerCount == 2 && otherPlayerCount == 0) {
                playerWinThreat = true;
            } else if (playerCount == 0 && otherPlayerCount == 2) {
                otherPlayerWinThreat = true;
            }

        playerCount = 0;
        otherPlayerCount = 0;

        for (int i = 0; i < board.length; i++) {
            if (board[i][board.length - i - 1].equals(player)) {
                playerCount++;
            } else if (board[i][board.length - i - 1].equals(Board.invertPlayer(player))) {
                otherPlayerCount++;
            }
        }

        if (playerCount == 2 && otherPlayerCount == 0) {
            playerWinThreat = true;
        } else if (playerCount == 0 && otherPlayerCount == 2) {
            otherPlayerWinThreat = true;
        }

        if (otherPlayerWinThreat) {
            return -1;
        } else if (playerWinThreat) {
            return 1;
        }

        return 0;
    }


    public static double evaluateGlobal(Board board, String player) {
        board.getLocalBoardWins(); board.isWin();

        int globalBoardScore;

        int currBoardLoc = board.lastMove[0];
        int currBoardScore = evaluateLocal(board.board[currBoardLoc], player);

        int nextBoardLoc = board.lastMove[1] * 3 + board.lastMove[2];
        int nextBoardScore = 0;

        if (board.wonBoards.contains(nextBoardLoc)) {
            for (int boardIndex: board.openBoards) {
                int winThreat = winThreat(board.board[boardIndex], player);

                if (winThreat == 1) {
                    nextBoardScore += 60;

                } else if (winThreat == -1) {
                    nextBoardScore -= 80;
                }
            }
        } else {
            nextBoardScore = evaluateLocal(board.board[nextBoardLoc], player);

            int winThreat = winThreat(board.board[nextBoardLoc], player);

            if (winThreat == 1) {
                nextBoardScore += 40;
            } else if (winThreat == -1) {
                nextBoardScore -= 60;
            }
        }

        String[][] globalBoard = representGlobalAsLocal(board);
        globalBoardScore = evaluateLocal(globalBoard, player);

        if (winThreat(globalBoard, player) == 1) {
            globalBoardScore += 30;
        } else if (winThreat(globalBoard, player) == -1) {
            globalBoardScore -= 80;
        }

        int globalBoardWeight = (int) (Math.exp((board.getFillFactor() / 20f)) / 5) + 1;

        return (int) Math.ceil(((3*currBoardScore + 2*nextBoardScore) + (globalBoardWeight * globalBoardScore)) / 5f);
    }

    public static String[][] representGlobalAsLocal(Board board) {
        String[][] localBoard = new String[3][3];
        board.getLocalBoardWins();
        for (int boardIndex = 0; boardIndex < board.board.length; boardIndex++) {
            int row = Math.floorDiv(boardIndex, 3);
            int col = boardIndex % 3;
            if (board.isWonBoard(boardIndex)) {
                localBoard[row][col] = board.localBoardWins[boardIndex].getWinner();
            } else {
                localBoard[row][col] = "";
            }
        }

        return localBoard;
    }
}
