import java.io.*;
import java.net.*;

public class Client implements Runnable {
    private final InetAddress host;
    private final int port;

    private final int TIMEOUT_MILLIS = 500;

    private int clientID;
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

        clientID = -1;
        isConnected = false;
        boardWon = false;
    }

    public void run() {
        ClientGUI.frame.resetBoardPanels();
        ClientGUI.frame.clearBottomLabel();
        ClientGUI.frame.clearNetworkLabel();
        ClientGUI.frame.clearPlayerLabel();

        if (connectToServer()) {
            ClientGUI.frame.setNetworkLabel("Connected", false);
            System.out.println("Connected to server : " + clientSocket);
            isConnected = true;
            ClientGUI.frame.setNetworked(true);
            ClientGUI.frame.setNetworkButtonFunction(false);
            ClientGUI.frame.setNetworkMode(true);

            ClientGUI.frame.setNewGameEnabled(false);

            reader = new ClientReader();
            Thread readerThread = new Thread(reader);
            readerThread.start();

            writer = new ClientWriter();

            String serverKey = getServerKey();
            if (serverKey != null) {
                writer.send("AUTH:" + serverKey);
            } else {
                writer.send("AUTH");
            }

            try {
                readerThread.join();
            } catch (InterruptedException e) {
                System.out.println("Error waiting for readerThread to stop");
            }
            writer.close();

            isConnected = false;
            ClientGUI.frame.setNetworkLabel("Disconnected" , true);
            ClientGUI.frame.clearBottomLabel();
            ClientGUI.frame.clearPlayerLabel();
            ClientGUI.frame.setNetworkMode(false);
            ClientGUI.frame.setNetworked(false);
            ClientGUI.frame.setNetworkButtonFunction(true);
            ClientGUI.frame.setClientID(0); ClientGUI.frame.setLobbyID(0);
            clientID = 0;

        } else {
            ClientGUI.frame.setNetworkLabel("Error connecting" , true);
        }

        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket");
            }
        }

        System.out.println("Client stopped");

        ClientMain.restartGameloop(true);
    }

    public void turn(int[] location) {
        if (isClientTurn && !boardWon) {
            if (location.length == 3) {
                writer.send("TURN:" + location[0] + location[1] + location[2]);
                isClientTurn = false;
            }
        }
    }

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
                System.out.println("Error connecting to server : " + e);
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

    public void disconnect() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                writer.send("DISCONNECT");
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket");
            }
        }
    }

    public static String getServerKey() {
        String urlStr = "https://alex-garrison.github.io/server-key";

        try {
            URL url = URI.create(urlStr).toURL();
            URLConnection connection = url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder content = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();

            return content.toString().strip();
        } catch (IOException e) {
            System.out.println("Error retrieving server IP");
        }
        return null;
    }

    public void sendNewGame() {
        if (writer != null) {
            System.out.println("Sending new game");
            writer.send("NEWGAME");
        }
    }

    private void setClientID(int clientID) {
        this.clientID = clientID;
        ClientGUI.frame.setClientID(clientID);
        System.out.println("Set clientID : " + clientID);
    }

    private void setLobbyID(int lobbyID) {
        ClientGUI.frame.setLobbyID(lobbyID);
        System.out.println("Set lobbyID : " + lobbyID);
        ClientGUI.frame.setNewGameEnabled(true);
    }

    public void setPlayer(String player) {
        this.player = player;
        ClientGUI.frame.setPlayerLabel(this.player, false);
    }

    public void setClientTurn(boolean isClientTurn) {
        ClientGUI.frame.setPlayerLabel(ClientGUI.frame.getPlayerLabel(), isClientTurn);
        this.isClientTurn = true;
    }

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

    private void updateBoard(String serialisedBoard) {
        try {
            NetworkedBoard newNetworkedBoard = new NetworkedBoard();
            try {
                newNetworkedBoard.deserializeBoard(serialisedBoard);
            } catch (NumberFormatException e) {
                System.out.println("Error deserializing : " + e);
            }

            if (newNetworkedBoard.isEmptyBoard()) {
                ClientGUI.frame.resetBoardPanels();
                ClientGUI.frame.clearBottomLabel();
            }

            newNetworkedBoard.isWin();
            ClientGUI.frame.updateBoard(newNetworkedBoard);
            ClientGUI.frame.setBoardColours(newNetworkedBoard, player);
            ClientGUI.frame.clearBottomLabel();
        } catch (GameException e) {
            System.out.println("Board error : " + e);
        }
    }

    private class ClientReader implements Runnable {
        private BufferedReader reader;
        private boolean keepRunning;

        private int nullDataCounter = 0;

        public ClientReader() {
            keepRunning = true;
        }

        public void run() {
            while (keepRunning) {
                if (reader == null) {
                    try {
                        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    } catch (IOException e) {
                        System.out.println("Error initialising reader : " + e);
                    }
                } else {
                    try {
                        if (nullDataCounter >= 5) {
                            System.out.println("Error with server.");
                            keepRunning = false; continue;
                        }

                        String receivedData = reader.readLine();

                        if (receivedData == null) {
                            nullDataCounter++;
                        } else if (!receivedData.isEmpty()){
                            String[] args = receivedData.split(":");
                            if (args[0].equals("CLIENTID")) {
                                try {
                                    setClientID(Integer.parseInt(args[1]));
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Error with CLIENTID command");
                                } catch (Exception e) {
                                    System.out.println("Error with setting clientID");
                                }
                            } else if (args[0].equals("LOBBYID")) {
                                try {
                                    setLobbyID(Integer.parseInt(args[1]));
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Error with LOBBYID command");
                                } catch (Exception e) {
                                    System.out.println("Error with setting clientID");
                                }
                            } else if (args[0].equals("BOARD")) {
                                try {
                                    updateBoard(args[1]);
                                    setClientTurn(false);
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Error with BOARD command");
                                }
                            } else if (args[0].equals("NEWGAME")) {
                                ClientGUI.frame.clearPlayerLabel();
                                boardWon = false;
                            } else if (args[0].equals("ASSIGNPLAYER")) {
                                try {
                                    setPlayer(args[1]);
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Error with ASSIGNPLAYER command");
                                }
                            } else if (args[0].equals("AWAITTURN")) {
                                setClientTurn(true);
                            } else if (args[0].equals("BOARDWON")) {
                                try {
                                    boardWon(args[1]);
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Error with BOARDWON command");
                                }
                            } else if (args[0].equals("ERROR")) {
                                try {
                                    ClientGUI.frame.setBottomLabel(args[1], true, false);
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Error with ERROR command");
                                } catch (Exception e) {
                                    System.out.println("Error displaying error message : " + e);
                                }
                            } else if (args[0].equals("DISCONNECT")) {
                                keepRunning = false;
                            } else {
                                System.out.println("Server sent : " + receivedData);
                            }
                        }
                    }  catch (SocketTimeoutException e) {} catch (IOException e) {
                        System.out.println("Error reading data : " + e);
                        keepRunning = false;
                    }
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.out.println("Error closing reader" + e);
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
                System.out.println("Error initialising writer : " + e);
            }
        }

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
                    if (errorCount >= 5) {
                        return;
                    } else {
                        System.out.println("Error sending message : " + e);
                        errorCount++;
                    }
                }
            }
        }

        public void close() {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println("Error closing writer" + e);
                }
            }
        }
    }
}
