public class ServerMain {
    public static ServerMain serverMain;
    public static Server server;
    private static Thread serverThread;

    private static boolean isHeadless;
    private static boolean isLogging;
    private static String serverKey;

    private ServerMain() {}

    // Processes and handles the command line flags and begins the corresponding threads
    public static void main(String[] args) {
        serverMain = new ServerMain();
        isHeadless = false;

        if (args.length > 0) {
            for (int arg = 0; arg < args.length; arg++) {
                // Checks for the headless flag
                if (args[arg].equals("-H") || args[arg].equals("--headless")) {
                    isHeadless = true;
                }
                // Checks for the logging flag
                else if (args[arg].equals("-L") || args[arg].equals("--logging")) {
                    isLogging = true;
                }
                // Checks for the server key flag
                else if (args[arg].equals("-K") || args[arg].equals("--server-key")) {
                    try {
                        // Checks if the server key is valid
                        if (args[arg+1] != null && !(args[arg+1].equals("-H") || args[arg+1].equals("--headless")) || args[arg+1].equals("-L") || args[arg+1].equals("--logging")) {
                            serverKey = args[arg+1];
                        } else {
                            System.out.println("Error with server key");
                            return;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("Error with server key");
                        return;
                    }
                }
            }
        }

        if (!isHeadless) {
            // Starts the server GUI if it's not in headless mode
            try {
                ServerGUI.startGUI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // Starts the server regularly if it's in headless mode
            serverMain.startServer();

            // Waits for the server to stop running
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Starts the main server thread and prepares the GUI for the server to run
    public void startServer() {
        if (serverThread == null || !serverThread.isAlive()) {
            if (!isHeadless) {
                ServerGUI.clear();
                ServerGUI.clearNetworkLabel();
            }

            server = new Server(serverKey);
            serverThread = new Thread(server);
            serverThread.start();
        }
    }

    // Stops the server
    public void stopServer() {
        if (server != null && serverThread != null) {
            Server.stopRunning();
            try {
                do {
                    serverThread.join(1000);
                    if (serverThread.isAlive()) {
                        serverThread.interrupt();
                    }
                } while (serverThread.isAlive());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void serverStopped() {
        ServerGUI.frame.setServerButtonFunction(true);
    }

    public static boolean isHeadless() {
        return isHeadless;
    }

    public static boolean isLogging() {
        return isLogging;
    }
}
