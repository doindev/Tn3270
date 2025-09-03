package com.tn3270;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CopyOnWriteArrayList;
import com.tn3270.ui.ScreenUI;

public class Screen {
    private static final int DEFAULT_ROWS = 24;
    private static final int DEFAULT_COLS = 80;
    
    private final Tn3270 tn3270;
    private final char[] buffer;
    private final byte[] attributes;
    private final Field[] fields;
    
    private int rows;
    private int cols;
    private int bufferSize;
    private int cursorPosition;
    private boolean insertMode;
    private boolean keyboardLocked;
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition dataArrived = lock.newCondition();
    private volatile boolean modified;
    
    private final List<ScreenUI> attachedUIs = new CopyOnWriteArrayList<>();
    
    public Screen(Tn3270 tn3270) {
        this(tn3270, DEFAULT_ROWS, DEFAULT_COLS);
    }
    
    public Screen(Tn3270 tn3270, int rows, int cols) {
        this.tn3270 = tn3270;
        this.rows = rows;
        this.cols = cols;
        this.bufferSize = rows * cols;
        this.buffer = new char[bufferSize];
        this.attributes = new byte[bufferSize];
        this.fields = new Field[bufferSize];
        clear();
    }
    
    public Screen putString(String text) {
        return putString(cursorPosition, text);
    }
    
    public Screen putString(int position, String text) {
        lock.lock();
        try {
            if (text == null || text.isEmpty()) {
                return this;
            }
            
            position = normalizePosition(position);
            Field field = getFieldAt(position);
            
            if (field != null && !field.isProtected()) {
                int maxLength = Math.min(text.length(), field.getLength() - (position - field.getStart()));
                for (int i = 0; i < maxLength; i++) {
                    int pos = (position + i) % bufferSize;
                    buffer[pos] = text.charAt(i);
                    modified = true;
                }
                cursorPosition = (position + maxLength) % bufferSize;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen enter() {
        sendAID(AIDKey.ENTER);
        return this;
    }
    
    public Screen pf1() {
        sendAID(AIDKey.PF1);
        return this;
    }
    
    public Screen pf2() {
        sendAID(AIDKey.PF2);
        return this;
    }
    
    public Screen pf3() {
        sendAID(AIDKey.PF3);
        return this;
    }
    
    public Screen pf4() {
        sendAID(AIDKey.PF4);
        return this;
    }
    
    public Screen pf5() {
        sendAID(AIDKey.PF5);
        return this;
    }
    
    public Screen pf6() {
        sendAID(AIDKey.PF6);
        return this;
    }
    
    public Screen pf7() {
        sendAID(AIDKey.PF7);
        return this;
    }
    
    public Screen pf8() {
        sendAID(AIDKey.PF8);
        return this;
    }
    
    public Screen pf9() {
        sendAID(AIDKey.PF9);
        return this;
    }
    
    public Screen pf10() {
        sendAID(AIDKey.PF10);
        return this;
    }
    
    public Screen pf11() {
        sendAID(AIDKey.PF11);
        return this;
    }
    
    public Screen pf12() {
        sendAID(AIDKey.PF12);
        return this;
    }
    
    public Screen pf13() {
        sendAID(AIDKey.PF13);
        return this;
    }
    
    public Screen pf14() {
        sendAID(AIDKey.PF14);
        return this;
    }
    
    public Screen pf15() {
        sendAID(AIDKey.PF15);
        return this;
    }
    
    public Screen pf16() {
        sendAID(AIDKey.PF16);
        return this;
    }
    
    public Screen pf17() {
        sendAID(AIDKey.PF17);
        return this;
    }
    
    public Screen pf18() {
        sendAID(AIDKey.PF18);
        return this;
    }
    
    public Screen pf19() {
        sendAID(AIDKey.PF19);
        return this;
    }
    
    public Screen pf20() {
        sendAID(AIDKey.PF20);
        return this;
    }
    
    public Screen pf21() {
        sendAID(AIDKey.PF21);
        return this;
    }
    
    public Screen pf22() {
        sendAID(AIDKey.PF22);
        return this;
    }
    
    public Screen pf23() {
        sendAID(AIDKey.PF23);
        return this;
    }
    
    public Screen pf24() {
        sendAID(AIDKey.PF24);
        return this;
    }
    
    public Screen pa1() {
        sendAID(AIDKey.PA1);
        return this;
    }
    
    public Screen pa2() {
        sendAID(AIDKey.PA2);
        return this;
    }
    
    public Screen pa3() {
        sendAID(AIDKey.PA3);
        return this;
    }
    
    public Screen clear() {
        sendAID(AIDKey.CLEAR);
        lock.lock();
        try {
            for (int i = 0; i < bufferSize; i++) {
                buffer[i] = ' ';
                attributes[i] = 0;
                fields[i] = null;
            }
            cursorPosition = 0;
            modified = false;
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen tab() {
        lock.lock();
        try {
            Field nextField = getNextUnprotectedField(cursorPosition);
            if (nextField != null) {
                cursorPosition = nextField.getStart();
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen backTab() {
        lock.lock();
        try {
            Field prevField = getPreviousUnprotectedField(cursorPosition);
            if (prevField != null) {
                cursorPosition = prevField.getStart();
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen home() {
        lock.lock();
        try {
            Field firstField = getFirstUnprotectedField();
            if (firstField != null) {
                cursorPosition = firstField.getStart();
            } else {
                cursorPosition = 0;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen end() {
        lock.lock();
        try {
            Field field = getFieldAt(cursorPosition);
            if (field != null) {
                int endPos = field.getStart() + field.getLength() - 1;
                while (endPos > field.getStart() && buffer[endPos] == ' ') {
                    endPos--;
                }
                cursorPosition = endPos + 1;
                if (cursorPosition >= field.getStart() + field.getLength()) {
                    cursorPosition = field.getStart() + field.getLength() - 1;
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen left() {
        return left(1);
    }
    
    public Screen left(int count) {
        lock.lock();
        try {
            cursorPosition = Math.max(0, cursorPosition - count);
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen right() {
        return right(1);
    }
    
    public Screen right(int count) {
        lock.lock();
        try {
            cursorPosition = Math.min(bufferSize - 1, cursorPosition + count);
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen up() {
        return up(1);
    }
    
    public Screen up(int count) {
        lock.lock();
        try {
            int newRow = Math.max(0, getRow(cursorPosition) - count);
            cursorPosition = newRow * cols + getCol(cursorPosition);
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen down() {
        return down(1);
    }
    
    public Screen down(int count) {
        lock.lock();
        try {
            int newRow = Math.min(rows - 1, getRow(cursorPosition) + count);
            cursorPosition = newRow * cols + getCol(cursorPosition);
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen delete() {
        lock.lock();
        try {
            Field field = getFieldAt(cursorPosition);
            if (field != null && !field.isProtected()) {
                int fieldEnd = field.getStart() + field.getLength();
                for (int i = cursorPosition; i < fieldEnd - 1; i++) {
                    buffer[i] = buffer[i + 1];
                }
                buffer[fieldEnd - 1] = ' ';
                modified = true;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen backspace() {
        lock.lock();
        try {
            if (cursorPosition > 0) {
                cursorPosition--;
                delete();
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen eraseEOF() {
        lock.lock();
        try {
            Field field = getFieldAt(cursorPosition);
            if (field != null && !field.isProtected()) {
                int fieldEnd = field.getStart() + field.getLength();
                for (int i = cursorPosition; i < fieldEnd; i++) {
                    buffer[i] = ' ';
                }
                modified = true;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen eraseField() {
        lock.lock();
        try {
            Field field = getFieldAt(cursorPosition);
            if (field != null && !field.isProtected()) {
                for (int i = field.getStart(); i < field.getStart() + field.getLength(); i++) {
                    buffer[i] = ' ';
                }
                cursorPosition = field.getStart();
                modified = true;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen reset() {
        sendAID(AIDKey.RESET);
        lock.lock();
        try {
            keyboardLocked = false;
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen attn() {
        sendAID(AIDKey.ATTN);
        return this;
    }
    
    public Screen sysReq() {
        sendAID(AIDKey.SYSREQ);
        return this;
    }
    
    public Screen setCursor(int row, int col) {
        return setCursorPosition(row * cols + col);
    }
    
    public Screen setCursorPosition(int position) {
        lock.lock();
        try {
            cursorPosition = normalizePosition(position);
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen waitForUnlock() {
        return waitForUnlock(30, TimeUnit.SECONDS);
    }
    
    public Screen waitForUnlock(long timeout, TimeUnit unit) {
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (keyboardLocked && nanos > 0) {
                nanos = dataArrived.awaitNanos(nanos);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public Screen waitForText(String text) {
        return waitForText(text, 30, TimeUnit.SECONDS);
    }
    
    public Screen waitForText(String text, long timeout, TimeUnit unit) {
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (!containsText(text) && nanos > 0) {
                nanos = dataArrived.awaitNanos(nanos);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return this;
    }
    
    public String getString(int position, int length) {
        lock.lock();
        try {
            position = normalizePosition(position);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length && position + i < bufferSize; i++) {
                sb.append(buffer[position + i]);
            }
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }
    
    public String getFieldString(int fieldNumber) {
        lock.lock();
        try {
            Field field = getFieldByNumber(fieldNumber);
            if (field != null) {
                return getString(field.getStart(), field.getLength()).trim();
            }
            return "";
        } finally {
            lock.unlock();
        }
    }
    
    public List<Field> getFields() {
        lock.lock();
        try {
            List<Field> fieldList = new ArrayList<>();
            for (Field field : fields) {
                if (field != null && !fieldList.contains(field)) {
                    fieldList.add(field);
                }
            }
            return fieldList;
        } finally {
            lock.unlock();
        }
    }
    
    public int getCursorPosition() {
        return cursorPosition;
    }
    
    public int getCursorRow() {
        return getRow(cursorPosition);
    }
    
    public int getCursorCol() {
        return getCol(cursorPosition);
    }
    
    public boolean isKeyboardLocked() {
        return keyboardLocked;
    }
    
    public String getText() {
        lock.lock();
        try {
            return new String(buffer);
        } finally {
            lock.unlock();
        }
    }
    
    public String[] getLines() {
        lock.lock();
        try {
            String[] lines = new String[rows];
            for (int row = 0; row < rows; row++) {
                lines[row] = new String(buffer, row * cols, cols);
            }
            return lines;
        } finally {
            lock.unlock();
        }
    }
    
    public Screen attachUI(ScreenUI ui) {
        if (ui != null && !attachedUIs.contains(ui)) {
            attachedUIs.add(ui);
            ui.addKeyListener(new UIKeyListener());
            updateUI(ui);
        }
        return this;
    }
    
    public Screen detachUI(ScreenUI ui) {
        if (ui != null) {
            attachedUIs.remove(ui);
        }
        return this;
    }
    
    public List<ScreenUI> getAttachedUIs() {
        return new ArrayList<>(attachedUIs);
    }
    
    private void updateAllUIs() {
        for (ScreenUI ui : attachedUIs) {
            updateUI(ui);
        }
    }
    
    private void updateUI(ScreenUI ui) {
        lock.lock();
        try {
            ui.updateScreen(buffer.clone(), rows, cols);
            ui.setCursorPosition(getCursorRow(), getCursorCol());
            ui.setKeyboardLocked(keyboardLocked);
            ui.refresh();
        } finally {
            lock.unlock();
        }
    }
    
    void processIncomingData(byte[] data) {
        lock.lock();
        try {
            DataStreamProcessor processor = new DataStreamProcessor(this);
            processor.process(data);
            dataArrived.signalAll();
            updateAllUIs();
        } finally {
            lock.unlock();
        }
    }
    
    private void sendAID(AIDKey aid) {
        try {
            DataStreamBuilder builder = new DataStreamBuilder(this);
            byte[] data = builder.buildAIDData(aid);
            tn3270.sendData(data);
            
            lock.lock();
            try {
                keyboardLocked = true;
            } finally {
                lock.unlock();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send AID key: " + aid, e);
        }
    }
    
    private int normalizePosition(int position) {
        if (position < 0) return 0;
        if (position >= bufferSize) return bufferSize - 1;
        return position;
    }
    
    private int getRow(int position) {
        return position / cols;
    }
    
    private int getCol(int position) {
        return position % cols;
    }
    
    private Field getFieldAt(int position) {
        return fields[normalizePosition(position)];
    }
    
    private Field getFieldByNumber(int number) {
        List<Field> fieldList = getFields();
        if (number >= 0 && number < fieldList.size()) {
            return fieldList.get(number);
        }
        return null;
    }
    
    private Field getNextUnprotectedField(int position) {
        int start = position;
        do {
            position = (position + 1) % bufferSize;
            Field field = fields[position];
            if (field != null && !field.isProtected() && position == field.getStart()) {
                return field;
            }
        } while (position != start);
        return null;
    }
    
    private Field getPreviousUnprotectedField(int position) {
        int start = position;
        do {
            position = (position - 1 + bufferSize) % bufferSize;
            Field field = fields[position];
            if (field != null && !field.isProtected() && position == field.getStart()) {
                return field;
            }
        } while (position != start);
        return null;
    }
    
    private Field getFirstUnprotectedField() {
        for (int i = 0; i < bufferSize; i++) {
            Field field = fields[i];
            if (field != null && !field.isProtected() && i == field.getStart()) {
                return field;
            }
        }
        return null;
    }
    
    private boolean containsText(String text) {
        String screenText = new String(buffer);
        return screenText.contains(text);
    }
    
    void setBuffer(int position, char ch) {
        buffer[normalizePosition(position)] = ch;
    }
    
    void setAttribute(int position, byte attr) {
        attributes[normalizePosition(position)] = attr;
    }
    
    void setField(int position, Field field) {
        fields[normalizePosition(position)] = field;
    }
    
    void setKeyboardLocked(boolean locked) {
        this.keyboardLocked = locked;
    }
    
    void setCursorPositionInternal(int position) {
        this.cursorPosition = normalizePosition(position);
    }
    
    int getRows() {
        return rows;
    }
    
    int getCols() {
        return cols;
    }
    
    int getBufferSize() {
        return bufferSize;
    }
    
    private class UIKeyListener implements ScreenUI.KeyListener {
        @Override
        public void keyPressed(ScreenUI.KeyEvent event) {
            switch (event.getKeyCode()) {
                case ENTER:
                    enter();
                    break;
                case TAB:
                    tab();
                    break;
                case BACKTAB:
                    backTab();
                    break;
                case HOME:
                    home();
                    break;
                case END:
                    end();
                    break;
                case LEFT:
                    left();
                    break;
                case RIGHT:
                    right();
                    break;
                case UP:
                    up();
                    break;
                case DOWN:
                    down();
                    break;
                case DELETE:
                    delete();
                    break;
                case BACKSPACE:
                    backspace();
                    break;
                case PF1:
                    pf1();
                    break;
                case PF2:
                    pf2();
                    break;
                case PF3:
                    pf3();
                    break;
                case PF4:
                    pf4();
                    break;
                case PF5:
                    pf5();
                    break;
                case PF6:
                    pf6();
                    break;
                case PF7:
                    pf7();
                    break;
                case PF8:
                    pf8();
                    break;
                case PF9:
                    pf9();
                    break;
                case PF10:
                    pf10();
                    break;
                case PF11:
                    pf11();
                    break;
                case PF12:
                    pf12();
                    break;
                case PA1:
                    pa1();
                    break;
                case PA2:
                    pa2();
                    break;
                case PA3:
                    pa3();
                    break;
                case CLEAR:
                    clear();
                    break;
                case RESET:
                    reset();
                    break;
                case ATTN:
                    attn();
                    break;
                case SYSREQ:
                    sysReq();
                    break;
            }
            updateAllUIs();
        }
        
        @Override
        public void keyReleased(ScreenUI.KeyEvent event) {
        }
        
        @Override
        public void keyTyped(ScreenUI.KeyEvent event) {
            if (!keyboardLocked && event.getKeyChar() != 0) {
                putString(String.valueOf(event.getKeyChar()));
                updateAllUIs();
            }
        }
    }
}