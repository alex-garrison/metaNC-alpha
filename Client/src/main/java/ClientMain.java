import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClientMain {
    public static Client client;
    private static Thread clientThread;

    private static GameLoop gameLoop;
    private static Future gameLoopFuture;
    private static ExecutorService executorService;

    private static InetAddress host;
    private static int port;
    private static boolean hostSet;

    // Starts the client GUI and creates a single threaded executor service to run the game loop
    public static void main(String[] args) {
        try {
            ClientGUI.startGUI();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        executorService = Executors.newSingleThreadExecutor();

        startGameloop(true, true);
    }

    public static boolean isHostSet() {
        return hostSet;
    }

    public static void setHostAndPort(InetAddress newHost, int newPort) {
        host = newHost;
        port = newPort;
        hostSet = true;
    }

    public static InetAddress getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    // Starts the networked client on a separate thread
    public static void startClient() {
        if (hostSet) {
            stopGameloop();

            client = new Client(host, port);
            clientThread = new Thread(client);
            clientThread.start();
        } else {
            ClientGUI.frame.setNetworkLabel("Host not set", true);
        }
    }

    // Starts the game loop with the given parameters
    private static void startGameloop(boolean waitForNewGame, boolean waitForModeSelect) {
        gameLoop = new GameLoop(waitForNewGame, waitForModeSelect);
        gameLoopFuture = executorService.submit(gameLoop);
    }

    // Stops the game loop
    private static void stopGameloop() {
        gameLoop.stopRunning();
        gameLoopFuture.cancel(true);
    }

    // Restarts the game loop with the given parameters
    public static void restartGameloop(boolean waitForNewGame, boolean waitForModeSelect) {
        stopGameloop();
        startGameloop(waitForNewGame, waitForModeSelect);
    }

    static class GameLoop implements Runnable {
        private String mode;
        public boolean gameLoopRunning;
        private boolean waitForNewGame;
        private boolean waitForModeSelect;
        private final int AI_MOVE_DELAY = 75;

        public GameLoop(boolean waitForNewGame, boolean waitForModeSelect) {
            this.waitForNewGame = waitForNewGame;
            this.waitForModeSelect = waitForModeSelect;
            gameLoopRunning = true;
        }

        // Waits for the user to select a mode and start a new game, then begins the main game loop
        public void run() {
            if (waitForNewGame) {
                try {
                    ClientGUI.frame.waitForNewGame();
                } catch (InterruptedException e) {
                    ClientGUI.frame.printToLog("Error waiting : " + e);
                }
            }

            if (waitForModeSelect) {
                try {
                    ClientGUI.frame.waitForModeSelect();
                } catch (InterruptedException e) {
                    ClientGUI.frame.printToLog("Error waiting : " + e);
                }
            }

            mode = ClientGUI.frame.getMode();

            gameLoop();
        }

        // Begins the main local game loop
        private void gameLoop() {
            // Clears the GUI and creates a new board
            ClientGUI.frame.resetBoardPanels();
            ClientGUI.frame.clearBottomLabel();
            ClientGUI.frame.clearNetworkLabel();

            Board board = new Board();
            board.emptyBoard();
            board.setStarter("X");

            // Sets the mode to either PvP or PvAI
            boolean isPVP = false;
            boolean isPVAI = false;

            if (mode.equals("PvP")) {
                isPVP = true;
            } else if (mode.equals("PvAI")) {
                isPVAI = true;
            }

            if (!(isPVP || isPVAI)) {
                ClientGUI.frame.printToLog("Mode not set");
                restartGameloop(true, true); return;
            }

            boolean errorOccurred = false;
            int[] moveLocation = new int[3];
            AiAgent ai = new AiAgent(board);

            while (gameLoopRunning) {
                // Checks if the game has been won or drawn
                if (board.isWin()) {
                    if (board.winner.equals("D")) {
                        ClientGUI.frame.setBottomLabel("Draw", false, false);
                    } else {
                        ClientGUI.frame.setBottomLabel("Player " + board.winner + " wins", false, true);
                    }
                    ClientGUI.frame.updateBoard(board);
                    ClientGUI.frame.setBoardColours(board);

                    break;
                }

                // Updates the GUI
                try {
                    ClientGUI.frame.updateBoard(board);
                    ClientGUI.frame.setBoardColours(board);
                } catch (RuntimeException e) {
                    gameLoopRunning = false; continue;
                }

                if (isPVP) {
                    // Waits for the user to make a move
                    try {
                        moveLocation = ClientGUI.frame.waitForMove();
                    } catch (InterruptedException e) {
                        gameLoopRunning = false; continue;
                    }
                } else if (isPVAI) {
                    // Waits for the user or AI to make a move
                    if (board.whoseTurn().equals("X")) {
                        try {
                            moveLocation = ClientGUI.frame.waitForMove();
                        } catch (InterruptedException e) {
                            gameLoopRunning = false; continue;
                        }
                    } else {
                        try {
                            moveLocation = ai.getMove(false);
                            Thread.sleep(AI_MOVE_DELAY);
                        } catch (InterruptedException e) {
                            break;
                        } catch (GameException e) {
                            ClientGUI.frame.printToLog(e.getMessage());
                        }
                    }
                }

                // Makes the move
                try {
                    board.turn(board.whoseTurn(), moveLocation);
                } catch (GameException e) {
                    ClientGUI.frame.setBottomLabel(e.getMessage(), true, false);
                    errorOccurred = true;
                } catch (NullPointerException e) {
                    continue;
                }

                if (!errorOccurred) {
                    ClientGUI.frame.clearBottomLabel();
                } else {
                    errorOccurred = false;
                }
            }
        }

        public void stopRunning() {
            synchronized (this) {
                gameLoopRunning = false;
            }
        }
    }
}
