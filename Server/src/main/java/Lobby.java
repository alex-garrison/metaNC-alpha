import java.util.Arrays;
import java.util.ConcurrentModificationException;

public class Lobby implements Runnable {
    public int lobbyID;

    private NetworkedBoard serverNetworkedBoard;
    private ServerClient[] serverClients;

    private boolean lobbyRunning;
    private boolean gameRunning;
    private boolean isNewGame;

    public Lobby(ServerClient[] serverClients, int lobbyID) {
        this.serverClients = serverClients;
        this.serverNetworkedBoard = new NetworkedBoard();

        this.lobbyRunning = true;
        this.gameRunning = false;
        this.isNewGame = false;

        this.lobbyID = lobbyID;

        if (!ServerMain.isHeadless()) {
            ServerGUI.frame.addLobby(this);
        }
    }

    // Begins the lobby process
    public void run() {
        output("Started : " + Arrays.toString(serverClients));

        // Waits for a new game and then begins the game loop
        awaitNewGame();

        lobbyGameLoop();

        output("Stopping lobby");

        // Remove the lobby and return the connected server-clients back to the pool of waiting server-clients
        Server.removeLobby(this);

        for (ServerClient serverClient: serverClients) {
            if (serverClient.isConnected()) {
                serverClient.getServerClientHandler().setLobby(null);
                Server.clientHandler.addAuthorisedClient(serverClient);
            } else {
                serverClient.stopClient();
            }
        }
    }

    // Validates and makes a move on the lobby’s board
    public synchronized void turn(int[] location, int clientID) {
        if (lobbyRunning && gameRunning) {
            try {
                // Broadcasts the new board state to the server-clients in the lobby
                serverNetworkedBoard.turn(location, clientID);

                broadcast("BOARD:"+ serverNetworkedBoard.serialiseBoard());

                // Checks for win conditions and requests the other player to make a move
                if (!serverNetworkedBoard.isWin()) {
                    Server.send(getServerClientFromClientID(serverNetworkedBoard.getCurrentClientID()), "AWAITTURN");
                }
            } catch (GameException e) {
                // If the move is invalid, it will prompt the server-client to make another
                ServerClient serverClient = getServerClientFromClientID(clientID);
                if (serverClient != null) {
                    Server.send(serverClient, "ERROR:"+e.getMessage());
                } else {
                    output("Error with turn : " + e.getMessage());
                }

                if (e.getMessage().equals("Move not valid")) {
                    Server.send(serverClient, "AWAITTURN");
                }
            }
        }
    }

    private void output(String text) {
        if (ServerMain.isHeadless()) {
            Server.print("L" + lobbyID + ": " + text);
        } else {
            ServerGUI.frame.printToLobby(text, this);
        }
    }

    // Broadcasts a message to all server-clients in the lobby
    private void broadcast(String message) {
        for (ServerClient serverClient : serverClients) {
            Server.send(serverClient, message);
        }
    }

    public void newGame() {
        isNewGame = true;
    }

    // Waits for a new game to be requested
    private void awaitNewGame() {
        isNewGame = false;

        while (lobbyRunning) {
            if (!isNewGame) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                break;
            }
        }
        isNewGame = false;
    }

    // Begins the game loop
    private void lobbyGameLoop() {
        lobbyGameLoop: while (lobbyRunning) {
            gameRunning = false;

            output("Starting new game");

            // Assigns players to the server-clients
            boolean clientsAssigned = setupClients();

            if (clientsAssigned) {
                serverNetworkedBoard.resetBoard();
                serverNetworkedBoard.setStarter("X");

                broadcast("BOARD:"+ serverNetworkedBoard.serialiseBoard());

                gameRunning = true;

                // Requests a move from the player whose turn it is
                Server.send(getServerClientFromClientID(serverNetworkedBoard.getCurrentClientID()), "AWAITTURN");

                while (gameRunning && lobbyRunning) {
                    // Continually checks for win conditions on the lobby board
                    if (serverNetworkedBoard.isWon) {
                        // If the board is won or drawn, it will notify the server-clients and wait for a new game
                        broadcast("BOARDWON:" + serverNetworkedBoard.winner);
                        if (serverNetworkedBoard.winner.equals("D")) {
                            output("Draw");
                        } else {
                            int clientWinner = serverNetworkedBoard.getClientID(serverNetworkedBoard.winner);
                            if (clientWinner != 0) {
                                output("Board won by C" + clientWinner);
                            }
                        }

                        awaitNewGame();
                        broadcast("NEWGAME");
                        continue lobbyGameLoop;
                    } else if (isNewGame) {
                        // If a new game is prematurely requested, it will begin a new game
                        broadcast("NEWGAME");
                        isNewGame = false;
                        continue lobbyGameLoop;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    // Assigns players to the server-clients and returns whether the operation was successful
    public boolean setupClients() {
        boolean clientsAssigned = false;

        try {
            serverNetworkedBoard.clearPlayers();
            for (ServerClient serverClient : serverClients) {
                try {
                    String player = serverNetworkedBoard.addPlayer(serverClient.getClientID());
                    output("C"+serverClient.getClientID() + " assigned player : " + player);

                    // Notifies the server-client of their assigned player
                    Server.send(serverClient, "ASSIGNPLAYER:" + player);
                    clientsAssigned = true;
                } catch (GameException e) {
                    output("Error assigning player : " + e.getMessage());
                    clientsAssigned = false;
                    break;
                }
            }
        } catch (ConcurrentModificationException e) {
            output("Error assigning players");
            clientsAssigned = false;
        }


        return clientsAssigned;
    }

    // Returns the server-client with the given clientID
    public ServerClient getServerClientFromClientID(int clientID) {
        for (ServerClient serverClient: serverClients) {
            if (serverClient.getClientID() == clientID) {
                return serverClient;
            }
        }
        return null;
    }

    // Notifies the lobby that a server-client has disconnected
    public void serverClientDisconnected(ServerClient serverClient) {
        output("C" + serverClient.getClientID() + " disconnected");
        Server.serverClientDisconnected(serverClient);
        stopRunning();
    }

    public void stopRunning() {
        lobbyRunning = false; gameRunning = false;
    }
}
