import java.util.Arrays;
import java.util.ConcurrentModificationException;

public class Lobby implements Runnable {
    public int lobbyID;

    private final NetworkedBoard serverNetworkedBoard;
    private final ServerClient[] serverClients;

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

    public void run() {
        output("Started : " + Arrays.toString(serverClients));

        awaitNewGame();

        lobbyGameLoop();

        output("Stopping lobby");

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

    public synchronized void turn(int[] location, int clientID) {
        if (lobbyRunning && gameRunning) {
            try {
                serverNetworkedBoard.turn(location, clientID);

                broadcast("BOARD:"+ serverNetworkedBoard.serialiseBoard());

                if (!serverNetworkedBoard.isWin()) {
                    Server.send(getServerClientFromClientID(serverNetworkedBoard.getCurrentClientID()), "AWAITTURN");
                }
            } catch (GameException e) {
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

    private void broadcast(String message) {
        for (ServerClient serverClient : serverClients) {
            Server.send(serverClient, message);
        }
    }

    public void newGame() {
        isNewGame = true;
    }

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

    private void lobbyGameLoop() {
        lobbyGameLoop: while (lobbyRunning) {
            gameRunning = false;

            output("Starting new game");
            boolean clientsAssigned = setupClients();

            if (clientsAssigned) {
                serverNetworkedBoard.resetBoard();
                serverNetworkedBoard.setStarter("X");

                broadcast("BOARD:"+ serverNetworkedBoard.serialiseBoard());

                gameRunning = true;

                Server.send(getServerClientFromClientID(serverNetworkedBoard.getCurrentClientID()), "AWAITTURN");

                while (gameRunning && lobbyRunning) {
                    if (serverNetworkedBoard.isWon) {
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

    public boolean setupClients() {
        boolean clientsAssigned = false;

        try {
            serverNetworkedBoard.clearPlayers();
            for (ServerClient serverClient : serverClients) {
                try {
                    String player = serverNetworkedBoard.addPlayer(serverClient.getClientID());
                    output("C"+serverClient.getClientID() + " assigned player : " + player);
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

    public ServerClient getServerClientFromClientID(int clientID) {
        for (ServerClient serverClient: serverClients) {
            if (serverClient.getClientID() == clientID) {
                return serverClient;
            }
        }
        return null;
    }

    public void serverClientDisconnected(ServerClient serverClient) {
        output("C" + serverClient.getClientID() + " disconnected");
        Server.serverClientDisconnected(serverClient, false);
        stopRunning();
    }

    public void stopRunning() {
        lobbyRunning = false; gameRunning = false;
    }
}
