import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Server.java
 * Distributed Inventory Management System - Server Side
 * Student ID : 2199
 * Default Port: 5199  (formula: 5000 + 2199 % 1000 = 5199)
 * Verify Hash : 179   (formula: digitSum(2199) * port % 1000 = 21 * 5199 % 1000 = 179)
 */
public class Server extends JFrame {

    // ── Personalization constants ──────────────────────────────────────
    private static final int    STUDENT_ID   = 2199;
    private static final String STUDENT_NAME = "Elvis Mtandika";
    private static final int    DEFAULT_PORT = 5199;               // 5000 + (2199 % 1000)
    private static final int    DIGIT_SUM    = 21;                 // 2+1+9+9
    // ──────────────────────────────────────────────────────────────────

    private static final String[] INVENTORY_FILES = {
        "Electronics.txt", "Clothing.txt", "Groceries.txt", "Books.txt"
    };
    private static final String MERGED_FILE = "MergedInventory.txt";

    // ── GUI components ─────────────────────────────────────────────────
    private JTextField  portField;
    private JTextArea   logArea;
    private JLabel      statusLabel;
    private JButton     startButton;
    private JButton     stopButton;

    // ── Server state ───────────────────────────────────────────────────
    private ServerSocket        serverSocket;
    private ExecutorService     threadPool;
    private volatile boolean    running = false;
    private Thread              acceptThread;

    // ── Overview cache (computed once by thread pool) ──────────────────
    private String overviewCache = null;

    
    public Server() {
        setTitle("Inventory Server  |  Student ID: " + STUDENT_ID);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(245, 245, 250));

        // ── Top panel: IP, Port, Start/Stop, Status ────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topPanel.setBackground(new Color(230, 230, 240));
        topPanel.setBorder(BorderFactory.createTitledBorder("Server Control"));

        // Auto-detect IP
        String ip = "Unknown";
        try { ip = InetAddress.getLocalHost().getHostAddress(); }
        catch (UnknownHostException ignored) {}

        topPanel.add(new JLabel("IP:"));
        JTextField ipField = new JTextField(ip, 12);
        ipField.setEditable(false);
        ipField.setFont(new Font("Monospaced", Font.BOLD, 12));
        topPanel.add(ipField);

        topPanel.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        portField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        topPanel.add(portField);

        startButton = new JButton("▶ Start");
        startButton.setBackground(new Color(60, 180, 75));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startServer());
        topPanel.add(startButton);

        stopButton = new JButton("■ Stop");
        stopButton.setBackground(new Color(220, 60, 60));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        topPanel.add(stopButton);

        statusLabel = new JLabel("● STOPPED");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(Color.RED);
        topPanel.add(statusLabel);

        root.add(topPanel, BorderLayout.NORTH);

        // ── Center: activity log ───────────────────────────────────────
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(20, 20, 30));
        logArea.setForeground(new Color(0, 230, 100));
        logArea.setMargin(new Insets(6, 8, 6, 8));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        root.add(scrollPane, BorderLayout.CENTER);

        // ── Bottom: Clear Log button ───────────────────────────────────
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(245, 245, 250));
        JButton clearBtn = new JButton("Clear Log");
        clearBtn.addActionListener(e -> logArea.setText(""));
        bottomPanel.add(clearBtn);
        root.add(bottomPanel, BorderLayout.SOUTH);

        add(root);
    }

    
    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            threadPool   = Executors.newFixedThreadPool(4);
            running      = true;

            // Update GUI
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            portField.setEditable(false);
            statusLabel.setText("● RUNNING");
            statusLabel.setForeground(new Color(0, 160, 0));

            // Personalized startup log entry (required by spec)
            log("Server started by " + STUDENT_NAME + " (ID: " + STUDENT_ID + ")");
            log("Server started on port " + port);

            // Pre-compute merged inventory & overview in background thread pool
            threadPool.submit(this::buildMergedInventory);

            // Accept clients in a dedicated thread
            acceptThread = new Thread(this::acceptClients, "AcceptThread");
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException ex) {
            log("ERROR starting server: " + ex.getMessage());
        }
    }

    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}

        if (threadPool != null) threadPool.shutdownNow();

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        portField.setEditable(true);
        statusLabel.setText("● STOPPED");
        statusLabel.setForeground(Color.RED);
        log("Server stopped");
    }

    
    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                log("Client connected from " + clientIP);
                // Handle each client in thread pool
                threadPool.submit(() -> handleClient(clientSocket, clientIP));
            } catch (IOException ex) {
                if (running) log("Socket error: " + ex.getMessage());
            }
        }
    }

    
    private void handleClient(Socket socket, String clientIP) {
        try (
            BufferedReader  in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter     out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            // 1. Send file list
            log("Sending file list to " + clientIP);
            out.println("FILELIST");
            for (String f : INVENTORY_FILES) out.println(f);
            out.println("END");

            // 2. Listen for client requests
            String request;
            while ((request = in.readLine()) != null) {
                request = request.trim();

                if (request.startsWith("GET:")) {
                    // Client wants a specific file
                    String filename = request.substring(4).trim();
                    log("Client requested file: " + filename);
                    sendFile(out, filename);

                } else if (request.equals("OVERVIEW")) {
                    // Client wants statistics
                    log("Client requested overview statistics");
                    sendOverview(out);

                } else if (request.equals("VERIFY")) {
                    // Personalization verification
                    int activePort = serverSocket.getLocalPort();
                    int hash = (DIGIT_SUM * activePort) % 1000;
                    log("VERIFY request from " + clientIP + " → hash=" + hash);
                    out.println("VERIFY_RESULT:" + hash);

                } else if (request.equals("DISCONNECT")) {
                    log("Client disconnected: " + clientIP);
                    break;
                }
            }
        } catch (IOException ex) {
            log("Client error (" + clientIP + "): " + ex.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    
    private void sendFile(PrintWriter out, String filename) {
        File f = new File(filename);
        if (!f.exists()) {
            out.println("ERROR:File not found: " + filename);
            return;
        }
        out.println("FILEDATA");
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) out.println(line);
        } catch (IOException ex) {
            out.println("ERROR:" + ex.getMessage());
        }
        out.println("END");
    }

   
    private void sendOverview(PrintWriter out) {
        // If not ready yet, build synchronously as fallback
        if (overviewCache == null) buildMergedInventory();

        if (overviewCache != null) {
            out.println("OVERVIEW");
            out.println(overviewCache);
            out.println("END");
        } else {
            out.println("ERROR:Overview not available yet. Try again.");
        }
    }

    
    private void buildMergedInventory() {
        log("Thread pool: starting merged inventory build...");

        // Submit one task per file → collect futures
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Map<String, double[]>>> futures = new ArrayList<>();

        for (String filename : INVENTORY_FILES) {
            futures.add(pool.submit(() -> readInventoryFile(filename)));
        }

        // Master map: productName → [highestPrice]
        // (deduplication: keep highest price across all files)
        Map<String, double[]> master = new LinkedHashMap<>();

        for (Future<Map<String, double[]>> future : futures) {
            try {
                Map<String, double[]> fileData = future.get();
                for (Map.Entry<String, double[]> entry : fileData.entrySet()) {
                    String name  = entry.getKey();
                    double price = entry.getValue()[0];
                    if (!master.containsKey(name) || price > master.get(name)[0]) {
                        master.put(name, new double[]{price});
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                log("Thread pool error: " + ex.getMessage());
            }
        }

        pool.shutdown();

        // Write MergedInventory.txt
        try (PrintWriter pw = new PrintWriter(new FileWriter(MERGED_FILE))) {
            int idx = 1;
            for (Map.Entry<String, double[]> e : master.entrySet()) {
                pw.printf("M%04d, %s, %.2f%n", idx++, e.getKey(), e.getValue()[0]);
            }
            log("Thread pool: MergedInventory.txt written (" + master.size() + " records)");
        } catch (IOException ex) {
            log("Error writing merged file: " + ex.getMessage());
            return;
        }

        // Compute statistics from merged file
        computeStatistics(master);
    }

    /**
     * Reads one inventory file and returns a map of productName → price.
     * This method runs inside a thread pool thread.
     */
    private Map<String, double[]> readInventoryFile(String filename) {
        Map<String, double[]> data = new LinkedHashMap<>();
        File f = new File(filename);
        if (!f.exists()) {
            log("WARNING: " + filename + " not found, skipping.");
            return data;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;
                String name = parts[1].trim();
                try {
                    double price = Double.parseDouble(parts[2].trim());
                    data.put(name, new double[]{price});
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ex) {
            log("Error reading " + filename + ": " + ex.getMessage());
        }
        return data;
    }

    /**
     * Computes average, max, min from merged map and caches the result.
     */
    private void computeStatistics(Map<String, double[]> master) {
        if (master.isEmpty()) {
            overviewCache = "No data available.";
            return;
        }

        double   sum     = 0;
        double   maxP    = Double.MIN_VALUE;
        double   minP    = Double.MAX_VALUE;
        String   maxName = "";
        String   minName = "";

        for (Map.Entry<String, double[]> e : master.entrySet()) {
            double price = e.getValue()[0];
            sum += price;
            if (price > maxP) { maxP = price; maxName = e.getKey(); }
            if (price < minP) { minP = price; minName = e.getKey(); }
        }

        double avg = sum / master.size();

        overviewCache =
            "Total Products : " + master.size()                          + "\n" +
            "Average Price  : $" + String.format("%.2f", avg)            + "\n" +
            "Highest Price  : $" + String.format("%.2f", maxP) +
                              " (" + maxName + ")"                        + "\n" +
            "Lowest Price   : $" + String.format("%.2f", minP) +
                              " (" + minName + ")";

        log("Thread pool: overview computed → avg=$" + String.format("%.2f", avg));
    }

    
    private void log(String message) {
        String entry = "[" + new java.util.Date() + "]  " + message;
        SwingUtilities.invokeLater(() -> {
            logArea.append(entry + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        System.out.println(entry);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Server server = new Server();
            server.setVisible(true);
        });
    }
}
