package com.tn3270;

public class Field {
    private final int start;
    private final int length;
    private final byte attribute;
    private boolean modified;
    
    public Field(int start, int length, byte attribute) {
        this.start = start;
        this.length = length;
        this.attribute = attribute;
        this.modified = false;
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return start + length - 1;
    }
    
    public int getLength() {
        return length;
    }
    
    public byte getAttribute() {
        return attribute;
    }
    
    public boolean isProtected() {
        return (attribute & 0x20) != 0;
    }
    
    public boolean isNumeric() {
        return (attribute & 0x10) != 0;
    }
    
    public boolean isHidden() {
        return (attribute & 0x0C) == 0x0C;
    }
    
    public boolean isIntensified() {
        return (attribute & 0x08) != 0;
    }
    
    public boolean isModified() {
        return modified || (attribute & 0x01) != 0;
    }
    
    public void setModified(boolean modified) {
        this.modified = modified;
    }
    
    public boolean contains(int position) {
        return position >= start && position < start + length;
    }
    
    @Override
    public String toString() {
        return String.format("Field[start=%d, length=%d, protected=%b, numeric=%b, hidden=%b]",
                start, length, isProtected(), isNumeric(), isHidden());
    }
}