/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.net;

import jviewer.util.logging.Logging;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author Andrey
 */
public abstract class SocketDataReaderThread extends Thread {
    
    protected static final Logging log = new Logging(SocketDataReaderThread.class);

    private long reconnectDelay = 5000; // 5 sec.

    private Socket socket;

    private InetAddress ip;
    private int port;
    private int readTimeout;

    private boolean isClosed;

    private long lastConnectTime;
    private long lastDisconnectTime;

    public SocketDataReaderThread(String host, int port) {

        try {
            ip = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            log.error("Can not determine host address for " + host + ": " + e.getMessage());
        }
        setPort(port);

    }

    /**
     * @param host
     * @param port
     * @param readTimeout socket read timeout in milliseconds
     */
    public SocketDataReaderThread(String host, int port, int readTimeout) {

        try {
            ip = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            log.error("Can not determine host address for " + host + ": " + e.getMessage());
        }
        setPort(port);
        this.readTimeout = readTimeout;
    }

    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port > 0 && port < 65536) {
            this.port = port;
        } else
            throw new IllegalArgumentException("Port number is out of range: port=" + port);
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public void close() throws IOException {
        isClosed = true;
        disconnect();
    }

    public void setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public void run() {
        while (!isClosed()) {
            DataInputStream in = null;
            try {

                beforeConnect();

                if (ip != null) {
                    log.debug("Connecting to " + ip.getHostAddress() + ':' + port);
                    socket = new Socket(ip, port);
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(readTimeout);
                    in = new DataInputStream(socket.getInputStream());
                    log.debug(
                            "Connection to " + ip.getHostAddress() + ':' + port +
                                    " established. KeepAlive = " + socket.getKeepAlive()
                    );
                    lastConnectTime = System.currentTimeMillis();
                } else {
                    String msg = "Invalid connection parameters. Can not connect.";
                    log.error(msg);
                    throw new IllegalArgumentException(msg);
                }

            } catch (IOException e) {
                log.error("Can not connect to " + ip.getHostAddress() + ':' + port + " - " + e);
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e1) {
                    log.error(e1);
                }
                try {
                    sleep(reconnectDelay);
                } catch (InterruptedException e1) {
                    log.error(e1);
                }
                continue;
            } catch (Exception e) {
                try {
                    log.error(e);
                    sleep(reconnectDelay);
                } catch (InterruptedException e1) {
                    log.error(e1);
                }
                continue;
            }

            try {
                while (!isClosed()) {
                    readFromSocket(in);
                }
            } catch (IOException e) {
                lastDisconnectTime = System.currentTimeMillis();
                log.error("Socket read error " + ip.getHostAddress() + ":" + port + " - " + e);
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e1) {
                    log.error(e1);
                }
                onDisconnect();
                try {
                    sleep(reconnectDelay);
                } catch (InterruptedException e1) {
                    log.error(e1);
                }
            }
        }
    }

    protected void onDisconnect() {
    }

    protected void beforeConnect() {
    }

    protected abstract void readFromSocket(DataInputStream in) throws IOException;
    
}
