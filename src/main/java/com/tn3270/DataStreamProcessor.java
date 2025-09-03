package com.tn3270;

import java.util.logging.Logger;

public class DataStreamProcessor {
    private static final Logger logger = Logger.getLogger(DataStreamProcessor.class.getName());
    
    private static final byte CMD_WRITE = (byte) 0xF1;
    private static final byte CMD_ERASE_WRITE = (byte) 0xF5;
    private static final byte CMD_ERASE_WRITE_ALTERNATE = (byte) 0x7E;
    private static final byte CMD_ERASE_ALL_UNPROTECTED = (byte) 0x6F;
    private static final byte CMD_READ_BUFFER = (byte) 0xF2;
    private static final byte CMD_READ_MODIFIED = (byte) 0xF6;
    private static final byte CMD_READ_MODIFIED_ALL = (byte) 0x6E;
    
    private static final byte ORDER_SF = (byte) 0x1D;
    private static final byte ORDER_SFE = (byte) 0x29;
    private static final byte ORDER_SBA = (byte) 0x11;
    private static final byte ORDER_IC = (byte) 0x13;
    private static final byte ORDER_PT = (byte) 0x05;
    private static final byte ORDER_RA = (byte) 0x3C;
    private static final byte ORDER_EUA = (byte) 0x12;
    private static final byte ORDER_GE = (byte) 0x08;
    private static final byte ORDER_MF = (byte) 0x2C;
    private static final byte ORDER_SA = (byte) 0x28;
    
    private static final byte WCC_KEYBOARD_RESTORE = (byte) 0x40;
    private static final byte WCC_MDT_RESET = (byte) 0x02;
    private static final byte WCC_SOUND_ALARM = (byte) 0x04;
    
    private final Screen screen;
    private byte[] data;
    private int position;
    private int currentBufferPosition;
    
    public DataStreamProcessor(Screen screen) {
        this.screen = screen;
        this.currentBufferPosition = 0;
    }
    
    public void process(byte[] data) {
        this.data = data;
        this.position = 0;
        
        if (data.length == 0) {
            return;
        }
        
        byte command = readByte();
        processCommand(command);
    }
    
    private void processCommand(byte command) {
        logger.fine(String.format("Processing command: 0x%02X", command));
        
        switch (command) {
            case CMD_WRITE:
                processWrite(false);
                break;
                
            case CMD_ERASE_WRITE:
            case CMD_ERASE_WRITE_ALTERNATE:
                processWrite(true);
                break;
                
            case CMD_ERASE_ALL_UNPROTECTED:
                processEraseAllUnprotected();
                break;
                
            case CMD_READ_BUFFER:
            case CMD_READ_MODIFIED:
            case CMD_READ_MODIFIED_ALL:
                break;
                
            default:
                logger.warning(String.format("Unknown command: 0x%02X", command));
                break;
        }
    }
    
    private void processWrite(boolean eraseFirst) {
        if (eraseFirst) {
            for (int i = 0; i < screen.getBufferSize(); i++) {
                screen.setBuffer(i, ' ');
                screen.setAttribute(i, (byte) 0);
                screen.setField(i, null);
            }
        }
        
        byte wcc = readByte();
        processWCC(wcc);
        
        while (position < data.length) {
            byte order = peekByte();
            
            if (isOrder(order)) {
                readByte();
                processOrder(order);
            } else {
                processText();
            }
        }
    }
    
    private void processWCC(byte wcc) {
        if ((wcc & WCC_KEYBOARD_RESTORE) != 0) {
            screen.setKeyboardLocked(false);
        } else {
            screen.setKeyboardLocked(true);
        }
        
        if ((wcc & WCC_MDT_RESET) != 0) {
            for (Field field : screen.getFields()) {
                if (field != null) {
                    field.setModified(false);
                }
            }
        }
        
        if ((wcc & WCC_SOUND_ALARM) != 0) {
            logger.info("Alarm sounded");
        }
    }
    
    private void processOrder(byte order) {
        switch (order) {
            case ORDER_SF:
                processStartField();
                break;
                
            case ORDER_SFE:
                processStartFieldExtended();
                break;
                
            case ORDER_SBA:
                processSetBufferAddress();
                break;
                
            case ORDER_IC:
                processInsertCursor();
                break;
                
            case ORDER_PT:
                processProgramTab();
                break;
                
            case ORDER_RA:
                processRepeatToAddress();
                break;
                
            case ORDER_EUA:
                processEraseUnprotected();
                break;
                
            case ORDER_GE:
                processGraphicEscape();
                break;
                
            case ORDER_MF:
                processModifyField();
                break;
                
            case ORDER_SA:
                processSetAttribute();
                break;
                
            default:
                logger.warning(String.format("Unknown order: 0x%02X", order));
                break;
        }
    }
    
    private void processStartField() {
        byte attribute = readByte();
        
        int nextPosition = currentBufferPosition;
        int fieldStart = (nextPosition + 1) % screen.getBufferSize();
        
        int fieldEnd = fieldStart;
        for (int i = fieldStart; i < screen.getBufferSize(); i++) {
            if (screen.getAttribute(i) != 0) {
                fieldEnd = i;
                break;
            }
        }
        
        if (fieldEnd == fieldStart) {
            fieldEnd = screen.getBufferSize();
        }
        
        int fieldLength = fieldEnd - fieldStart;
        Field field = new Field(fieldStart, fieldLength, attribute);
        
        for (int i = fieldStart; i < fieldEnd; i++) {
            screen.setField(i, field);
        }
        
        screen.setAttribute(nextPosition, attribute);
        currentBufferPosition = (currentBufferPosition + 1) % screen.getBufferSize();
    }
    
    private void processStartFieldExtended() {
        int count = readByte() & 0xFF;
        byte attribute = 0;
        
        for (int i = 0; i < count; i++) {
            byte type = readByte();
            byte value = readByte();
            
            if (type == (byte) 0xC0) {
                attribute = value;
            }
        }
        
        int nextPosition = currentBufferPosition;
        int fieldStart = (nextPosition + 1) % screen.getBufferSize();
        
        int fieldEnd = screen.getBufferSize();
        Field field = new Field(fieldStart, fieldEnd - fieldStart, attribute);
        
        for (int i = fieldStart; i < fieldEnd; i++) {
            screen.setField(i, field);
        }
        
        screen.setAttribute(nextPosition, attribute);
        currentBufferPosition = (currentBufferPosition + 1) % screen.getBufferSize();
    }
    
    private void processSetBufferAddress() {
        int address = readAddress();
        currentBufferPosition = address;
    }
    
    private void processInsertCursor() {
        screen.setCursorPositionInternal(currentBufferPosition);
    }
    
    private void processProgramTab() {
        Field nextField = null;
        int start = currentBufferPosition;
        
        for (int i = (start + 1) % screen.getBufferSize(); i != start; i = (i + 1) % screen.getBufferSize()) {
            final int position = i;
            Field field = screen.getFields().stream()
                    .filter(f -> f != null && f.getStart() == position && !f.isProtected())
                    .findFirst()
                    .orElse(null);
            
            if (field != null) {
                nextField = field;
                break;
            }
        }
        
        if (nextField != null) {
            currentBufferPosition = nextField.getStart();
        }
    }
    
    private void processRepeatToAddress() {
        int stopAddress = readAddress();
        char ch = (char) readByte();
        
        while (currentBufferPosition != stopAddress) {
            screen.setBuffer(currentBufferPosition, ch);
            currentBufferPosition = (currentBufferPosition + 1) % screen.getBufferSize();
        }
    }
    
    private void processEraseUnprotected() {
        int stopAddress = readAddress();
        
        while (currentBufferPosition != stopAddress) {
            Field field = screen.getFields().stream()
                    .filter(f -> f != null && f.contains(currentBufferPosition))
                    .findFirst()
                    .orElse(null);
            
            if (field == null || !field.isProtected()) {
                screen.setBuffer(currentBufferPosition, ' ');
            }
            
            currentBufferPosition = (currentBufferPosition + 1) % screen.getBufferSize();
        }
    }
    
    private void processEraseAllUnprotected() {
        for (int i = 0; i < screen.getBufferSize(); i++) {
            final int position = i;
            Field field = screen.getFields().stream()
                    .filter(f -> f != null && f.contains(position))
                    .findFirst()
                    .orElse(null);
            
            if (field == null || !field.isProtected()) {
                screen.setBuffer(i, ' ');
            }
        }
    }
    
    private void processGraphicEscape() {
        char ch = (char) readByte();
        screen.setBuffer(currentBufferPosition, ch);
        currentBufferPosition = (currentBufferPosition + 1) % screen.getBufferSize();
    }
    
    private void processModifyField() {
        int count = readByte() & 0xFF;
        
        for (int i = 0; i < count; i++) {
            byte type = readByte();
            byte value = readByte();
        }
    }
    
    private void processSetAttribute() {
        byte type = readByte();
        byte value = readByte();
    }
    
    private void processText() {
        while (position < data.length && !isOrder(peekByte())) {
            byte b = readByte();
            char ch = ebcdicToAscii(b);
            screen.setBuffer(currentBufferPosition, ch);
            currentBufferPosition = (currentBufferPosition + 1) % screen.getBufferSize();
        }
    }
    
    private boolean isOrder(byte b) {
        return b == ORDER_SF || b == ORDER_SFE || b == ORDER_SBA || 
               b == ORDER_IC || b == ORDER_PT || b == ORDER_RA || 
               b == ORDER_EUA || b == ORDER_GE || b == ORDER_MF || 
               b == ORDER_SA;
    }
    
    private int readAddress() {
        byte b1 = readByte();
        byte b2 = readByte();
        
        if ((b1 & 0xC0) == 0) {
            return ((b1 & 0x3F) << 8) | (b2 & 0xFF);
        } else {
            return ((b1 & 0x3F) << 6) | (b2 & 0x3F);
        }
    }
    
    private byte readByte() {
        if (position >= data.length) {
            return 0;
        }
        return data[position++];
    }
    
    private byte peekByte() {
        if (position >= data.length) {
            return 0;
        }
        return data[position];
    }
    
    private char ebcdicToAscii(byte b) {
        int index = b & 0xFF;
        return EBCDIC_TO_ASCII[index];
    }
    
    private static final char[] EBCDIC_TO_ASCII = {
        0x00, 0x01, 0x02, 0x03, 0x9C, 0x09, 0x86, 0x7F, 0x97, 0x8D, 0x8E, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x9D, 0x0A, 0x08, 0x87, 0x18, 0x19, 0x92, 0x8F, 0x1C, 0x1D, 0x1E, 0x1F,
        0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x17, 0x1B, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x05, 0x06, 0x07,
        0x90, 0x91, 0x16, 0x93, 0x94, 0x95, 0x96, 0x04, 0x98, 0x99, 0x9A, 0x9B, 0x14, 0x15, 0x9E, 0x1A,
        0x20, 0xA0, 0xE2, 0xE4, 0xE0, 0xE1, 0xE3, 0xE5, 0xE7, 0xF1, 0xA2, 0x2E, 0x3C, 0x28, 0x2B, 0x7C,
        0x26, 0xE9, 0xEA, 0xEB, 0xE8, 0xED, 0xEE, 0xEF, 0xEC, 0xDF, 0x21, 0x24, 0x2A, 0x29, 0x3B, 0x5E,
        0x2D, 0x2F, 0xC2, 0xC4, 0xC0, 0xC1, 0xC3, 0xC5, 0xC7, 0xD1, 0xA6, 0x2C, 0x25, 0x5F, 0x3E, 0x3F,
        0xF8, 0xC9, 0xCA, 0xCB, 0xC8, 0xCD, 0xCE, 0xCF, 0xCC, 0x60, 0x3A, 0x23, 0x40, 0x27, 0x3D, 0x22,
        0xD8, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0xAB, 0xBB, 0xF0, 0xFD, 0xFE, 0xB1,
        0xB0, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0xAA, 0xBA, 0xE6, 0xB8, 0xC6, 0xA4,
        0xB5, 0x7E, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0xA1, 0xBF, 0xD0, 0x5B, 0xDE, 0xAE,
        0xAC, 0xA3, 0xA5, 0xB7, 0xA9, 0xA7, 0xB6, 0xBC, 0xBD, 0xBE, 0xDD, 0xA8, 0xAF, 0x5D, 0xB4, 0xD7,
        0x7B, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0xAD, 0xF4, 0xF6, 0xF2, 0xF3, 0xF5,
        0x7D, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0xB9, 0xFB, 0xFC, 0xF9, 0xFA, 0xFF,
        0x5C, 0xF7, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0xB2, 0xD4, 0xD6, 0xD2, 0xD3, 0xD5,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0xB3, 0xDB, 0xDC, 0xD9, 0xDA, 0x9F
    };
}