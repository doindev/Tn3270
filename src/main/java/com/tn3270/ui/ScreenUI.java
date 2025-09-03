package com.tn3270.ui;

public interface ScreenUI {
    void updateScreen(char[] buffer, int rows, int cols);
    
    void setCursorPosition(int row, int col);
    
    void setKeyboardLocked(boolean locked);
    
    void soundAlarm();
    
    void clearScreen();
    
    void highlightField(int start, int end, boolean protected);
    
    void setStatusMessage(String message);
    
    void refresh();
    
    boolean isVisible();
    
    void show();
    
    void hide();
    
    void dispose();
    
    void addKeyListener(KeyListener listener);
    
    void removeKeyListener(KeyListener listener);
    
    public interface KeyListener {
        void keyPressed(KeyEvent event);
        void keyReleased(KeyEvent event);
        void keyTyped(KeyEvent event);
    }
    
    public static class KeyEvent {
        public enum KeyCode {
            ENTER, TAB, BACKTAB, HOME, END, 
            LEFT, RIGHT, UP, DOWN,
            DELETE, BACKSPACE,
            PF1, PF2, PF3, PF4, PF5, PF6, PF7, PF8, PF9, PF10,
            PF11, PF12, PF13, PF14, PF15, PF16, PF17, PF18, PF19, PF20,
            PF21, PF22, PF23, PF24,
            PA1, PA2, PA3,
            CLEAR, RESET, ATTN, SYSREQ,
            UNKNOWN
        }
        
        private final KeyCode keyCode;
        private final char keyChar;
        private final boolean shift;
        private final boolean ctrl;
        private final boolean alt;
        
        public KeyEvent(KeyCode keyCode, char keyChar, boolean shift, boolean ctrl, boolean alt) {
            this.keyCode = keyCode;
            this.keyChar = keyChar;
            this.shift = shift;
            this.ctrl = ctrl;
            this.alt = alt;
        }
        
        public KeyCode getKeyCode() {
            return keyCode;
        }
        
        public char getKeyChar() {
            return keyChar;
        }
        
        public boolean isShift() {
            return shift;
        }
        
        public boolean isCtrl() {
            return ctrl;
        }
        
        public boolean isAlt() {
            return alt;
        }
    }
}