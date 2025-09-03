package com.tn3270;

public enum TerminalType {
    IBM_3278_2("IBM-3278-2", 24, 80, false),
    IBM_3278_2_E("IBM-3278-2-E", 24, 80, true),
    IBM_3278_3("IBM-3278-3", 32, 80, false),
    IBM_3278_3_E("IBM-3278-3-E", 32, 80, true),
    IBM_3278_4("IBM-3278-4", 43, 80, false),
    IBM_3278_4_E("IBM-3278-4-E", 43, 80, true),
    IBM_3278_5("IBM-3278-5", 27, 132, false),
    IBM_3278_5_E("IBM-3278-5-E", 27, 132, true),
    IBM_3279_2("IBM-3279-2", 24, 80, false),
    IBM_3279_2_E("IBM-3279-2-E", 24, 80, true),
    IBM_3279_3("IBM-3279-3", 32, 80, false),
    IBM_3279_3_E("IBM-3279-3-E", 32, 80, true),
    IBM_3279_4("IBM-3279-4", 43, 80, false),
    IBM_3279_4_E("IBM-3279-4-E", 43, 80, true),
    IBM_3279_5("IBM-3279-5", 27, 132, false),
    IBM_3279_5_E("IBM-3279-5-E", 27, 132, true);
    
    private final String name;
    private final int rows;
    private final int cols;
    private final boolean extended;
    
    TerminalType(String name, int rows, int cols, boolean extended) {
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.extended = extended;
    }
    
    public String getName() {
        return name;
    }
    
    public int getRows() {
        return rows;
    }
    
    public int getCols() {
        return cols;
    }
    
    public boolean isExtended() {
        return extended;
    }
    
    public int getBufferSize() {
        return rows * cols;
    }
    
    public byte[] getNameBytes() {
        return name.getBytes();
    }
    
    public static TerminalType fromName(String name) {
        for (TerminalType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return IBM_3278_2_E;
    }
}