import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class LogDialog extends JDialog {
    private JPanel buttonPanel;
    private JButton closeButton;
    private static JPanel outputPanel;
    private static JTextArea output;

    private static final Color TEXT = new Color(224, 225, 221);
    private static final Color OUTPUT_BACKGROUND = Color.BLACK;

    public LogDialog(ClientGUI clientGUI) {
        super(clientGUI, "Log", true);
        setMinimumSize(new Dimension(550, 350));
        setResizable(false);

        setLayout(new BorderLayout());

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(new EmptyBorder(10,0,10,0));

        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(closeButton);
        buttonPanel.add(Box.createHorizontalGlue());

        outputPanel = new JPanel();
        outputPanel.setBorder(new EmptyBorder(5,5,0,5));
        outputPanel.setLayout(new GridLayout());

        output = new JTextArea();
        output.setFont(new Font("monospaced", Font.PLAIN, 15));
        output.setForeground(TEXT);
        output.setBackground(OUTPUT_BACKGROUND);
        output.setEditable(false);
        output.setBorder(new EmptyBorder(5,5,5,5));
        DefaultCaret caret = (DefaultCaret) output.getCaret();
        caret.setVisible(false);
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(output);
        scrollPane.setBorder(new LineBorder(TEXT, 5));
        scrollPane.setBackground(OUTPUT_BACKGROUND);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        outputPanel.add(scrollPane);

        add(outputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Displays the given text on the output panel of the log dialog
    public void printToLog(String text) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> output.append(text + "\n"));
            } catch (InterruptedException | InvocationTargetException e) {}
        }
    }
}
