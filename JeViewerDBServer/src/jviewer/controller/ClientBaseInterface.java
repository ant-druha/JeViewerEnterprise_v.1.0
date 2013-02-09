/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.controller;

import jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder;
import jviewer.util.Holder;
import org.omg.CORBA.StringHolder;

/**
 *
 * @author Andrey
 */
public interface ClientBaseInterface {
    
    public boolean ClientLogout(int client_id, Holder<String> error);
    
    /**
     *
     * @param login
     * @param password
     * @param clientInfo
     * @return
     */
    public boolean login(final String login, final String password, 
            ClientInfoHolder clientInfo, StringHolder strResult);
    
}
