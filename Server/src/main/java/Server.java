import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.TreeSet;

public class Server implements Runnable {
    private static final int PORT = 8000;
    private final int TIMEOUT_MILLIS = 2000;

    public static Server server;
    public static int serverID;
    public static String serverKey;

    private static ServerSocket serverSocket;
    public static ArrayList<ServerClient> serverClients;

    public static ArrayList<Lobby> lobbies;
    private static int lobbyCounter = 1;
    private static TreeSet<Integer> availableLobbyIDs;

    private static clientHandler clientHandler;
    private static loggingHandler loggingHandler;

    private static boolean isHeadless;
    private static boolean isLogging;
    private static boolean serverRunning;

    public Server(String serverKey) {
        serverClients = new ArrayList<>();
        lobbies = new ArrayList<>();
        availableLobbyIDs = new TreeSet<>();
        serverRunning = true;
        isHeadless = ServerMain.isHeadless();
        isLogging = ServerMain.isLogging();
        this.serverKey = serverKey;

        server = this;
        serverID = this.hashCode();
    }

    // Begins the server process
    @Override
    public void run() {
        if (isLogging) {
            // Start logging handler thread
            loggingHandler = new loggingHandler();
            Thread loggingHandlerThread = new Thread(loggingHandler);
            loggingHandlerThread.start();
        }

        output("Started server");

        // Open server socket and binds it to a port
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

        if (serverKey == null) {
            serverKey = Client.getServerKey();
        }

        // Start client handler thread and wait for it to stop
        clientHandler = new clientHandler();
        Thread clientHandlerThread = new Thread(clientHandler);
        clientHandlerThread.start();
        try {
            clientHandlerThread.join();
        } catch (InterruptedException e) {
            Server.output("clientHandlerThread stopped");
        }

        // Disconnect from clients and close server socket
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
        if (isLogging) Server.loggingHandler.stopRunning();
    }

    // Broadcasts a message to all server-clients
    private static void broadcast(String message) {
        for (ServerClient  serverClient : serverClients) {
            send(serverClient, message);
        }
    }

    // Sends a message to a server-client
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

    // Prints a message to the server and logs it if logging is enabled
    public static void print(String text) {
        if (isHeadless) {
            System.out.println(text);
        } else {
            ServerGUI.frame.printToServer(text);
        }

        if (isLogging) {
            Server.loggingHandler.log(text);
        }
    }

    public static void setNetworkLabel() {
        try {
            ServerGUI.setNetworkLabel(InetAddress.getLocalHost(), PORT);
        } catch (UnknownHostException e) {
            output("Error setting network label : " + e);
        }
    }

    // Closes all the currently connected server-clients
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

    // Removes a disconnected server-client from the server's queue of waiting server-clients
    public static void serverClientDisconnected(ServerClient serverClient) {
        output("C" + serverClient.getClientID() + " disconnected");
        serverClients.remove(serverClient);
        Server.clientHandler.removeFromWaiting(serverClient);
    }


    // Returns a suitable lobby identifier
    private static int getLobbyID() {
        int lobbyID;
        if (availableLobbyIDs.size() > 0) {
            // Get the first available lobbyID from the pool
            try {
                lobbyID =  availableLobbyIDs.pollFirst();
            } catch (NullPointerException e) {
                lobbyID = lobbyCounter;
                lobbyCounter++;
            }
        } else {
            // Generate a new lobbyID
            lobbyID = lobbyCounter;
            lobbyCounter++;
        }
        return lobbyID;
    }

    // Removes a lobby from the server's list of lobbies and return its lobbyID to the pool
    public static void removeLobby(Lobby lobby) {
        availableLobbyIDs.add(lobby.lobbyID);
        lobbies.remove(lobby);
        if (!isHeadless) {
            ServerGUI.frame.removeLobby(lobby);
        }
    }

    // Returns a server-client from it's clientID
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

    static class clientHandler implements Runnable {
        public static LinkedList<ServerClient> waitingClients = new LinkedList<>();

        // Continuously accepts new clients
        @Override
        public void run() {
            while (serverRunning) {
                try {
                    Socket serverClientSocket = serverSocket.accept();
                    ServerClientHandler serverClientHandler = new ServerClientHandler(serverClientSocket);
                    Thread serverClientHandlerThread = new Thread(serverClientHandler);

                    // Encapsulates the server-client and its handler in a new ServerClient object
                    ServerClient serverClient = new ServerClient(serverClientSocket, serverClientHandler, serverClientHandlerThread);

                    // Starts the server-client handling thread
                    serverClientHandlerThread.start();
                } catch (SocketTimeoutException e) {} catch (IOException e) {
                    output("Error accepting client : " + e);
                }
            }
        }

        // Adds authorised server-client into the queue of waiting server-clients
        public static void addAuthorisedClient(ServerClient serverClient) {
            waitingClients.add(serverClient);

            synchronized (waitingClients) {
                if (waitingClients.size() >= 2) {
                    // If there are at least two waiting server-clients, create a new lobby with them

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
        }

        // Removes a server-client from the queue of waiting server-clients
        public void removeFromWaiting(ServerClient serverClient) {
            if (waitingClients.contains(serverClient)) {
                waitingClients.remove(serverClient);
            }
        }
    }

    static class loggingHandler implements Runnable {
        String logFileName = "serverLog.txt";
        FileWriter fileWriter;
        boolean keepRunning = true;

        // Begins the logging handler
        @Override
        public void run() {
            try {
                // Opens the log file
                fileWriter = new FileWriter(logFileName, false);

                while (keepRunning) {
                    Thread.sleep(100);
                }

                // Closes the log file once the process has stopped
                fileWriter.close();
            } catch (IOException e) {
                output("Could not open log file");
            } catch (InterruptedException e) {
                output("Error with loggingHandler thread");
            }
        }

        // Appends a new message to the log file
        public void log(String message) {
            boolean messageSent = false;
            int errorCount = 0;

            while (keepRunning && fileWriter != null && messageSent != true) {
                try {
                    // Formats the message with a timestamp
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    String timestamp = dateFormat.format(new Date());

                    fileWriter.write("[" + timestamp + "] " + message.strip());
                    fileWriter.write("\n");
                    fileWriter.flush();

                    messageSent = true;
                } catch (IOException e) {
                    if (errorCount >= 5) {
                        keepRunning = false;
                    } else {
                        errorCount++;
                    }
                }
            }
        }

        public void stopRunning() {
            this.keepRunning = false;
        }
    }
}
