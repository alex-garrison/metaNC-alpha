import javax.swing.*;
import java.io.*;
import java.net.*;

public class Client implements Runnable {
    private InetAddress host;
    private int port;

    private final int TIMEOUT_MILLIS = 500;

    private String player;
    private boolean isClientTurn;
    private boolean boardWon;

    private Socket clientSocket;
    private boolean isConnected;
    private ClientWriter writer;
    private ClientReader reader;

    public Client(InetAddress host, int port) {
        this.host = host;
        this.port = port;

        isConnected = false;
        boardWon = false;
    }

    // Connects to the server and handles the UI elements associated with networking
    public void run() {
        ClientGUI.frame.resetBoardPanels();
        ClientGUI.frame.clearBottomLabel();
        ClientGUI.frame.clearNetworkLabel();
        ClientGUI.frame.clearPlayerLabel();

        if (connectToServer()) {
            ClientGUI.frame.setNetworkLabel("Connecting", false);
            ClientGUI.frame.printToLog("Connected to server : " + clientSocket.getInetAddress());
            isConnected = true;
            ClientGUI.frame.setNetworked(true);
            ClientGUI.frame.setNetworkButtonFunction(false);
            ClientGUI.frame.setNetworkMode(true);

            ClientGUI.frame.setNewGameEnabled(false);

            // Start the reader and writer threads
            reader = new ClientReader();
            Thread readerThread = new Thread(reader);
            readerThread.start();

            writer = new ClientWriter();

            // Wait for the reader thread to stop
            try {
                readerThread.join();
            } catch (InterruptedException e) {
                ClientGUI.frame.printToLog("Error waiting for readerThread to stop");
            }
            writer.close();

            isConnected = false;
            ClientGUI.frame.setNetworkLabel("Disconnected" , true);
            ClientGUI.frame.clearBottomLabel();
            ClientGUI.frame.clearPlayerLabel();
            ClientGUI.frame.setNetworkMode(false);
            ClientGUI.frame.setNetworked(false);
            ClientGUI.frame.setNetworkButtonFunction(true);
            ClientGUI.frame.setNewGameEnabled(true);
            ClientGUI.frame.setClientID(0); ClientGUI.frame.setLobbyID(0);

        } else {
            ClientGUI.frame.setNetworkLabel("Error connecting" , true);
        }

        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                ClientGUI.frame.printToLog("Error closing socket");
            }
        }

        ClientGUI.frame.printToLog("Client stopped");

        ClientMain.restartGameloop(true, true);
    }

    // Sends a turn to the server
    public void turn(int[] location) {
        if (isClientTurn && !boardWon) {
            if (location.length == 3) {
                writer.send("TURN:" + location[0] + location[1] + location[2]);
                isClientTurn = false;
            }
        }
    }

    // Connects to the server and returns whether the connection was successful or not.
    private boolean connectToServer() {
        int connectionFailCounter = 0;
        while (clientSocket == null) {
            if (connectionFailCounter >= 5) {
                break;
            }
            try {
                clientSocket = new Socket(host.getHostName(), port);
                clientSocket.setSoTimeout(TIMEOUT_MILLIS);
                return true;
            } catch (IOException e) {
                ClientGUI.frame.printToLog("Error connecting to server : " + e);
                connectionFailCounter++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return false;
    }

    // Disconnects from the server
    public void disconnect() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                writer.send("DISCONNECT");
                clientSocket.close();
            } catch (IOException e) {
                ClientGUI.frame.printToLog("Error closing socket");
            }
        }
    }

    // Retrieves and returns the server key from the project website
    public static String getServerKey() {
        String urlStr = "https://alex-garrison.github.io/server-key";

        try {
            // Opens a connection to the URL
            URL url = URI.create(urlStr).toURL();
            URLConnection connection = url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder content = new StringBuilder();

            // Reads the content of the page
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();

            // Returns the content of the page
            return content.toString().strip();
        } catch (IOException e) {
            ClientGUI.frame.printToLog("Error retrieving server key : " + e.getClass().getSimpleName());
        }
        return null;
    }

    // Sends a new game request to the server
    public void sendNewGame() {
        if (writer != null) {
            ClientGUI.frame.printToLog("Sending new game");
            writer.send("NEWGAME");
        }
    }

    // Sets the clientID in the GUI
    private void setClientID(int clientID) {
        ClientGUI.frame.setClientID(clientID);
        ClientGUI.frame.printToLog("Set clientID : " + clientID);
    }

    // Sets the lobbyID in the GUI
    private void setLobbyID(int lobbyID) {
        ClientGUI.frame.setLobbyID(lobbyID);
        ClientGUI.frame.printToLog("Set lobbyID : " + lobbyID);
        ClientGUI.frame.setNewGameEnabled(true);
    }

    // Handles the disconnection of the lobby
    private void lobbyDisconnected() {
        ClientGUI.frame.printToLog("Lobby disconnected");
        ClientGUI.frame.setLobbyID(0);
        ClientGUI.frame.setNewGameEnabled(false);
        ClientGUI.frame.clearPlayerLabel();
        ClientGUI.frame.resetBoardPanels();
        ClientGUI.frame.setNetworkLabel("Lobby disconnected", true);
    }

    // Sets the player in the GUI
    public void setPlayer(String player) {
        this.player = player;
        ClientGUI.frame.setPlayerLabel(this.player, false);
    }

    // Shows that it's the client's turn in the GUI
    public void setClientTurn(boolean isClientTurn) {
        ClientGUI.frame.setPlayerLabel(ClientGUI.frame.getPlayerLabel(), isClientTurn);
        this.isClientTurn = true;
    }

    // Shows that the board has been won in the GUI
    public void boardWon(String winner) {
        if (winner.equals(player)) {
            ClientGUI.frame.setBottomLabel("You won!", false, true);
        } else if (winner.equals(Board.invertPlayer(player))) {
            ClientGUI.frame.setBottomLabel("You lost", true, false);
        } else if (winner.equals("D")) {
            ClientGUI.frame.setBottomLabel("Draw", false, false);
        }
        boardWon = true;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // Deserialises a received board and displays it on the GUI
    private void updateBoard(String serialisedBoard) {
        try {
            NetworkedBoard newNetworkedBoard = new NetworkedBoard();
            try {
                newNetworkedBoard.deserializeBoard(serialisedBoard);
            } catch (NumberFormatException e) {
                ClientGUI.frame.printToLog("Error deserializing : " + e);
            }

            if (newNetworkedBoard.isEmptyBoard()) {
                ClientGUI.frame.resetBoardPanels();
                ClientGUI.frame.clearBottomLabel();
            }

            newNetworkedBoard.isWin();
            ClientGUI.frame.updateBoard(newNetworkedBoard);
            ClientGUI.frame.setBoardColours(newNetworkedBoard);
            ClientGUI.frame.clearBottomLabel();
        } catch (GameException e) {
            ClientGUI.frame.printToLog("Board error : " + e);
        }
    }

    private class ClientReader implements Runnable {
        private BufferedReader reader;
        private boolean keepRunning;

        private int nullDataCounter = 0;

        public ClientReader() {
            keepRunning = true;
        }

        // Begins the reader service
        public void run() {
            while (keepRunning) {
                if (reader == null) {
                    // Create the reader
                    try {
                        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    } catch (IOException e) {
                        ClientGUI.frame.printToLog("Error initialising reader : " + e);
                    }
                } else {
                    try {
                        // Checks for null data and if there is a lot of it, stop the reader
                        if (nullDataCounter >= 5) {
                            ClientGUI.frame.printToLog("Error with server.");
                            keepRunning = false; continue;
                        }

                        // Reads the data from the server
                        String receivedData = reader.readLine();

                        if (receivedData == null) {
                            nullDataCounter++;
                        } else if (!receivedData.isEmpty()){
                            // Parse the data and execute the appropriate function

                            String[] args = receivedData.split(":");
                            if (args[0].equals("CLIENTID")) {
                                try {
                                    setClientID(Integer.parseInt(args[1]));
                                } catch (IndexOutOfBoundsException e) {
                                    ClientGUI.frame.printToLog("Error with CLIENTID command");
                                } catch (Exception e) {
                                    ClientGUI.frame.printToLog("Error with setting clientID");
                                }
                            } else if (args[0].equals("LOBBYID")) {
                                try {
                                    setLobbyID(Integer.parseInt(args[1]));
                                    ClientGUI.frame.setNetworkLabel("Lobby connected", false);
                                } catch (IndexOutOfBoundsException e) {
                                    ClientGUI.frame.printToLog("Error with LOBBYID command");
                                } catch (Exception e) {
                                    ClientGUI.frame.printToLog("Error with setting lobbyID");
                                }
                            } else if (args[0].equals("LOBBYDISCONNECT")) {
                                lobbyDisconnected();
                            } else if (args[0].equals("BOARD")) {
                                try {
                                    updateBoard(args[1]);
                                    setClientTurn(false);
                                } catch (IndexOutOfBoundsException e) {
                                    ClientGUI.frame.printToLog("Error with BOARD command");
                                }
                            } else if (args[0].equals("NEWGAME")) {
                                ClientGUI.frame.clearPlayerLabel();
                                boardWon = false;
                            } else if (args[0].equals("ASSIGNPLAYER")) {
                                try {
                                    setPlayer(args[1]);
                                } catch (IndexOutOfBoundsException e) {
                                    ClientGUI.frame.printToLog("Error with ASSIGNPLAYER command");
                                }
                            } else if (args[0].equals("AWAITTURN")) {
                                setClientTurn(true);
                            } else if (args[0].equals("BOARDWON")) {
                                try {
                                    boardWon(args[1]);
                                } catch (IndexOutOfBoundsException e) {
                                    ClientGUI.frame.printToLog("Error with BOARDWON command");
                                }
                            } else if (args[0].equals("ERROR")) {
                                try {
                                    ClientGUI.frame.setBottomLabel(args[1], true, false);
                                } catch (IndexOutOfBoundsException e) {
                                    ClientGUI.frame.printToLog("Error with ERROR command");
                                } catch (Exception e) {
                                    ClientGUI.frame.printToLog("Error displaying error message : " + e);
                                }
                            } else if (args[0].equals("REQAUTH")) {
                                String serverKey = getServerKey();
                                if (serverKey != null) {
                                    writer.send("AUTH:" + serverKey);
                                } else {
                                    writer.send("AUTH");
                                }
                            } else if (args[0].equals("AUTHSUCCESS")) {
                                ClientGUI.frame.setNetworkLabel("Connected", false);
                            } else if (args[0].equals("AUTHFAIL")) {
                                ClientGUI.frame.setNetworkLabel("Authorisation failed", false);

                                String serverKey = JOptionPane.showInputDialog(ClientGUI.frame, "Please enter the server key : ");

                                if (serverKey != null) {
                                    writer.send("AUTH:" + serverKey);
                                }
                            } else if (args[0].equals("DISCONNECT")) {
                                keepRunning = false;
                            } else {
                                ClientGUI.frame.printToLog("Server sent : " + receivedData);
                            }
                        }
                    }  catch (SocketTimeoutException e) {} catch (IOException e) {
                        ClientGUI.frame.printToLog("Error reading data : " + e);
                        keepRunning = false;
                    }
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    ClientGUI.frame.printToLog("Error closing reader" + e);
                }
            }
        }

        public void stopRunning() {
            keepRunning = false;
        }
    }

    private class ClientWriter {
        private BufferedWriter writer;

        public ClientWriter() {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            } catch (IOException e) {
                ClientGUI.frame.printToLog("Error initialising writer : " + e);
            }
        }

        // Sends the data passed to it to the server
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
                        return;
                    } else {
                        ClientGUI.frame.printToLog("Error sending message : " + e);
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
                } catch (IOException e) {
                    ClientGUI.frame.printToLog("Error closing writer" + e);
                }
            }
        }
    }
}
