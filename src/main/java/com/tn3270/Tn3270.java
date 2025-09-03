package com.tn3270;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class Tn3270 {
    private static final Logger logger = Logger.getLogger(Tn3270.class.getName());
    
    private String host;
    private int port = 23;
    private int sslPort = 992;
    private int connectTimeout = 10000;
    private int readTimeout = 30000;
    
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private TelnetStream telnetStream;
    
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean useSSL = new AtomicBoolean(true);
    
    private Screen screen;
    private TerminalType terminalType = TerminalType.IBM_3278_2_E;
    
    private Thread readerThread;
    
    public Tn3270() {
        this.screen = new Screen(this);
    }
    
    public Tn3270(String host) {
        this();
        this.host = host;
    }
    
    public Tn3270(String host, int port) {
        this(host);
        this.port = port;
    }
    
    public Tn3270 setHost(String host) {
        this.host = host;
        return this;
    }
    
    public Tn3270 setPort(int port) {
        this.port = port;
        return this;
    }
    
    public Tn3270 setSslPort(int sslPort) {
        this.sslPort = sslPort;
        return this;
    }
    
    public Tn3270 setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }
    
    public Tn3270 setReadTimeout(int timeout) {
        this.readTimeout = timeout;
        return this;
    }
    
    public Tn3270 setTerminalType(TerminalType type) {
        this.terminalType = type;
        return this;
    }
    
    public Tn3270 connect() throws IOException {
        if (connected.get()) {
            throw new IllegalStateException("Already connected");
        }
        
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host must be specified");
        }
        
        Exception lastException = null;
        
        if (useSSL.get()) {
            try {
                connectSSL();
                return this;
            } catch (SSLHandshakeException e) {
                logger.warning("SSL handshake failed, falling back to plain connection: " + e.getMessage());
                lastException = e;
                useSSL.set(false);
            } catch (IOException e) {
                lastException = e;
            }
        }
        
        try {
            connectPlain();
        } catch (IOException e) {
            if (lastException != null) {
                e.addSuppressed(lastException);
            }
            throw e;
        }
        
        return this;
    }
    
    private void connectSSL() throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket();
        
        sslSocket.connect(new InetSocketAddress(host, sslPort), connectTimeout);
        sslSocket.setSoTimeout(readTimeout);
        sslSocket.startHandshake();
        
        this.socket = sslSocket;
        setupStreams();
        startReaderThread();
        connected.set(true);
        
        logger.info("Connected to " + host + ":" + sslPort + " using SSL");
    }
    
    private void connectPlain() throws IOException {
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress(host, port), connectTimeout);
        plainSocket.setSoTimeout(readTimeout);
        
        this.socket = plainSocket;
        setupStreams();
        startReaderThread();
        connected.set(true);
        
        logger.info("Connected to " + host + ":" + port + " using plain connection");
    }
    
    private void setupStreams() throws IOException {
        this.inputStream = new BufferedInputStream(socket.getInputStream());
        this.outputStream = new BufferedOutputStream(socket.getOutputStream());
        this.telnetStream = new TelnetStream(inputStream, outputStream, terminalType);
    }
    
    private void startReaderThread() {
        readerThread = new Thread(new DataStreamReader(), "TN3270-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
    
    public Tn3270 disconnect() {
        if (!connected.compareAndSet(true, false)) {
            return this;
        }
        
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        closeQuietly(telnetStream);
        closeQuietly(inputStream);
        closeQuietly(outputStream);
        closeQuietly(socket);
        
        logger.info("Disconnected from " + host);
        return this;
    }
    
    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logger.fine("Error closing resource: " + e.getMessage());
            }
        }
    }
    
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
    
    public Screen getScreen() {
        return screen;
    }
    
    void sendData(byte[] data) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }
        telnetStream.send(data);
    }
    
    private class DataStreamReader implements Runnable {
        @Override
        public void run() {
            try {
                while (connected.get() && !Thread.currentThread().isInterrupted()) {
                    byte[] data = telnetStream.receive();
                    if (data != null && data.length > 0) {
                        screen.processIncomingData(data);
                    }
                }
            } catch (IOException e) {
                if (connected.get()) {
                    logger.severe("Connection error: " + e.getMessage());
                    disconnect();
                }
            }
        }
    }
}