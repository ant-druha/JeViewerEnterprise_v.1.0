/*
 * Copyright (c) 2013. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.ejb;

import java.util.Properties;
import javax.ejb.Remote;

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
