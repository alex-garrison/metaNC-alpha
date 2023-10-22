import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.TreeMap;

public class ServerGUI extends JFrame {
    public static ServerGUI frame;

    private static final Color TEXT = new Color(224, 225, 221);
    private final Color BACKGROUND = new Color(27, 38, 59);
    private static final Color OUTPUT_BACKGROUND = Color.BLACK;

    private static JTabbedPane tabs;

    private static JPanel contentPane;
    private static JPanel serverOutputPanel;
    private static JTextArea serverOutput;

    private static TreeMap<Integer, JTextArea> lobbyOutputs;
    private static TreeMap<Integer, JPanel> lobbyOutputPanels;

    private static JPanel optionPanel;
    private static JButton startServerButton;
    private static JButton stopServerButton;
    private static JButton serverButton;
    private static JLabel networkLabel;

    private ServerGUI() {
        lobbyOutputs = new TreeMap<>();
        lobbyOutputPanels = new TreeMap<>();
        initGUI();
    }

    private void initGUI() {
        FlatMacDarkLaf.setup();

        setTitle("uNC - Server");
        setMinimumSize(new Dimension(500, 550));
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        optionPanel = new JPanel();
        optionPanel.setBorder(new EmptyBorder(10,20,0,25));
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));

        startServerButton = new JButton("Start");
        startServerButton.addActionListener(new StartServerListener());

        stopServerButton = new JButton("Stop");
        stopServerButton.addActionListener(new StopServerListener());

        serverButton = new JButton();
        serverButton.setFont(new Font("Arial", Font.PLAIN, 20));
        serverButton.setForeground(TEXT);
        setServerButtonFunction(true);

        networkLabel = new JLabel();
        networkLabel.setFont(new Font("monospaced", Font.PLAIN, 20));
        networkLabel.setForeground(TEXT);

        optionPanel.add(serverButton);
        optionPanel.add(Box.createHorizontalGlue());
        optionPanel.add(networkLabel);

        serverOutputPanel = new JPanel();
        serverOutputPanel.setBorder(new EmptyBorder(0,5,0,5));
        serverOutputPanel.setLayout(new GridLayout());

        serverOutput = new JTextArea();
        serverOutput.setFont(new Font("monospaced", Font.PLAIN, 15));
        serverOutput.setForeground(TEXT);
        serverOutput.setBackground(OUTPUT_BACKGROUND);
        serverOutput.setEditable(false);
        serverOutput.setBorder(new EmptyBorder(5,5,5,5));
        DefaultCaret caret = (DefaultCaret) serverOutput.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(serverOutput);
        scrollPane.setBorder(new LineBorder(TEXT, 5));
        scrollPane.setBackground(OUTPUT_BACKGROUND);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        serverOutputPanel.add(scrollPane);

        tabs = new JTabbedPane();
        tabs.setForeground(TEXT);
        tabs.setBorder(new EmptyBorder(10,10,10,10));
        tabs.add("Server", serverOutputPanel);

        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        contentPane.add(optionPanel);
        contentPane.add(Box.createVerticalGlue());
        contentPane.add(tabs);

        add(contentPane);
    }

    private class StartServerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ServerMain.serverMain.startServer();
            setServerButtonFunction(false);
        }
    }

    private class StopServerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ServerMain.serverMain.stopServer();
            setServerButtonFunction(true);
        }
    }

    public void setServerButtonFunction(Boolean isStart) {
        JButton[] serverButtons = new JButton[]{startServerButton, stopServerButton};
        int buttonSelector = isStart ? 0 : 1;

        for (ActionListener listener: serverButton.getActionListeners()) {
            serverButton.removeActionListener(listener);
        }

        serverButton.setText(serverButtons[buttonSelector].getText());
        serverButton.addActionListener(serverButtons[buttonSelector].getActionListeners()[0]);
        serverButton.revalidate();
        serverButton.repaint();
    }

    public void addLobby(Lobby lobby) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JPanel lobbyOutputPanel = new JPanel();
                    lobbyOutputPanel.setBorder(new EmptyBorder(0,5,0,5));
                    lobbyOutputPanel.setBackground(BACKGROUND);

                    JTextArea lobbyOutput = new JTextArea();
                    lobbyOutput.setFont(new Font("monospaced", Font.PLAIN, 15));
                    lobbyOutput.setBackground(OUTPUT_BACKGROUND);
                    lobbyOutput.setForeground(TEXT);
                    lobbyOutput.setEditable(false);
                    lobbyOutput.setBorder(new EmptyBorder(5,5,5,5));
                    DefaultCaret caret = (DefaultCaret) lobbyOutput.getCaret();
                    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                    JScrollPane scrollPane = new JScrollPane(lobbyOutput);
                    scrollPane.setBorder(new LineBorder(TEXT, 5));
                    scrollPane.setBackground(OUTPUT_BACKGROUND);
                    scrollPane.setPreferredSize(new Dimension(450, 400));
                    lobbyOutputPanel.add(scrollPane);

                    lobbyOutputs.put(lobby.lobbyID, lobbyOutput);
                    lobbyOutputPanels.put(lobby.lobbyID, lobbyOutputPanel);

                    int index = new ArrayList<>(lobbyOutputPanels.keySet()).indexOf(lobby.lobbyID);

                    tabs.insertTab("Lobby " + lobby.lobbyID, null, lobbyOutputPanel, null, index+1);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void removeLobby(Lobby lobby) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    lobbyOutputs.remove(lobby.lobbyID);
                    JPanel lobbyOutputPanel = lobbyOutputPanels.remove(lobby.lobbyID);
                    tabs.remove(lobbyOutputPanel);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void printToLobby(String text, Lobby lobby) {
        if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> {
                    JTextArea lobbyOutput = lobbyOutputs.get(lobby.lobbyID);
                    if (lobbyOutput != null) {
                        lobbyOutput.append(text + "\n");
                    }
                });
        }
    }


    public void printToServer(String text) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> serverOutput.append(text + "\n"));
            } catch (InterruptedException | InvocationTargetException e) {}
        }
    }

    public static void clear() {
        serverOutput.setText("");
    }

    public static void setNetworkLabel(InetAddress host, int port) {
        networkLabel.setText(host.getHostAddress() + ":" + port);
    }

    public static void clearNetworkLabel() {
        networkLabel.setText("");
    }

    public static void startGUI() throws InterruptedException, InvocationTargetException {
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.awt.application.name", "Ultimate Noughts and Crosses");

        SwingUtilities.invokeAndWait(() -> {
            frame = new ServerGUI();
            frame.setVisible(true);
        });
    }
}
