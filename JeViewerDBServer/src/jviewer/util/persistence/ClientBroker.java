/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.persistence;

import java.util.Enumeration;
import jviewer.controller.ClientBaseInterface;
import jviewer.controller.orbHistory.HistoryOperations;
import jviewer.objectdomain.Client;

/**
 *
 * @author Andrey
 */
public interface ClientBroker extends HistoryOperations, ClientBaseInterface {
    
    public boolean getClient();
    
    public Enumeration<Client> getClients();
}
