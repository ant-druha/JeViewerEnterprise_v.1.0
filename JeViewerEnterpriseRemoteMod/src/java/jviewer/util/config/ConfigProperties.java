/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.config;


import com.sun.istack.internal.NotNull;

import java.util.Properties;

/**
 *
 * @author Andrey
 */
public class ConfigProperties {
    

    private final Properties properties;   
    
    public ConfigProperties(@NotNull Properties properties) {
        assert properties != null : "null properties";
        this.properties = properties;
    }
    
    public final String getProperty(final String key) {
        return properties.getProperty(key);
    }
    
    public Object setProperty(final String key, String property) {
    return properties.setProperty(key, property);
    }
    
    public Properties getProperties() {
        return properties;
    }
}
