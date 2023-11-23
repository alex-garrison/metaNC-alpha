public class ServerMain {
    public static ServerMain serverMain;
    public static Server server;
    private static Thread serverThread;

    private static boolean isHeadless;
    private static boolean isLogging;
    private static String serverKey;

    private ServerMain() {}

    public static void main(String[] args) {
        serverMain = new ServerMain();
        isHeadless = false;

        if (args.length > 0) {
            for (int arg = 0; arg < args.length; arg++) {
                if (args[arg].equals("-H") || args[arg].equals("--headless")) {
                    isHeadless = true;
                } else if (args[arg].equals("-L") || args[arg].equals("--logging")) {
                    isLogging = true;
                } else if (args[arg].equals("-K") || args[arg].equals("--server-key")) {
                    try {
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
            try {
                ServerGUI.startGUI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            serverMain.startServer();

            try {
                serverThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
