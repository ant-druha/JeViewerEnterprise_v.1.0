/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.persistence;

import jviewer.controller.ClientBaseInterface;
import jviewer.controller.orbHistory.HistoryOperations;
import jviewer.objectdomain.Client;

import java.util.Enumeration;

/**
 *
 * @author Andrey
 */
public interface ClientBroker extends HistoryOperations, ClientBaseInterface {
    
    public boolean getClient();
    
    public Enumeration<Client> getClients();
}
