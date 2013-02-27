/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.dbserver;

import jviewer.controller.orbHistory.HistoryExtendedPOA;
import jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder;
import jviewer.util.logging.Logging;
import jviewer.util.persistence.ClientBroker;
import jviewer.util.persistence.ClientDBBroker;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StringHolder;

import java.util.Arrays;

/**
 *
 * @author Andrey
 */
public class PersistentHistoryServant extends HistoryExtendedPOA {
    
    private ORB orb;
    private static ClientBroker clientDbBroker;
    private static Logging log = new Logging(PersistentHistoryServant.class);
    // м.б. еще и тут хранить информацию обо всех открытых сессиях?
    // но постоянного выделенного соединения у базы с апп. сервером нет.. 
    // и как тогда гарантировать консистентность статуса клиента в таблице sessions?? - 
    // видимо нужно тут session bean использовать этот объект один для всех

    public PersistentHistoryServant(ORB orb) {
        this.orb = orb;
        clientDbBroker = new ClientDBBroker();
        log.info("PersistentHistoryServant instance ir ready to serve connections");
    }


    @Override
    public void shutdown() {
        orb.shutdown(false);
    }

    @Override
    public String[] getHistory(int clientId) {
        return clientDbBroker.getHistory(clientId);
    }

    @Override
    public boolean login(String login, String password) {
        log.info("DB Server got login request for " + login + " Short handshake protocol.");
        return clientDbBroker.login(login, password);
    }

    @Override
    public boolean logout(int clientId) {
        log.info("Got logout request from clientId=" + clientId);
        return clientDbBroker.logout(clientId);
        //return clientDbBroker.ClientLogout(clientId, new Holder<String>(null));
    }

    @Override
    public boolean loginExt(String login, String password, ClientInfoHolder clientInfo, StringHolder strResult) {
        // here we should cast array to HashMap ... or create another method in dbBroker...
        log.info("DB Server got login request for " + login + ". Client info: " + Arrays.toString(clientInfo.value) );

        
        return clientDbBroker.login(login, password, clientInfo, strResult);
        //return clientDbBroker.login(login, password);
    }
}
