/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.ejb;

import javax.ejb.Remote;
import java.util.Properties;

/**
 *
 * @author Andrey
 */
@Remote
public interface ApplicationRemote {

    public Properties getConfigProperties();

    public String getConfigProperty(final String key);

    public void reloadConfigProperties();
    
    public void setConfigProperty(final String key, final String value);

}
