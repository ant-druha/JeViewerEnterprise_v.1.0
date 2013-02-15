/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.net;

import jviewer.util.logging.Logging;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * @author Andrey
 */
public abstract class ConnectionListener extends Thread {

    private static final long reconnectDelay = 5000; // 5 seconds
    private int port;
    private String bindAddress;
    private ServerSocket socket;
    private boolean isStop;
    private Logging log = new Logging(ConnectionListener.class);

    protected ConnectionListener() {
        isStop = false;
    }

    /**
     * Creates new instance of tcp connection listener on the specified port
     *
     * @param port - tcp port listener should listen to
     */
    public ConnectionListener(int port) {
        this.port = port;
    }

    public ConnectionListener(String bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
    }

    @Override
    public void run() {
        while (!isStopped()) {
            log.info("Creating server socket at port " + port);
            try {
                socket = bindAddress == null
                        ? new ServerSocket(port, 1000)
                        : new ServerSocket(port, 1000, InetAddress.getByName(bindAddress));
            } catch (IOException ex) {
                log.error("Can not open server socket on " + bindAddress + ":" + port + ": " + ex.getMessage());
                try {
                    Thread.sleep(reconnectDelay);
                } catch (InterruptedException ex1) {
                    log.error(ex1.getMessage());
                }
                continue;
            }

            Socket clientSocket;
            try {
                while (!isStopped()) {
                    log.info("Waiting for client connection at port " + port + " ...");
                    clientSocket = socket.accept();
                    log.info("Got new client connection from "
                            + clientSocket.getInetAddress().getHostAddress() + " at port " + port);
                    try {
                        onNewConnection(clientSocket);
                    } catch (IOException ioEx) {
                        log.error("I/O Error while working with client connection" + ioEx);
                    }
                }
            } catch (SocketException e) {
                log.error("Socket seems to be closed " + e);
            } catch (IOException ioEx) {
                log.error("Can not accept client connection on port " + port + ": " + ioEx);
            }
        }
    }

    private boolean isStopped() {
        return interrupted() || isStop;
    }

    /**
     * This abstract method is called once a new tcp connection is accepted.
     *
     * @param s - new connection client socket
     */
    protected abstract void onNewConnection(Socket s) throws IOException;

    public void stopListen() throws IOException {
        if (isStarted()) {
            socket.close();
            socket = null;
        }
        isStop = true;
    }

    private boolean isStarted() {
        return socket != null;
    }
}
