/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.appserver;

import jviewer.ejb.ApplicationLocal;
import jviewer.util.logging.Logging;
import jviewer.util.net.ConnectionListener;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author Andrey
 */
public class ServerConnectionListener extends ConnectionListener { 

    private Logging log = new Logging(getClass());
    private static ApplicationLocal app;

    public ServerConnectionListener(String bindAddress, int port) {
        super(bindAddress, port);
        try {
            app = getApplicationRef();
        } catch (NamingException ex) {
            log.error("Exception while initializing EJB \"Application\": " + ex.getMessage(), ex);
        }

    }

    @Override
    protected void onNewConnection(Socket s) throws IOException {

        log.info("Creating dedicated server process ...");
        ClientSession session = new ClientSession(s);
        app.addClientSession(session);
        session.start();
    }

    private ApplicationLocal getApplicationRef() throws NamingException {
            Context c = new InitialContext();
            app = (ApplicationLocal) c.lookup("java:global/JeViewerEnterprise-ejb/Application!jviewer.ejb.ApplicationLocal");
            return app;
    }
    
}
