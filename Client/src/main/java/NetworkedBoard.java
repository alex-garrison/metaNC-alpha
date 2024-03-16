import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkedBoard extends Board {
    private HashMap<Integer, String> players;

    public NetworkedBoard() {
        super();
        this.players = new HashMap<>();

    }

    // Adds a player to the board and assigns them a player which is then returned
    public String addPlayer(int clientID) throws GameException {
        if (players.keySet().size() >= 2) {
            throw new GameException("Players already assigned");
        } else {
            String player = new String[]{"X", "O"}[new Random().nextInt(2)];
            if (players.containsValue(player)) {
                player = invertPlayer(player);
            }
            players.put(clientID, player);
            return player;
        }
    }

    // Returns the clientID of the given player
    public int getClientID(String player) {
        for (Map.Entry<Integer, String> entry: players.entrySet()) {
            if (entry.getValue().equals(player)) {
                return entry.getKey();
            }
        }
        return 0;
    }

    public void clearPlayers() {
        players.clear();
    }

    // Returns the clientID of the current player
    public int getCurrentClientID() {
        while (true) {
            if (players.keySet().size() == 2) {
                for (Map.Entry<Integer, String> entry : players.entrySet()) {
                    if(entry.getValue().equals(turn)) {
                        return entry.getKey();
                    }
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Returns a serialised version of the board
    public String serialiseBoard() {
        StringBuilder output = new StringBuilder();
        output.append("|||");

        // Write the board to the output
        for (int boardIndex = 0; boardIndex < board.length; boardIndex++) {
            for (int row = 0; row < board[boardIndex].length; row++) {
                for (int col = 0; col < board[boardIndex][row].length; col++) {
                    String cell = board[boardIndex][row][col];
                    if (cell.equals("")) {cell = " ";}
                    output.append(cell);
                }
                if (row < 2) {
                    output.append("/");
                }
            }
            output.append("|||");
        }

        // Write the metadata to the output
        String lastMoveString;
        if (lastMove[0] == -1) {
            lastMoveString = "---";
        } else {
            lastMoveString = String.valueOf(lastMove[0]) + (lastMove[1]) + (lastMove[2]);
        }
        output.append("<").append(lastMoveString);
        output.append(",").append(turn).append(">");
        return output.toString();
    }

    // Takes a serialised board and parses it into the current board
    public void deserializeBoard(String serialisedBoard) throws GameException {
        Pattern boardPattern = Pattern.compile("(?=\\|\\|\\|(.*?)\\|\\|\\|)");
        Matcher boardMatcher = boardPattern.matcher(serialisedBoard);

        String[] serialisedBoards = new String[9];
        int i = 0;

        // Use regex to find the sub-boards in the serialised board
        while (boardMatcher.find()) {
            String match = boardMatcher.group(1);
            serialisedBoards[i] = match;
            i++;
        }

        if (i == 9) {
            int rowCounter = 0;
            int colCounter = 0;
            for (int boardIndex = 0; boardIndex < serialisedBoards.length; boardIndex++) {
                for (String row: serialisedBoards[boardIndex].split("/")) {
                    for (String cell: row.split("")) {
                        // Parse in the cells
                        if (cell.equals(" ")) cell = "";
                        board[boardIndex][rowCounter][colCounter] = cell;
                        colCounter++;
                    }
                    colCounter = 0;
                    rowCounter++;
                }
                rowCounter = 0;
            }

            // Use regex to find the metadata in the serialised board
            Pattern metadataPattern = Pattern.compile("<(.+)>");
            Matcher metadataMatcher = metadataPattern.matcher(serialisedBoard);
            if (!metadataMatcher.find()) throw new GameException("Error deserializing");
            String[] metaData = metadataMatcher.group(1).split(",");

            String[] lastMoveString = metaData[0].split("");
            int[] lastMoveArr = new int[3];
            for (int j = 0; j < 3; j++) {
                if (lastMoveString[j].equals("-")) {
                    lastMoveArr[j] = -1;
                } else {
                    lastMoveArr[j] = Integer.parseInt(lastMoveString[j]);
                }
            }
            // Parse in the metadata
            lastMove = lastMoveArr;
            turn = metaData[1];
        } else {
            throw new GameException("Error deserializing");
        }
    }

    // Makes a turn on the board using a player’s client identifier (Overloaded version)
    public void turn(int[] location, int clientID) throws GameException {
            String player = players.get(clientID);

            if (player == null) {
                throw new GameException("No player for this clientID");
            }
            if (player.equals(turn)) {
                if (isValidMove(location) && !isInWonBoard(location) && isInCorrectSubBoard(location)) {
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
}

