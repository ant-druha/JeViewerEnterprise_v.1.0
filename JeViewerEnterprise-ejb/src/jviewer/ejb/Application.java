/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.ejb;

import com.sun.istack.internal.Nullable;
import jviewer.appserver.ClientSession;
import jviewer.appserver.ServerConnectionListener;
import jviewer.controller.orbHistory.HistoryExtended;
import jviewer.controller.orbHistory.HistoryExtendedHelper;
import jviewer.util.config.ConfigProperties;
import jviewer.util.logging.Logging;
import jviewer.util.net.ConnectionListener;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

/**
 * @author Andrey
 */

@SuppressWarnings("FeatureEnvy")
@Singleton
public class Application implements ApplicationRemote, ApplicationLocal {

    //region Data
    // Business logic below.
    private ConfigProperties configProperties;
    private static final Logging log = new Logging(Application.class);
    private ConnectionListener serverListener;
    // TODO: find another holder for session therads !
    private final Vector<ClientSession> clientSessions = new Vector<>();
    private HistoryExtended history = null;
    //endregion
    private void loadProperties() {

        FileReader fr = null;
        try {
            Properties props = new Properties();

            // TODO: add file location parameter to application.xml
            File f1 = new File("C:\\Users\\Andrey\\IdeaProjects\\JeViewerEnterprise_v.1.0\\JeViewerEnterprise.properties");
            //File f2 = new File("C:\\Users\\Andrey\\Documents\\Dropbox\\Edu\\4_term\\JavaLabs\\Curs\\JeViewerEnterprise\\JeViewerEnterprise.properties");
            File f2 = new File("C:\\Users\\Andrey.Dernov\\IdeaProjects\\JeViewerEnterprise_v.1.0\\JeViewerEnterprise.properties");
            //File proprsFile = new File(getClass().getResource("JeViewerEnterprise.properties").getPath());
            File proprsFile = f1.exists() ? f1 : f2;

            fr = new FileReader(proprsFile);

            //PropertyResourceBundle
            props.load(fr);

            configProperties = new ConfigProperties(props);
            configProperties.setProperty("frontendHost", InetAddress.getLocalHost().getHostAddress());

            startServerListener();

        } catch (IOException e) {
            log.error("IO Exception while closing file" + e.getMessage());
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                log.error("Exception while closing file" + ex.getMessage());
            }
        }
    }

    @Override
    public Properties getConfigProperties() {
        return configProperties.getProperties();
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public String getConfigProperty(final String key) {
        return configProperties.getProperty(key);
    }

    /**
     * Application initialization
     */
    @PostConstruct
    private void init() {

        loadProperties();

    }

    @PreDestroy
    private void onDestroy() {
        log.warn("Application is shutting down ...");

        log.info("Shutting down server listener ...");
        stopServerListener();

        log.info("Closing all clients sessions ...");
        closeSessions();
        log.info("All sessions were closed.");

        log.info("Shutting down remote objects ...");
        closeRemoteObjects();
    }

    private void stopServerListener() {
        try {
            serverListener.stopListen();
        } catch (IOException ex) {
            log.error("Failed to stop server listener: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void reloadConfigProperties() {
        loadProperties();
    }

    private void startServerListener() {
        if (serverListener == null) {
            int serverListenPort = Integer.parseInt(configProperties.getProperty("frontendPort"));
            String serverListenHost = configProperties.getProperty("frontendHost");
            serverListener = new ServerConnectionListener(serverListenHost, serverListenPort);
            serverListener.start();
        }
    }

    @Override
    public void setConfigProperty(String key, String value) {
        configProperties.setProperty(key, value);
    }

    @Override
    public boolean addClientSession(ClientSession session) {
        if (clientSessions.add(session)) {
            log.info("New client session created. Current sessions: " +
                    String.valueOf(ClientSession.getClientsOnline()));
            return true;
        }
        return false;
    }

    @Override
    public boolean removeClientSession(ClientSession session) {

        if (history == null) {
            try {
                history = getHistoryExtendedRef();
            } catch (Exception ex) {
                log.error("Exception while obtaining ref to CORBA object \"HistoryExtended\": "
                    + ex.getMessage(), ex);
            }
        }

        try {
            log.info("Closing client session: " + session.getSessionId());
            if (session.isConnected()) {
                // если клиент не закрыл соединение - делаем это
                log.info("Client's session was not closed correctly. Call logout for client ...");
                if (history.logout(session.getClientId())) {
                    session.setToClose(true);
                } else {// иначе ... пока хз чего делать
                    log.warn("Could not colse client session in DB");
                }
            }
            session.close();
        } catch (IOException ex) {
            log.error("Exception while closing client session: " + ex.getMessage(), ex);
        }
        if (clientSessions.remove(session)) {
            log.info("Session closed. Current sessions count: " + ClientSession.getClientsOnline());
            return true;
        }
        return false;

    }

    private void closeSessions() {
        Iterator<ClientSession> it = clientSessions.iterator();
        while (it.hasNext()) {
            removeClientSession(it.next());
        }
        clientSessions.clear();
    }

    private void closeRemoteObjects() {
        if (history != null) {
            history.shutdown();
            //history = getHistoryExtendedRef();
        }

    }

    //  tocheck: how it works? :)
    @Nullable
    private HistoryExtended getHistoryExtendedRef() throws Exception {
            // Step 1: Instantiate the ORB
            String[] args = null;
            //args = {" -ORBInitialPort", " 1050", " -ORBInitialHost", " 192.168.0.50"};
            //Properties props = getConfigProperties();
            ORB orb = ORB.init(args, configProperties.getProperties());
            // get the root naming context
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            // Use NamingContextExt instead of NamingContext. This is 
            // part of the Interoperable naming Service.  
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // resolve the Object Reference in Naming 
            String name = "History";
            history = HistoryExtendedHelper.narrow(ncRef.resolve_str(name));
            log.info("Obtained a handle on server object: " + history);
            return history;
    }
}
