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

    private static void startGameloop(boolean waitForNewGame, boolean waitForModeSelect) {
        gameLoop = new GameLoop(waitForNewGame, waitForModeSelect);
        gameLoopFuture = executorService.submit(gameLoop);
    }

    private static void stopGameloop() {
        gameLoop.stopRunning();
        gameLoopFuture.cancel(true);
    }

    public static void restartGameloop(boolean waitForNewGame, boolean waitForModeSelect) {
        stopGameloop();
        startGameloop(waitForNewGame, waitForModeSelect);
    }

    static class GameLoop implements Runnable {
        private String mode;
        public boolean gameLoopRunning;
        private boolean waitForNewGame;
        private boolean waitForModeSelect;

        public GameLoop(boolean waitForNewGame, boolean waitForModeSelect) {
            this.waitForNewGame = waitForNewGame;
            this.waitForModeSelect = waitForModeSelect;
            gameLoopRunning = true;
        }

        public void run() {
            if (waitForNewGame) {
                try {
                    ClientGUI.frame.waitForNewGame();
                } catch (InterruptedException e) {
                    System.out.println("Error waiting : " + e);
                }
            }

            if (waitForModeSelect) {
                try {
                    ClientGUI.frame.waitForModeSelect();
                } catch (InterruptedException e) {
                    System.out.println("Error waiting : " + e);
                }
            }

            mode = ClientGUI.frame.getMode();

            gameLoop();
        }

        private void gameLoop() {
            ClientGUI.frame.resetBoardPanels();
            ClientGUI.frame.clearBottomLabel();
            ClientGUI.frame.clearNetworkLabel();

            Board board = new Board();
            board.emptyBoard();
            board.setStarter("X");

            long AI_MOVE_DELAY = 75;

            boolean isPVP = false;
            boolean isPVAI = false;

            if (mode.equals("PvP")) {
                isPVP = true;
            } else if (mode.equals("PvAI")) {
                isPVAI = true;
            }

            if (!(isPVP || isPVAI)) {
                System.out.println("Mode not set");
                restartGameloop(true, true); return;
            }

            boolean errorOccurred = false;
            int[] moveLocation = new int[3];
            AiAgent ai = new AiAgent(board);

            while (gameLoopRunning) {
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

                try {
                    ClientGUI.frame.updateBoard(board);
                    ClientGUI.frame.setBoardColours(board);
                } catch (RuntimeException e) {
                    gameLoopRunning = false; continue;
                }

                if (isPVP) {
                    try {
                        moveLocation = ClientGUI.frame.waitForMove();
                    } catch (InterruptedException e) {
                        gameLoopRunning = false; continue;
                    }
                } else if (isPVAI) {
                    if (board.whoseTurn().equals("X")) {
                        try {
                            moveLocation = ClientGUI.frame.waitForMove();
                        } catch (InterruptedException e) {
                            gameLoopRunning = false; continue;
                        }
                    } else {
                        try {
                            moveLocation = ai.getMove();
                            Thread.sleep(AI_MOVE_DELAY);
                        } catch (InterruptedException e) {
                            break;
                        } catch (GameException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }

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
