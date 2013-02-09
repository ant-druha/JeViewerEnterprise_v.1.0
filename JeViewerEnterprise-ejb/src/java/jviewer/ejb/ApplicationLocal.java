/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.ejb;

import javax.ejb.Local;
import jviewer.appserver.ClientSession;

/**
 *
 * @author Andrey
 */
@Local
public interface ApplicationLocal extends ApplicationRemote {
    
    public boolean addClientSession(ClientSession session);
    public boolean removeClientSession(ClientSession session);
    
}
