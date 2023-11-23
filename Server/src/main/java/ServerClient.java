import java.io.IOException;
import java.net.Socket;

public class ServerClient {
    private final Socket serverClientSocket;
    private final ServerClientHandler serverClientHandler;
    private final Thread serverClientHandlerThread;
    private final int clientID;
    private boolean isAuthorised;
    private boolean isConnected;
    private Lobby lobby;

    public ServerClient(Socket serverClientSocket, ServerClientHandler serverClientHandler, Thread serverClientHandlerThread) {
        this.serverClientSocket = serverClientSocket;
        this.serverClientHandler = serverClientHandler;
        this.serverClientHandlerThread = serverClientHandlerThread;
        this.clientID = (this.hashCode() / 100000);
        this.isAuthorised = false;
        this.isConnected = true;

        this.serverClientHandler.setClientID(this.clientID);
        this.serverClientHandler.setServerClient(this);

        new Thread(new authMonitor(this)).start();
        new Thread(new monitor(this)).start();
    }

    public void stopClient() {
        serverClientHandler.stopRunning();
    }

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

    private class monitor implements Runnable {
        ServerClient serverClient;
        public monitor(ServerClient serverClient) {
            this.serverClient = serverClient;
        }
        public void run() {
            try {
                serverClientHandlerThread.join();
            } catch (InterruptedException e) {
                output("Error waiting for serverClientHandlerThread to stop");
            } finally {
                if (serverClientSocket != null) {
                    try {
                        serverClientSocket.close();
                    } catch (IOException e) {
                        output("Error closing serverClientSocket : " + e);
                    }
                }

                if (isAuthorised) {
                    if (lobby != null) {
                        lobby.serverClientDisconnected(serverClient);
                    } else {
                        Server.serverClientDisconnected(serverClient, false);
                    }
                }
                isConnected = false;
            }
        }
    }

    private class authMonitor implements Runnable {
        ServerClient serverClient;
        public authMonitor(ServerClient serverClient) {
            this.serverClient = serverClient;
        }
        public void run() {
            try {
                Thread.sleep(20000);

                if (!isAuthorised) {
                    serverClientHandler.send("DISCONNECT");
                    serverClient.stopClient();
                }
            } catch (InterruptedException e) {
                output("Error waiting for serverClientHandlerThread to stop");
            }
        }
    }
}
