package com.tn3270.example;

import com.tn3270.ui.ScreenUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SwingScreenUI extends JFrame implements ScreenUI {
    private final JTextArea textArea;
    private final JLabel statusBar;
    private final List<KeyListener> keyListeners = new CopyOnWriteArrayList<>();
    private int cursorRow;
    private int cursorCol;
    private boolean keyboardLocked;
    
    public SwingScreenUI() {
        super("TN3270 Terminal");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        Font terminalFont = new Font("Courier New", Font.PLAIN, 14);
        
        textArea = new JTextArea(24, 80);
        textArea.setFont(terminalFont);
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.GREEN);
        textArea.setCaretColor(Color.GREEN);
        textArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
        
        statusBar = new JLabel(" Ready");
        statusBar.setFont(new Font("Arial", Font.PLAIN, 12));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);
        
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                handleKeyPress(e);
            }
            
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                handleKeyTyped(e);
            }
        });
        
        pack();
        setLocationRelativeTo(null);
    }
    
    @Override
    public void updateScreen(char[] buffer, int rows, int cols) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    sb.append(buffer[row * cols + col]);
                }
                if (row < rows - 1) {
                    sb.append('\n');
                }
            }
            textArea.setText(sb.toString());
        });
    }
    
    @Override
    public void setCursorPosition(int row, int col) {
        this.cursorRow = row;
        this.cursorCol = col;
        SwingUtilities.invokeLater(() -> {
            try {
                int position = textArea.getLineStartOffset(row) + col;
                textArea.setCaretPosition(position);
            } catch (Exception e) {
            }
        });
    }
    
    @Override
    public void setKeyboardLocked(boolean locked) {
        this.keyboardLocked = locked;
        SwingUtilities.invokeLater(() -> {
            statusBar.setText(locked ? " Keyboard Locked" : " Ready");
            textArea.setEditable(!locked);
        });
    }
    
    @Override
    public void soundAlarm() {
        Toolkit.getDefaultToolkit().beep();
    }
    
    @Override
    public void clearScreen() {
        SwingUtilities.invokeLater(() -> textArea.setText(""));
    }
    
    @Override
    public void highlightField(int start, int end, boolean isProtected) {
    }
    
    @Override
    public void setStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> statusBar.setText(" " + message));
    }
    
    @Override
    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            textArea.repaint();
            statusBar.repaint();
        });
    }
    
    @Override
    public boolean isVisible() {
        return super.isVisible();
    }
    
    @Override
    public void show() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
    
    @Override
    public void hide() {
        SwingUtilities.invokeLater(() -> setVisible(false));
    }
    
    @Override
    public void dispose() {
        SwingUtilities.invokeLater(() -> super.dispose());
    }
    
    @Override
    public void addKeyListener(KeyListener listener) {
        keyListeners.add(listener);
    }
    
    @Override
    public void removeKeyListener(KeyListener listener) {
        keyListeners.remove(listener);
    }
    
    private void handleKeyPress(java.awt.event.KeyEvent e) {
        KeyEvent.KeyCode keyCode = mapKeyCode(e);
        if (keyCode != KeyEvent.KeyCode.UNKNOWN) {
            KeyEvent event = new KeyEvent(
                keyCode,
                (char) 0,
                e.isShiftDown(),
                e.isControlDown(),
                e.isAltDown()
            );
            for (KeyListener listener : keyListeners) {
                listener.keyPressed(event);
            }
        }
    }
    
    private void handleKeyTyped(java.awt.event.KeyEvent e) {
        if (!keyboardLocked && e.getKeyChar() != java.awt.event.KeyEvent.CHAR_UNDEFINED) {
            KeyEvent event = new KeyEvent(
                KeyEvent.KeyCode.UNKNOWN,
                e.getKeyChar(),
                e.isShiftDown(),
                e.isControlDown(),
                e.isAltDown()
            );
            for (KeyListener listener : keyListeners) {
                listener.keyTyped(event);
            }
        }
    }
    
    private KeyEvent.KeyCode mapKeyCode(java.awt.event.KeyEvent e) {
        switch (e.getKeyCode()) {
            case java.awt.event.KeyEvent.VK_ENTER: return KeyEvent.KeyCode.ENTER;
            case java.awt.event.KeyEvent.VK_TAB: 
                return e.isShiftDown() ? KeyEvent.KeyCode.BACKTAB : KeyEvent.KeyCode.TAB;
            case java.awt.event.KeyEvent.VK_HOME: return KeyEvent.KeyCode.HOME;
            case java.awt.event.KeyEvent.VK_END: return KeyEvent.KeyCode.END;
            case java.awt.event.KeyEvent.VK_LEFT: return KeyEvent.KeyCode.LEFT;
            case java.awt.event.KeyEvent.VK_RIGHT: return KeyEvent.KeyCode.RIGHT;
            case java.awt.event.KeyEvent.VK_UP: return KeyEvent.KeyCode.UP;
            case java.awt.event.KeyEvent.VK_DOWN: return KeyEvent.KeyCode.DOWN;
            case java.awt.event.KeyEvent.VK_DELETE: return KeyEvent.KeyCode.DELETE;
            case java.awt.event.KeyEvent.VK_BACK_SPACE: return KeyEvent.KeyCode.BACKSPACE;
            case java.awt.event.KeyEvent.VK_F1: return KeyEvent.KeyCode.PF1;
            case java.awt.event.KeyEvent.VK_F2: return KeyEvent.KeyCode.PF2;
            case java.awt.event.KeyEvent.VK_F3: return KeyEvent.KeyCode.PF3;
            case java.awt.event.KeyEvent.VK_F4: return KeyEvent.KeyCode.PF4;
            case java.awt.event.KeyEvent.VK_F5: return KeyEvent.KeyCode.PF5;
            case java.awt.event.KeyEvent.VK_F6: return KeyEvent.KeyCode.PF6;
            case java.awt.event.KeyEvent.VK_F7: return KeyEvent.KeyCode.PF7;
            case java.awt.event.KeyEvent.VK_F8: return KeyEvent.KeyCode.PF8;
            case java.awt.event.KeyEvent.VK_F9: return KeyEvent.KeyCode.PF9;
            case java.awt.event.KeyEvent.VK_F10: return KeyEvent.KeyCode.PF10;
            case java.awt.event.KeyEvent.VK_F11: return KeyEvent.KeyCode.PF11;
            case java.awt.event.KeyEvent.VK_F12: return KeyEvent.KeyCode.PF12;
            case java.awt.event.KeyEvent.VK_ESCAPE: 
                if (e.isControlDown()) return KeyEvent.KeyCode.ATTN;
                return KeyEvent.KeyCode.RESET;
            case java.awt.event.KeyEvent.VK_PAUSE: return KeyEvent.KeyCode.PA1;
            default: return KeyEvent.KeyCode.UNKNOWN;
        }
    }
    
    public static void main(String[] args) {
        SwingScreenUI ui = new SwingScreenUI();
        ui.show();
        
        com.tn3270.Tn3270 tn3270 = new com.tn3270.Tn3270("mainframe.example.com");
        
        try {
            tn3270.connect();
            tn3270.getScreen().attachUI(ui);
            
            Thread.sleep(60000);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tn3270.disconnect();
            ui.dispose();
        }
    }
}