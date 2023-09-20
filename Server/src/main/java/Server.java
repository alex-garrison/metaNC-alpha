import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

public class Server implements Runnable {
    private static final int PORT = 8000;
    private final int TIMEOUT_MILLIS = 2000;

    public static Server server;
    public static int serverID;

    private ServerSocket serverSocket;
    private static ArrayList<ServerClient> serverClients;

    public static ArrayList<Lobby> lobbies;
    private static int lobbyCounter = 1;
    private static TreeSet<Integer> availableLobbyIDs;

    private static clientHandler clientHandler;

    private static boolean isHeadless;
    private static boolean serverRunning;

    public Server() {
        serverClients = new ArrayList<>();
        lobbies = new ArrayList<>();
        availableLobbyIDs = new TreeSet<>();
        serverRunning = true;
        isHeadless = ServerMain.isHeadless();

        server = this;
        serverID = this.hashCode();
    }

    @Override
    public void run() {
        output("Started server");

        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setSoTimeout(TIMEOUT_MILLIS);
        } catch (IOException e) {
            output("Error initialising server : " + e);
        }

        if (isHeadless) {
            try {
                output("Socket open: " + InetAddress.getLocalHost().getHostAddress() + ":" + PORT);
            } catch (UnknownHostException e) {
                output("Error getting localHost : " + e);
            }
        } else {
            setNetworkLabel();
        }

        clientHandler = new clientHandler();
        Thread clientHandlerThread = new Thread(clientHandler);
        clientHandlerThread.start();
        try {
            clientHandlerThread.join();
        } catch (InterruptedException e) {
            Server.output("clientHandlerThread stopped");
        }

        broadcast("DISCONNECT");
        if (serverClients.size() > 0) {
            output("Disconnecting from clients");
        }
        closeClients();

        try {
            serverSocket.close();
        } catch (IOException e) {
            output("Error closing serverSocket : " + e.getMessage());
        }

        output("Server stopped");
        if (!isHeadless) ServerMain.serverStopped();
    }

    private static void broadcast(String message) {
        for (ServerClient serverClient : serverClients) {
            send(serverClient, message);
        }
    }

    public static void send(ServerClient serverClient, String message) {
        if (serverClient != null) {
            try {
                serverClient.getServerClientHandler().send(message);
            } catch(Exception e){
                output("Error sending message : " + message + " : " + e);
            }
        }
    }

    private static void output(String text) {
        print("S: " + text);
    }

    public static void print(String text) {
        if (isHeadless) {
            System.out.println(text);
        } else {
            ServerGUI.frame.printToServer(text);
        }
    }

    public static void setNetworkLabel() {
        try {
            ServerGUI.setNetworkLabel(InetAddress.getLocalHost(), PORT);
        } catch (UnknownHostException e) {
            output("Error setting network label : " + e);
        }
    }

    private void closeClients() {
        for (ServerClient serverClient : serverClients) {
            serverClient.stopClient();
        }

        while (serverClients.size() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Server.output("Error waiting for clients to close : " + e.getMessage());
                return;
            }
        }
    }

    public static void serverClientDisconnected(ServerClient serverClient, boolean stopServer) {
        output("serverClient " + serverClient.getClientID() + " disconnected");
        serverClients.remove(serverClient);
        Server.clientHandler.removeFromWaiting(serverClient);
        if (stopServer) {
            serverRunning = false;
        }
    }

    private static int getLobbyID() {
        int lobbyID;
        if (availableLobbyIDs.size() > 0) {
            lobbyID =  availableLobbyIDs.pollFirst();
        } else {
            lobbyID = lobbyCounter;
            lobbyCounter++;
        }
        return lobbyID;
    }

    public static void removeLobby(Lobby lobby) {
        availableLobbyIDs.add(lobby.lobbyID);
        lobbies.remove(lobby);
        if (!isHeadless) {
            ServerGUI.frame.removeLobby(lobby);
        }
    }

    public static ServerClient getServerClientFromClientID(int clientID) {
        for (ServerClient serverClient: serverClients) {
            if (serverClient.getClientID() == clientID) {
                return serverClient;
            }
        }
        return null;
    }

    public static void stopRunning() {
        serverRunning = false;

        for (Lobby lobby: lobbies) {
            lobby.stopRunning();
        }
    }

    class clientHandler implements Runnable {
        private static final LinkedList<ServerClient> waitingClients = new LinkedList<>();
        @Override
        public void run() {
            while (serverRunning) {
                try {
                    Socket serverClientSocket = serverSocket.accept();
                    ServerClientHandler serverClientHandler = new ServerClientHandler(serverClientSocket);
                    Thread serverClientHandlerThread = new Thread(serverClientHandler);

                    ServerClient serverClient = new ServerClient(serverClientSocket, serverClientHandler, serverClientHandlerThread);
                    serverClientHandlerThread.start();

                    if (serverClient.isConnected()) {
                        serverClients.add(serverClient);

                        waitingClients.add(serverClient);
                    }

                    synchronized (waitingClients) {
                        if (waitingClients.size() >= 2) {
                            ServerClient player1 = waitingClients.removeFirst();
                            ServerClient player2 = waitingClients.removeFirst();

                            Lobby lobby = new Lobby(new ServerClient[]{player1, player2}, getLobbyID());
                            player1.setLobby(lobby);
                            player2.setLobby(lobby);

                            Thread lobbyThread = new Thread(lobby);
                            lobbyThread.start();

                            lobbies.add(lobby);
                        }
                    }
                } catch (SocketTimeoutException e) {} catch (IOException e) {
                    if (serverRunning) {
                        output("Error accepting serverClient : " + e.getMessage());
                    }
                }
            }
        }

        public void removeFromWaiting(ServerClient serverClient) {
            if (waitingClients.contains(serverClient)) {
                waitingClients.remove(serverClient);
            }
        }
    }
}
