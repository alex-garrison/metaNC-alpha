import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerClientHandler implements Runnable {
    private Socket serverClientSocket;
    private ServerClient serverClient;
    private Lobby lobby;

    private ServerClientReader reader;
    private ServerClientWriter writer;
    private boolean keepRunning;

    private int clientID = 0;

    private final int TIMEOUT_MILLIS = 500;

    public ServerClientHandler(Socket serverClientSocket) {
        this.serverClientSocket = serverClientSocket;
        try {
            serverClientSocket.setSoTimeout(TIMEOUT_MILLIS);
        } catch (IOException e) {
            output("Error setting timeout");
        }

        keepRunning = true;
    }

    // Starts the server-client handling process
    @Override
    public void run() {
        // Start the reader and writer processes
        reader = new ServerClientReader();
        Thread readerThread = new Thread(reader);
        readerThread.start();

        writer = new ServerClientWriter();

        // Send the client the request for authentication
        writer.send("REQAUTH");

        while (keepRunning) {
            if (!reader.keepRunning) {
                keepRunning = false; continue;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Closes the reader and writer processes
        reader.stopRunning();
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            output("Error waiting for readerThread to stop");
        }

        writer.close();
    }

    // Sends a message to the client
    public void send(String message) {
        while (true) {
            if (writer != null) {
                writer.send(message);
                break;
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public void setServerClient(ServerClient serverClient) {
        this.serverClient = serverClient;
    }

    public void setLobby(Lobby lobby) {
        if (lobby == null) {
            this.lobby = null;
            send("LOBBYDISCONNECT");
        } else {
            this.lobby = lobby;
            send("LOBBYID:"+lobby.lobbyID);
        }
    }

    private void output(String text) {
        if (serverClient.isAuthorised()) {
            Server.print("C" + clientID + ": " + text);
        }
    }

    public void stopRunning() {
        keepRunning = false;
    }

    private class ServerClientReader implements Runnable {
        private BufferedReader reader;
        private boolean keepRunning;

        private int nullDataCounter = 0;
        private int authFailCounter = 0;

        public ServerClientReader() {
            keepRunning = true;
        }

        // Begins the reader service
        public void run() {
            while (keepRunning) {
                if (reader == null) {
                    try {
                        // Create the reader
                        reader = new BufferedReader(new InputStreamReader(serverClientSocket.getInputStream()));
                    } catch (IOException e) {
                        output("Error initialising reader : " + e); keepRunning = false; continue;
                    }
                }

                if (!serverClientSocket.isClosed()) {
                    // Checks for null data and if there is a lot of it, stop the reader
                    try {
                        if (nullDataCounter >= 5) {
                            keepRunning = false;
                            continue;
                        }

                        // Reads the data from the server
                        String receivedData = reader.readLine();

                        if (receivedData == null) {
                            nullDataCounter++;
                            Thread.sleep(100);
                        } else if (!receivedData.isEmpty()) {
                            // Parse the data and execute the appropriate function
                            String[] args = receivedData.split(":");

                            if (!serverClient.isAuthorised()) {
                                // If the client is not authorised, check for the correct server key
                                try {
                                    if (args.length == 1 && args[0].equals("AUTH")) {
                                        writer.send("AUTHFAIL");
                                    } else if (Server.serverKey == null && args[0].equals("AUTH")) {
                                        writer.send("CLIENTID:" + clientID);
                                        writer.send("AUTHSUCCESS");
                                        serverClient.authoriseClient();
                                    } else if (args[0].equals("AUTH") && args[1].equals(Server.serverKey)) {
                                        writer.send("CLIENTID:" + clientID);
                                        writer.send("AUTHSUCCESS");
                                        serverClient.authoriseClient();
                                    } else if (args[0].equals("AUTH") &!args[1].equals(Server.serverKey)) {
                                        writer.send("AUTHFAIL");
                                        authFailCounter++;

                                        if (authFailCounter >= 3) {
                                            serverClient.stopClient();
                                        }
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    output("Error with AUTH command");
                                }

                            } else {
                                if (args[0].equals("TURN")) {
                                    try {
                                        String[] locationString = args[1].split("");
                                        int[] location = new int[3];
                                        for (int i = 0; i < location.length; i++) {
                                            location[i] = Integer.parseInt(locationString[i]);
                                        }
                                        lobby.turn(location, clientID);
                                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                        output("Error with TURN command");
                                    }
                                } else if (args[0].equals("DISCONNECT")) {
                                    stopRunning();
                                } else if (args[0].equals("NEWGAME")) {
                                    if (lobby != null) {
                                        lobby.newGame();
                                    }
                                } else {
                                    output("Client sent : " + receivedData);
                                }
                            }
                        }
                    }  catch (SocketTimeoutException e) {} catch (IOException e) {
                        if (serverClient.isAuthorised()) {
                            output("Error reading data : " + e);
                        }
                        keepRunning = false;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    output("Sockets closed");
                    keepRunning = false;
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    output("Error closing reader" + e);
                }
            }

            this.stopRunning();
        }

        public void stopRunning() {
            keepRunning = false;
        }
    }

    private class ServerClientWriter {
        private BufferedWriter writer;

        public ServerClientWriter() {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(serverClientSocket.getOutputStream()));
            } catch (IOException e) {
                output("Error initialising writer : " + e);
            }
        }

        // Sends the data passed to it to the server-client
        public void send(String message) {
            boolean messageSent = false;
            int errorCount = 0;

            while (!messageSent) {
                try {
                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                    messageSent = true;
                } catch (SocketTimeoutException e) {} catch (IOException e) {
                    // If there are too many errors, stop trying to send the message
                    if (errorCount >= 5) {
                        close();
                        return;
                    } else {
                        output("Error sending message : " + e);
                        errorCount++;
                    }
                }
            }
        }

        // Closes the writer
        public void close() {
            if (writer != null) {
                try {
                    writer.close();
                    keepRunning = false;
                } catch (IOException e) {
                    output("Error closing writer" + e);
                }
            }
        }
    }
}
