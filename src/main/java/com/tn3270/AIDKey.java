package com.tn3270;

public enum AIDKey {
    NO_AID((byte) 0x60),
    ENTER((byte) 0x7D),
    PF1((byte) 0xF1),
    PF2((byte) 0xF2),
    PF3((byte) 0xF3),
    PF4((byte) 0xF4),
    PF5((byte) 0xF5),
    PF6((byte) 0xF6),
    PF7((byte) 0xF7),
    PF8((byte) 0xF8),
    PF9((byte) 0xF9),
    PF10((byte) 0x7A),
    PF11((byte) 0x7B),
    PF12((byte) 0x7C),
    PF13((byte) 0xC1),
    PF14((byte) 0xC2),
    PF15((byte) 0xC3),
    PF16((byte) 0xC4),
    PF17((byte) 0xC5),
    PF18((byte) 0xC6),
    PF19((byte) 0xC7),
    PF20((byte) 0xC8),
    PF21((byte) 0xC9),
    PF22((byte) 0x4A),
    PF23((byte) 0x4B),
    PF24((byte) 0x4C),
    PA1((byte) 0x6C),
    PA2((byte) 0x6E),
    PA3((byte) 0x6B),
    CLEAR((byte) 0x6D),
    RESET((byte) 0x6F),
    ATTN((byte) 0xE0),
    SYSREQ((byte) 0xF0);
    
    private final byte code;
    
    AIDKey(byte code) {
        this.code = code;
    }
    
    public byte getCode() {
        return code;
    }
    
    public static AIDKey fromCode(byte code) {
        for (AIDKey aid : values()) {
            if (aid.code == code) {
                return aid;
            }
        }
        return NO_AID;
    }
}