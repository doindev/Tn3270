package com.tn3270;

import java.io.*;
import java.util.logging.Logger;

public class TelnetStream implements Closeable {
    private static final Logger logger = Logger.getLogger(TelnetStream.class.getName());
    
    private static final byte IAC = (byte) 0xFF;
    private static final byte WILL = (byte) 0xFB;
    private static final byte WONT = (byte) 0xFC;
    private static final byte DO = (byte) 0xFD;
    private static final byte DONT = (byte) 0xFE;
    private static final byte SB = (byte) 0xFA;
    private static final byte SE = (byte) 0xF0;
    private static final byte NOP = (byte) 0xF1;
    private static final byte GA = (byte) 0xF9;
    private static final byte EOR = (byte) 0xEF;
    
    private static final byte TELOPT_BINARY = 0x00;
    private static final byte TELOPT_ECHO = 0x01;
    private static final byte TELOPT_SGA = 0x03;
    private static final byte TELOPT_STATUS = 0x05;
    private static final byte TELOPT_TIMING = 0x06;
    private static final byte TELOPT_TTYPE = 0x18;
    private static final byte TELOPT_EOR = 0x19;
    private static final byte TELOPT_TSPEED = 0x20;
    private static final byte TELOPT_LINEMODE = 0x22;
    private static final byte TELOPT_XDISPLOC = 0x23;
    private static final byte TELOPT_ENVIRON = 0x24;
    private static final byte TELOPT_AUTH = 0x25;
    private static final byte TELOPT_NEW_ENVIRON = 0x27;
    private static final byte TELOPT_TN3270E = 0x28;
    
    private static final byte IS = 0x00;
    private static final byte SEND = 0x01;
    private static final byte INFO = 0x02;
    
    private static final byte TN3270E_ASSOCIATE = 0x00;
    private static final byte TN3270E_CONNECT = 0x01;
    private static final byte TN3270E_DEVICE_TYPE = 0x02;
    private static final byte TN3270E_FUNCTIONS = 0x03;
    private static final byte TN3270E_IS = 0x04;
    private static final byte TN3270E_REASON = 0x05;
    private static final byte TN3270E_REJECT = 0x06;
    private static final byte TN3270E_REQUEST = 0x07;
    private static final byte TN3270E_SEND = 0x08;
    
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final TerminalType terminalType;
    
    private boolean[] localOptions = new boolean[256];
    private boolean[] remoteOptions = new boolean[256];
    private boolean tn3270eMode = false;
    private boolean binaryMode = false;
    private boolean eorMode = false;
    
    private final ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
    
    public TelnetStream(InputStream inputStream, OutputStream outputStream, TerminalType terminalType) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.terminalType = terminalType;
        initializeNegotiation();
    }
    
    private void initializeNegotiation() {
        try {
            sendWill(TELOPT_TTYPE);
            sendDo(TELOPT_TN3270E);
            sendDo(TELOPT_BINARY);
            sendDo(TELOPT_EOR);
            sendDo(TELOPT_SGA);
            sendWont(TELOPT_ECHO);
            sendWont(TELOPT_LINEMODE);
            outputStream.flush();
        } catch (IOException e) {
            logger.warning("Failed to initialize telnet negotiation: " + e.getMessage());
        }
    }
    
    public byte[] receive() throws IOException {
        dataBuffer.reset();
        boolean inCommand = false;
        boolean inSubnegotiation = false;
        ByteArrayOutputStream subBuffer = new ByteArrayOutputStream();
        
        while (true) {
            int b = inputStream.read();
            if (b == -1) {
                throw new IOException("Connection closed");
            }
            
            byte data = (byte) b;
            
            if (data == IAC) {
                int next = inputStream.read();
                if (next == -1) {
                    throw new IOException("Connection closed");
                }
                
                byte command = (byte) next;
                
                if (command == IAC) {
                    dataBuffer.write(IAC);
                } else if (command == EOR) {
                    break;
                } else if (command == GA) {
                    break;
                } else if (command == SB) {
                    inSubnegotiation = true;
                    subBuffer.reset();
                } else if (command == SE) {
                    if (inSubnegotiation) {
                        processSubnegotiation(subBuffer.toByteArray());
                        inSubnegotiation = false;
                    }
                } else if (command == WILL || command == WONT || command == DO || command == DONT) {
                    int option = inputStream.read();
                    if (option == -1) {
                        throw new IOException("Connection closed");
                    }
                    processOption(command, (byte) option);
                } else if (command == NOP) {
                    continue;
                }
            } else if (inSubnegotiation) {
                subBuffer.write(data);
            } else {
                dataBuffer.write(data);
                
                if (!eorMode && !binaryMode && data == 0x0A) {
                    break;
                }
            }
            
            if (inputStream.available() == 0 && dataBuffer.size() > 0 && !eorMode) {
                break;
            }
        }
        
        return dataBuffer.toByteArray();
    }
    
    public void send(byte[] data) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        for (byte b : data) {
            if (b == IAC) {
                output.write(IAC);
                output.write(IAC);
            } else {
                output.write(b);
            }
        }
        
        if (eorMode) {
            output.write(IAC);
            output.write(EOR);
        }
        
        outputStream.write(output.toByteArray());
        outputStream.flush();
    }
    
    private void processOption(byte command, byte option) throws IOException {
        logger.fine(String.format("Telnet option: %s %s", commandToString(command), optionToString(option)));
        
        switch (command) {
            case WILL:
                handleWill(option);
                break;
            case WONT:
                handleWont(option);
                break;
            case DO:
                handleDo(option);
                break;
            case DONT:
                handleDont(option);
                break;
        }
    }
    
    private void handleWill(byte option) throws IOException {
        if (!remoteOptions[option & 0xFF]) {
            remoteOptions[option & 0xFF] = true;
            
            switch (option) {
                case TELOPT_BINARY:
                case TELOPT_EOR:
                case TELOPT_SGA:
                case TELOPT_TN3270E:
                    sendDo(option);
                    break;
                case TELOPT_TTYPE:
                    sendDo(option);
                    sendTerminalType();
                    break;
                default:
                    sendDont(option);
                    break;
            }
        }
    }
    
    private void handleWont(byte option) throws IOException {
        if (remoteOptions[option & 0xFF]) {
            remoteOptions[option & 0xFF] = false;
            sendDont(option);
        }
    }
    
    private void handleDo(byte option) throws IOException {
        if (!localOptions[option & 0xFF]) {
            localOptions[option & 0xFF] = true;
            
            switch (option) {
                case TELOPT_BINARY:
                    binaryMode = true;
                    sendWill(option);
                    break;
                case TELOPT_EOR:
                    eorMode = true;
                    sendWill(option);
                    break;
                case TELOPT_SGA:
                    sendWill(option);
                    break;
                case TELOPT_TTYPE:
                    sendWill(option);
                    break;
                case TELOPT_TN3270E:
                    tn3270eMode = true;
                    sendWill(option);
                    break;
                default:
                    sendWont(option);
                    localOptions[option & 0xFF] = false;
                    break;
            }
        }
    }
    
    private void handleDont(byte option) throws IOException {
        if (localOptions[option & 0xFF]) {
            localOptions[option & 0xFF] = false;
            sendWont(option);
            
            if (option == TELOPT_BINARY) {
                binaryMode = false;
            } else if (option == TELOPT_EOR) {
                eorMode = false;
            } else if (option == TELOPT_TN3270E) {
                tn3270eMode = false;
            }
        }
    }
    
    private void processSubnegotiation(byte[] data) throws IOException {
        if (data.length < 1) return;
        
        byte option = data[0];
        
        switch (option) {
            case TELOPT_TTYPE:
                if (data.length > 1 && data[1] == SEND) {
                    sendTerminalType();
                }
                break;
                
            case TELOPT_TN3270E:
                processTn3270eSubnegotiation(data);
                break;
        }
    }
    
    private void processTn3270eSubnegotiation(byte[] data) throws IOException {
        if (data.length < 2) return;
        
        byte command = data[1];
        
        switch (command) {
            case TN3270E_SEND:
                if (data.length > 2 && data[2] == TN3270E_DEVICE_TYPE) {
                    sendTn3270eDeviceType();
                }
                break;
                
            case TN3270E_DEVICE_TYPE:
                if (data.length > 3 && data[2] == TN3270E_REQUEST) {
                    sendTn3270eDeviceType();
                }
                break;
                
            case TN3270E_FUNCTIONS:
                if (data.length > 2 && data[2] == TN3270E_REQUEST) {
                    sendTn3270eFunctions();
                }
                break;
        }
    }
    
    private void sendTerminalType() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(IAC);
        output.write(SB);
        output.write(TELOPT_TTYPE);
        output.write(IS);
        output.write(terminalType.getNameBytes());
        output.write(IAC);
        output.write(SE);
        
        outputStream.write(output.toByteArray());
        outputStream.flush();
        
        logger.info("Sent terminal type: " + terminalType.getName());
    }
    
    private void sendTn3270eDeviceType() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(IAC);
        output.write(SB);
        output.write(TELOPT_TN3270E);
        output.write(TN3270E_DEVICE_TYPE);
        output.write(TN3270E_IS);
        output.write(terminalType.getNameBytes());
        output.write(IAC);
        output.write(SE);
        
        outputStream.write(output.toByteArray());
        outputStream.flush();
        
        logger.info("Sent TN3270E device type: " + terminalType.getName());
    }
    
    private void sendTn3270eFunctions() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(IAC);
        output.write(SB);
        output.write(TELOPT_TN3270E);
        output.write(TN3270E_FUNCTIONS);
        output.write(TN3270E_IS);
        output.write(IAC);
        output.write(SE);
        
        outputStream.write(output.toByteArray());
        outputStream.flush();
    }
    
    private void sendWill(byte option) throws IOException {
        outputStream.write(new byte[]{IAC, WILL, option});
    }
    
    private void sendWont(byte option) throws IOException {
        outputStream.write(new byte[]{IAC, WONT, option});
    }
    
    private void sendDo(byte option) throws IOException {
        outputStream.write(new byte[]{IAC, DO, option});
    }
    
    private void sendDont(byte option) throws IOException {
        outputStream.write(new byte[]{IAC, DONT, option});
    }
    
    private String commandToString(byte command) {
        switch (command) {
            case WILL: return "WILL";
            case WONT: return "WONT";
            case DO: return "DO";
            case DONT: return "DONT";
            default: return String.format("0x%02X", command);
        }
    }
    
    private String optionToString(byte option) {
        switch (option) {
            case TELOPT_BINARY: return "BINARY";
            case TELOPT_ECHO: return "ECHO";
            case TELOPT_SGA: return "SGA";
            case TELOPT_TTYPE: return "TERMINAL-TYPE";
            case TELOPT_EOR: return "END-OF-RECORD";
            case TELOPT_TN3270E: return "TN3270E";
            default: return String.format("0x%02X", option);
        }
    }
    
    public boolean isTn3270eMode() {
        return tn3270eMode;
    }
    
    public boolean isBinaryMode() {
        return binaryMode;
    }
    
    public boolean isEorMode() {
        return eorMode;
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
        outputStream.close();
    }
}