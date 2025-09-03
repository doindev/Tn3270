package com.tn3270.example;

import com.tn3270.Tn3270;
import com.tn3270.Screen;
import com.tn3270.TerminalType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Demonstration of TN3270 emulator with Swing UI
 * 
 * This example shows how to:
 * 1. Create a TN3270 connection
 * 2. Attach a Swing UI to visualize the terminal
 * 3. Interact with the mainframe both programmatically and through the UI
 */
public class UIDemo {
    
    private static Tn3270 tn3270;
    private static SwingScreenUI ui;
    private static JFrame controlFrame;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
    
    private static void createAndShowGUI() {
        // Create the terminal UI
        ui = new SwingScreenUI();
        ui.setTitle("TN3270 Terminal Emulator - Disconnected");
        
        // Create control panel
        controlFrame = createControlPanel();
        controlFrame.setVisible(true);
        
        // Position windows
        ui.setLocation(100, 100);
        controlFrame.setLocation(ui.getX() + ui.getWidth() + 20, 100);
        
        ui.show();
    }
    
    private static JFrame createControlPanel() {
        JFrame frame = new JFrame("TN3270 Control Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Host field
        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        JTextField hostField = new JTextField("pub400.com", 20);
        connectionPanel.add(hostField, gbc);
        
        // Port field
        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        JTextField portField = new JTextField("23", 20);
        connectionPanel.add(portField, gbc);
        
        // SSL Port field
        gbc.gridx = 0; gbc.gridy = 2;
        connectionPanel.add(new JLabel("SSL Port:"), gbc);
        gbc.gridx = 1;
        JTextField sslPortField = new JTextField("992", 20);
        connectionPanel.add(sslPortField, gbc);
        
        // Terminal type
        gbc.gridx = 0; gbc.gridy = 3;
        connectionPanel.add(new JLabel("Terminal:"), gbc);
        gbc.gridx = 1;
        JComboBox<TerminalType> terminalCombo = new JComboBox<>(TerminalType.values());
        terminalCombo.setSelectedItem(TerminalType.IBM_3278_2_E);
        connectionPanel.add(terminalCombo, gbc);
        
        // Connection buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton connectButton = new JButton("Connect");
        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        
        // Status area
        JTextArea statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Status"));
        
        // Quick commands panel
        JPanel commandPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        commandPanel.setBorder(BorderFactory.createTitledBorder("Quick Commands"));
        
        JButton clearButton = new JButton("Clear Screen");
        JButton resetButton = new JButton("Reset");
        JButton enterButton = new JButton("Enter");
        JButton tabButton = new JButton("Tab");
        
        commandPanel.add(clearButton);
        commandPanel.add(resetButton);
        commandPanel.add(enterButton);
        commandPanel.add(tabButton);
        
        // Add PF key buttons
        for (int i = 1; i <= 12; i++) {
            final int pfNum = i;
            JButton pfButton = new JButton("PF" + i);
            pfButton.addActionListener(e -> sendPFKey(pfNum));
            if (i <= 4) {
                commandPanel.add(pfButton);
            }
        }
        
        // Sample automation panel
        JPanel automationPanel = new JPanel(new FlowLayout());
        automationPanel.setBorder(BorderFactory.createTitledBorder("Sample Automation"));
        
        JButton sampleLoginButton = new JButton("Sample Login");
        JButton readScreenButton = new JButton("Read Screen");
        JButton getFieldsButton = new JButton("Get Fields");
        
        automationPanel.add(sampleLoginButton);
        automationPanel.add(readScreenButton);
        automationPanel.add(getFieldsButton);
        
        // Layout
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.add(connectionPanel);
        frame.add(buttonPanel);
        frame.add(commandPanel);
        frame.add(automationPanel);
        frame.add(statusScroll);
        
        // Event handlers
        connectButton.addActionListener(e -> {
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());
            int sslPort = Integer.parseInt(sslPortField.getText());
            TerminalType terminalType = (TerminalType) terminalCombo.getSelectedItem();
            
            statusArea.append("Connecting to " + host + "...\n");
            
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    tn3270 = new Tn3270()
                        .setHost(host)
                        .setPort(port)
                        .setSslPort(sslPort)
                        .setTerminalType(terminalType);
                    
                    try {
                        tn3270.connect();
                        return true;
                    } catch (IOException ex) {
                        statusArea.append("Connection failed: " + ex.getMessage() + "\n");
                        return false;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            // Attach UI to screen
                            tn3270.getScreen().attachUI(ui);
                            
                            connectButton.setEnabled(false);
                            disconnectButton.setEnabled(true);
                            ui.setTitle("TN3270 Terminal Emulator - Connected to " + host);
                            statusArea.append("Connected successfully!\n");
                            statusArea.append("Terminal type: " + terminalType.getName() + "\n");
                        }
                    } catch (Exception ex) {
                        statusArea.append("Error: " + ex.getMessage() + "\n");
                    }
                }
            }.execute();
        });
        
        disconnectButton.addActionListener(e -> {
            if (tn3270 != null) {
                tn3270.disconnect();
                tn3270 = null;
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                ui.setTitle("TN3270 Terminal Emulator - Disconnected");
                ui.clearScreen();
                statusArea.append("Disconnected\n");
            }
        });
        
        clearButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                tn3270.getScreen().clear();
            }
        });
        
        resetButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                tn3270.getScreen().reset();
            }
        });
        
        enterButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                tn3270.getScreen().enter();
            }
        });
        
        tabButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                tn3270.getScreen().tab();
            }
        });
        
        sampleLoginButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                statusArea.append("Executing sample login sequence...\n");
                new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Screen screen = tn3270.getScreen();
                        
                        publish("Waiting for login prompt...");
                        screen.waitForUnlock();
                        
                        publish("Entering credentials...");
                        screen
                            .putString("USER123")
                            .tab()
                            .putString("PASS123")
                            .enter();
                        
                        publish("Waiting for response...");
                        screen.waitForUnlock();
                        
                        publish("Login sequence completed");
                        return null;
                    }
                    
                    @Override
                    protected void process(java.util.List<String> chunks) {
                        for (String message : chunks) {
                            statusArea.append(message + "\n");
                        }
                    }
                }.execute();
            }
        });
        
        readScreenButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                Screen screen = tn3270.getScreen();
                String screenText = screen.getText();
                
                statusArea.append("\n=== Screen Content ===\n");
                // Show first 5 lines
                String[] lines = screen.getLines();
                for (int i = 0; i < Math.min(5, lines.length); i++) {
                    statusArea.append(String.format("Line %02d: %s\n", i + 1, lines[i].trim()));
                }
                statusArea.append("...\n");
                
                // Show cursor position
                statusArea.append(String.format("Cursor at: Row=%d, Col=%d\n", 
                    screen.getCursorRow() + 1, 
                    screen.getCursorCol() + 1));
            }
        });
        
        getFieldsButton.addActionListener(e -> {
            if (tn3270 != null && tn3270.isConnected()) {
                Screen screen = tn3270.getScreen();
                statusArea.append("\n=== Fields ===\n");
                
                java.util.List<com.tn3270.Field> fields = screen.getFields();
                statusArea.append("Total fields: " + fields.size() + "\n");
                
                int count = 0;
                for (com.tn3270.Field field : fields) {
                    if (count++ < 5) {
                        statusArea.append(field.toString() + "\n");
                    }
                }
                if (fields.size() > 5) {
                    statusArea.append("... and " + (fields.size() - 5) + " more\n");
                }
            }
        });
        
        frame.pack();
        return frame;
    }
    
    private static void sendPFKey(int pfNum) {
        if (tn3270 != null && tn3270.isConnected()) {
            Screen screen = tn3270.getScreen();
            switch (pfNum) {
                case 1: screen.pf1(); break;
                case 2: screen.pf2(); break;
                case 3: screen.pf3(); break;
                case 4: screen.pf4(); break;
                case 5: screen.pf5(); break;
                case 6: screen.pf6(); break;
                case 7: screen.pf7(); break;
                case 8: screen.pf8(); break;
                case 9: screen.pf9(); break;
                case 10: screen.pf10(); break;
                case 11: screen.pf11(); break;
                case 12: screen.pf12(); break;
            }
        }
    }
}