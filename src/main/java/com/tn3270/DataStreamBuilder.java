package com.tn3270;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DataStreamBuilder {
    private final Screen screen;
    
    public DataStreamBuilder(Screen screen) {
        this.screen = screen;
    }
    
    public byte[] buildAIDData(AIDKey aid) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        output.write(aid.getCode());
        
        int cursorAddress = screen.getCursorPosition();
        for (byte b : encodeAddress(cursorAddress)) {
            output.write(b);
        }
        
        if (aid != AIDKey.CLEAR && aid != AIDKey.PA1 && aid != AIDKey.PA2 && aid != AIDKey.PA3) {
            List<Field> fields = screen.getFields();
            
            for (Field field : fields) {
                if (field != null && field.isModified() && !field.isProtected()) {
                    output.write((byte) 0x11);
                    for (byte b : encodeAddress(field.getStart())) {
                        output.write(b);
                    }
                    
                    String fieldData = screen.getString(field.getStart(), field.getLength()).trim();
                    byte[] ebcdicData = asciiToEbcdic(fieldData);
                    output.write(ebcdicData, 0, ebcdicData.length);
                }
            }
        }
        
        return output.toByteArray();
    }
    
    private byte[] encodeAddress(int address) {
        byte[] result = new byte[2];
        
        if (address < 4096) {
            result[0] = (byte) ((address >> 6) & 0x3F);
            result[1] = (byte) (address & 0x3F);
            
            result[0] = translateSixBitToEbcdic(result[0]);
            result[1] = translateSixBitToEbcdic(result[1]);
        } else {
            result[0] = (byte) ((address >> 8) & 0x3F);
            result[1] = (byte) (address & 0xFF);
        }
        
        return result;
    }
    
    private byte translateSixBitToEbcdic(byte sixBit) {
        if (sixBit == 0x00) return 0x40;
        if (sixBit >= 0x01 && sixBit <= 0x09) return (byte) (0xC0 + sixBit);
        if (sixBit >= 0x0A && sixBit <= 0x12) return (byte) (0xC9 + sixBit - 0x09);
        if (sixBit >= 0x13 && sixBit <= 0x1B) return (byte) (0xD1 + sixBit - 0x12);
        if (sixBit >= 0x1C && sixBit <= 0x24) return (byte) (0xD9 + sixBit - 0x1B);
        if (sixBit >= 0x25 && sixBit <= 0x2D) return (byte) (0xE1 + sixBit - 0x24);
        if (sixBit >= 0x2E && sixBit <= 0x36) return (byte) (0xE9 + sixBit - 0x2D);
        if (sixBit >= 0x37 && sixBit <= 0x3F) return (byte) (0xF0 + sixBit - 0x36);
        return 0x40;
    }
    
    private byte[] asciiToEbcdic(String text) {
        byte[] result = new byte[text.length()];
        for (int i = 0; i < text.length(); i++) {
            result[i] = asciiToEbcdic(text.charAt(i));
        }
        return result;
    }
    
    private byte asciiToEbcdic(char ch) {
        if (ch < 256) {
            return ASCII_TO_EBCDIC[ch];
        }
        return 0x3F;
    }
    
    private static final byte[] ASCII_TO_EBCDIC = {
        (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x37, (byte)0x2D, (byte)0x2E, (byte)0x2F, (byte)0x16, (byte)0x05, (byte)0x15, (byte)0x0B, (byte)0x0C, (byte)0x0D, (byte)0x0E, (byte)0x0F,
        (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x3C, (byte)0x3D, (byte)0x32, (byte)0x26, (byte)0x18, (byte)0x19, (byte)0x3F, (byte)0x27, (byte)0x1C, (byte)0x1D, (byte)0x1E, (byte)0x1F,
        (byte)0x40, (byte)0x5A, (byte)0x7F, (byte)0x7B, (byte)0x5B, (byte)0x6C, (byte)0x50, (byte)0x7D, (byte)0x4D, (byte)0x5D, (byte)0x5C, (byte)0x4E, (byte)0x6B, (byte)0x60, (byte)0x4B, (byte)0x61,
        (byte)0xF0, (byte)0xF1, (byte)0xF2, (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7, (byte)0xF8, (byte)0xF9, (byte)0x7A, (byte)0x5E, (byte)0x4C, (byte)0x7E, (byte)0x6E, (byte)0x6F,
        (byte)0x7C, (byte)0xC1, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5, (byte)0xC6, (byte)0xC7, (byte)0xC8, (byte)0xC9, (byte)0xD1, (byte)0xD2, (byte)0xD3, (byte)0xD4, (byte)0xD5, (byte)0xD6,
        (byte)0xD7, (byte)0xD8, (byte)0xD9, (byte)0xE2, (byte)0xE3, (byte)0xE4, (byte)0xE5, (byte)0xE6, (byte)0xE7, (byte)0xE8, (byte)0xE9, (byte)0xAD, (byte)0xE0, (byte)0xBD, (byte)0x5F, (byte)0x6D,
        (byte)0x79, (byte)0x81, (byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89, (byte)0x91, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96,
        (byte)0x97, (byte)0x98, (byte)0x99, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7, (byte)0xA8, (byte)0xA9, (byte)0xC0, (byte)0x4F, (byte)0xD0, (byte)0xA1, (byte)0x07,
        (byte)0x20, (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x06, (byte)0x17, (byte)0x28, (byte)0x29, (byte)0x2A, (byte)0x2B, (byte)0x2C, (byte)0x09, (byte)0x0A, (byte)0x1B,
        (byte)0x30, (byte)0x31, (byte)0x1A, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x08, (byte)0x38, (byte)0x39, (byte)0x3A, (byte)0x3B, (byte)0x04, (byte)0x14, (byte)0x3E, (byte)0xFF,
        (byte)0x41, (byte)0xAA, (byte)0x4A, (byte)0xB1, (byte)0x9F, (byte)0xB2, (byte)0x6A, (byte)0xB5, (byte)0xBB, (byte)0xB4, (byte)0x9A, (byte)0x8A, (byte)0xB0, (byte)0xCA, (byte)0xAF, (byte)0xBC,
        (byte)0x90, (byte)0x8F, (byte)0xEA, (byte)0xFA, (byte)0xBE, (byte)0xA0, (byte)0xB6, (byte)0xB3, (byte)0x9D, (byte)0xDA, (byte)0x9B, (byte)0x8B, (byte)0xB7, (byte)0xB8, (byte)0xB9, (byte)0xAB,
        (byte)0x64, (byte)0x65, (byte)0x62, (byte)0x66, (byte)0x63, (byte)0x67, (byte)0x9E, (byte)0x68, (byte)0x74, (byte)0x71, (byte)0x72, (byte)0x73, (byte)0x78, (byte)0x75, (byte)0x76, (byte)0x77,
        (byte)0xAC, (byte)0x69, (byte)0xED, (byte)0xEE, (byte)0xEB, (byte)0xEF, (byte)0xEC, (byte)0xBF, (byte)0x80, (byte)0xFD, (byte)0xFE, (byte)0xFB, (byte)0xFC, (byte)0xBA, (byte)0xAE, (byte)0x59,
        (byte)0x44, (byte)0x45, (byte)0x42, (byte)0x46, (byte)0x43, (byte)0x47, (byte)0x9C, (byte)0x48, (byte)0x54, (byte)0x51, (byte)0x52, (byte)0x53, (byte)0x58, (byte)0x55, (byte)0x56, (byte)0x57,
        (byte)0x8C, (byte)0x49, (byte)0xCD, (byte)0xCE, (byte)0xCB, (byte)0xCF, (byte)0xCC, (byte)0xE1, (byte)0x70, (byte)0xDD, (byte)0xDE, (byte)0xDB, (byte)0xDC, (byte)0x8D, (byte)0x8E, (byte)0xDF
    };
}