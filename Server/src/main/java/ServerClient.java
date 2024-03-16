import java.io.IOException;
import java.net.Socket;

public class ServerClient {
    private Socket serverClientSocket;
    private ServerClientHandler serverClientHandler;
    private Thread serverClientHandlerThread;
    private int clientID;
    private boolean isAuthorised;
    private boolean isConnected;
    private Lobby lobby;

    public ServerClient(Socket serverClientSocket, ServerClientHandler serverClientHandler, Thread serverClientHandlerThread) {
        this.serverClientSocket = serverClientSocket;
        this.serverClientHandler = serverClientHandler;
        this.serverClientHandlerThread = serverClientHandlerThread;
        this.isAuthorised = false;
        this.isConnected = true;

        // Creates the client identifier from a hash of the socket and the ServerClientHandler process
        this.clientID = (this.hashCode() / 100000);

        this.serverClientHandler.setClientID(this.clientID);
        this.serverClientHandler.setServerClient(this);

        // Begin monitoring the client's connection and authorisation status
        new Thread(new authMonitor(this)).start();
        new Thread(new connMonitor(this)).start();
    }

    public void stopClient() {
        serverClientHandler.stopRunning();
    }

    // Called when the client authorises themselves
    public void authoriseClient() {
        isAuthorised = true;
        output("New client authorised");
        Server.clientHandler.addAuthorisedClient(this);
        Server.serverClients.add(this);
    }

    public void output(String text) {
        Server.print("C" + clientID + ": " + text);
    }

    public int getClientID() {
        return clientID;
    }

    public boolean isAuthorised() {
        return isAuthorised;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public ServerClientHandler getServerClientHandler() {
        return serverClientHandler;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
        serverClientHandler.setLobby(lobby);
    }

    public String toString() {
        return "C" + clientID;
    }

    private class connMonitor implements Runnable {
        ServerClient serverClient;
        public connMonitor(ServerClient serverClient) {
            this.serverClient = serverClient;
        }

        // Starts the connection monitoring process
        public void run() {
            // Waits for the server-client handling thread to complete
            try {
                serverClientHandlerThread.join();
            } catch (InterruptedException e) {
                output("Error waiting for serverClientHandlerThread to stop");
            } finally {
                // Gracefully disconnect the server-client
                if (serverClientSocket != null) {
                    try {
                        serverClientSocket.close();
                    } catch (IOException e) {
                        output("Error closing serverClientSocket : " + e);
                    }
                }

                if (isAuthorised) {
                    // Remove the server-client from the server’s, and lobby’s, list of connected clients
                    if (lobby != null) {
                        lobby.serverClientDisconnected(serverClient);
                    } else {
                        Server.serverClientDisconnected(serverClient);
                    }
                }
                isConnected = false;
            }
        }
    }

    // Starts the authorisation monitoring process
    private class authMonitor implements Runnable {
        ServerClient serverClient;
        public authMonitor(ServerClient serverClient) {
            this.serverClient = serverClient;
        }
        public void run() {
            try {
                // Waits 20 seconds for the server-client to authorise itself
                Thread.sleep(20000);

                if (!isAuthorised) {
                    // If the server-client has not authorised itself, disconnect the server-client
                    serverClientHandler.send("DISCONNECT");
                    serverClient.stopClient();
                }
            } catch (InterruptedException e) {
                output("Error waiting for serverClientHandlerThread to stop");
            }
        }
    }
}
