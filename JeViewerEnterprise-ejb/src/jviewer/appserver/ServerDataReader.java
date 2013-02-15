/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.appserver;

import jviewer.util.net.SocketDataReaderThread;

import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author Andrey
 */
public class ServerDataReader extends SocketDataReaderThread {

    public ServerDataReader(String host, int port) {
        super(host, port);
    }

    public ServerDataReader(String host, int port, int readTimeout) {
        super(host, port, readTimeout);
    }
    
    @Override
    public void onDisconnect() {
        
    }
    
    @Override 
    public void beforeConnect() {
        
    }

    @Override
    public void readFromSocket(DataInputStream in) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
