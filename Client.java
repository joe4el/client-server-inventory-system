import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * Client.java
 * Distributed Inventory Management System - Client Side
 * Student ID : 2199
 * Default Port: 5199  (formula: 5000 + 2199 % 1000 = 5199)
 */
public class Client extends JFrame {

    // ── Personalization constants ──────────────────────────────────────
    private static final int DEFAULT_PORT = 5199;  // 5000 + (2199 % 1000)
    // ──────────────────────────────────────────────────────────────────

    // ── GUI components ─────────────────────────────────────────────────
    private JTextField      ipField;
    private JTextField      portField;
    private JButton         connectButton;
    private JButton         disconnectButton;
    private JButton         verifyButton;
    private JLabel          statusLabel;
    private JPanel          fileButtonsPanel;
    private JTable          productTable;
    private DefaultTableModel tableModel;
    private JTextArea       overviewArea;

    // ── Network state ──────────────────────────────────────────────────
    private Socket          socket;
    private BufferedReader  in;
    private PrintWriter     out;
    private volatile boolean connected = false;

    
    public Client() {
        setTitle("Inventory Client  |  Student ID: 2199");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 620);
        setLocationRelativeTo(null);
        setResizable(true);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(245, 245, 250));

        // ── TOP: Connection panel ──────────────────────────────────────
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        connPanel.setBackground(new Color(230, 230, 240));
        connPanel.setBorder(BorderFactory.createTitledBorder("Connection"));

        connPanel.add(new JLabel("Server IP:"));
        ipField = new JTextField("127.0.0.1", 11);
        ipField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        connPanel.add(ipField);

        connPanel.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        portField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        connPanel.add(portField);

        connectButton = new JButton("⚡ Connect");
        connectButton.setBackground(new Color(60, 180, 75));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectButton.addActionListener(e -> connectToServer());
        connPanel.add(connectButton);

        disconnectButton = new JButton("✖ Disconnect");
        disconnectButton.setBackground(new Color(220, 60, 60));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnectFromServer());
        connPanel.add(disconnectButton);

        verifyButton = new JButton("🔑 Verify");
        verifyButton.setBackground(new Color(70, 130, 180));
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setFocusPainted(false);
        verifyButton.setEnabled(false);
        verifyButton.addActionListener(e -> sendVerify());
        connPanel.add(verifyButton);

        statusLabel = new JLabel("● DISCONNECTED");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(Color.RED);
        connPanel.add(statusLabel);

        root.add(connPanel, BorderLayout.NORTH);

        // ── CENTER: split pane (left = file buttons, right = data) ────
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(6);

        // Left side – file buttons + overview button
        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.setBackground(new Color(240, 240, 248));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Files"));

        fileButtonsPanel = new JPanel();
        fileButtonsPanel.setLayout(new BoxLayout(fileButtonsPanel, BoxLayout.Y_AXIS));
        fileButtonsPanel.setBackground(new Color(240, 240, 248));

        JScrollPane fileScroll = new JScrollPane(fileButtonsPanel);
        fileScroll.setBorder(null);
        leftPanel.add(fileScroll, BorderLayout.CENTER);

        JButton overviewButton = new JButton("📊 Get Overview");
        overviewButton.setBackground(new Color(255, 165, 0));
        overviewButton.setForeground(Color.WHITE);
        overviewButton.setFocusPainted(false);
        overviewButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        overviewButton.addActionListener(e -> requestOverview());
        leftPanel.add(overviewButton, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Right side – tabbed: product table + overview text
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Product table tab
        String[] columns = {"Product ID", "Product Name", "Price ($)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(tableModel);
        productTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        productTable.setRowHeight(22);
        productTable.setGridColor(new Color(200, 200, 220));
        productTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        productTable.getTableHeader().setBackground(new Color(70, 130, 180));
        productTable.getTableHeader().setForeground(Color.WHITE);

        // Alternating row colors
        productTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0
                        ? Color.WHITE : new Color(235, 240, 255));
                return this;
            }
        });

        JScrollPane tableScroll = new JScrollPane(productTable);
        tabs.addTab("📋 Product List", tableScroll);

        // Overview text tab
        overviewArea = new JTextArea();
        overviewArea.setEditable(false);
        overviewArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        overviewArea.setMargin(new Insets(12, 14, 12, 14));
        overviewArea.setBackground(new Color(25, 25, 35));
        overviewArea.setForeground(new Color(0, 220, 180));
        tabs.addTab("📈 Overview", new JScrollPane(overviewArea));

        splitPane.setRightComponent(tabs);
        root.add(splitPane, BorderLayout.CENTER);

        add(root);
    }

    
    private void connectToServer() {
        String ip   = ipField.getText().trim();
        int    port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Run connection in background thread to avoid freezing GUI
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                connected = true;

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("● CONNECTED");
                    statusLabel.setForeground(new Color(0, 150, 0));
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    verifyButton.setEnabled(true);
                    ipField.setEditable(false);
                    portField.setEditable(false);
                });

                // Read file list from server
                receiveFileList();

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Could not connect to " + ip + ":" + port + "\n" + ex.getMessage(),
                        "Connection Failed", JOptionPane.ERROR_MESSAGE));
            }
        }, "ConnectThread").start();
    }

    private void disconnectFromServer() {
        connected = false;
        try {
            if (out != null)    out.println("DISCONNECT");
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("● DISCONNECTED");
            statusLabel.setForeground(Color.RED);
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            verifyButton.setEnabled(false);
            ipField.setEditable(true);
            portField.setEditable(true);
            fileButtonsPanel.removeAll();
            fileButtonsPanel.revalidate();
            fileButtonsPanel.repaint();
            tableModel.setRowCount(0);
            overviewArea.setText("");
        });
    }

    
    private void receiveFileList() {
        try {
            String header = in.readLine(); // "FILELIST"
            if (!"FILELIST".equals(header)) return;

            java.util.List<String> files = new java.util.ArrayList<>();
            String line;
            while (!(line = in.readLine()).equals("END")) {
                files.add(line.trim());
            }

            SwingUtilities.invokeLater(() -> {
                fileButtonsPanel.removeAll();
                for (String filename : files) {
                    JButton btn = new JButton("📁 " + filename);
                    btn.setAlignmentX(Component.CENTER_ALIGNMENT);
                    btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                    btn.setBackground(new Color(100, 149, 237));
                    btn.setForeground(Color.WHITE);
                    btn.setFocusPainted(false);
                    btn.addActionListener(e -> requestFile(filename));
                    fileButtonsPanel.add(btn);
                    fileButtonsPanel.add(Box.createVerticalStrut(5));
                }
                fileButtonsPanel.revalidate();
                fileButtonsPanel.repaint();
            });

        } catch (IOException ex) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this,
                    "Error receiving file list: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE));
        }
    }


    private void requestFile(String filename) {
        if (!connected) return;

        new Thread(() -> {
            try {
                out.println("GET:" + filename);

                String header = in.readLine();
                if (header == null) return;

                if (header.startsWith("ERROR:")) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, header.substring(6),
                            "Server Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }

                if (!"FILEDATA".equals(header)) return;

                java.util.List<String[]> rows = new java.util.ArrayList<>();
                String line;
                while (!(line = in.readLine()).equals("END")) {
                    String[] parts = line.split(",", 3);
                    if (parts.length == 3) {
                        rows.add(new String[]{
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim()
                        });
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (String[] row : rows) tableModel.addRow(row);
                    // Switch to product list tab
                    ((JTabbedPane)((JScrollPane)productTable.getParent().getParent())
                        .getParent()).setSelectedIndex(0);
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Error fetching file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "FileRequestThread").start();
    }

   
    private void requestOverview() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Not connected to server.",
                "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                out.println("OVERVIEW");

                String header = in.readLine();
                if (header == null) return;

                if (header.startsWith("ERROR:")) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, header.substring(6),
                            "Server Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }

                if (!"OVERVIEW".equals(header)) return;

                StringBuilder sb = new StringBuilder();
                String line;
                while (!(line = in.readLine()).equals("END")) {
                    sb.append(line).append("\n");
                }

                SwingUtilities.invokeLater(() -> {
                    overviewArea.setText(sb.toString());
                    // Switch to overview tab
                    ((JTabbedPane)((JScrollPane)overviewArea.getParent().getParent())
                        .getParent()).setSelectedIndex(1);
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Error fetching overview: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "OverviewThread").start();
    }

    
    private void sendVerify() {
        if (!connected) return;

        new Thread(() -> {
            try {
                out.println("VERIFY");
                String response = in.readLine();

                if (response != null && response.startsWith("VERIFY_RESULT:")) {
                    String hash = response.substring("VERIFY_RESULT:".length());
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                            "✅ Verification Successful!\n\n" +
                            "Student ID  : 2199\n" +
                            "Digit Sum   : 21  (2+1+9+9)\n" +
                            "Active Port : " + portField.getText() + "\n" +
                            "Hash Result : " + hash + "\n\n" +
                            "Formula: (21 × port) % 1000 = " + hash,
                            "Verification Result",
                            JOptionPane.INFORMATION_MESSAGE));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Verify error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "VerifyThread").start();
    }

    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
