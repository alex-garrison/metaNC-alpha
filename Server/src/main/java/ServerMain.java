public class ServerMain {
    public static ServerMain serverMain;
    public static Server server;
    private static Thread serverThread;

    private static boolean isHeadless;
    private static boolean isLogging;

    private ServerMain() {}

    public static void main(String[] args) {
        serverMain = new ServerMain();
        isHeadless = false;

        if (args.length > 0) {
            for (String arg: args) {
                if (arg.equals("-H") || arg.equals("--headless")) {
                    isHeadless = true;
                } else if (arg.equals("-L") || arg.equals("--logging")) {
                    isLogging = true;
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

            server = new Server();
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
