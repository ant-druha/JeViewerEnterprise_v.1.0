/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.logging;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrey
 */
public class Logging {
    
    private Class<?> sourseCalss;
    private String prefix;
    
    
    public Logging(Class sourseCalss) {
        this(sourseCalss,null);
    }
    
    public Logging(Class sourseCalss, String custPrefix) {
        this.sourseCalss = sourseCalss;
        prefix = custPrefix == null ?
                "[Jviewer log][" + this.sourseCalss.getName() +"]" :
                "[Jviewer log][" + this.sourseCalss.getName() + "-" + custPrefix +"]";
    }
    
    private String dateToString(Date date) {
        String dt = DateFormat.getDateInstance().format(date);
        String tm = DateFormat.getTimeInstance().format(date);
        String result = dt + ":" + tm;
        
        String timestamp = new java.sql.Timestamp(date.getTime()).toString();
        
        return timestamp;
    }
    
    public void info(String msg) {
        info(msg, null);
    }
    
    public void info(String msg, Throwable e) {        
        Logger.getLogger(sourseCalss.getName()).log(Level.INFO, dateToString(new Date()) + prefix + msg, e);
    }
    
    public void warn(String msg) {
        warn(msg, null);
    }
    
    public void warn(String msg, Throwable e) {
        Logger.getLogger(sourseCalss.getName()).log(Level.WARNING, dateToString(new Date()) + prefix + msg, e);
    }
    
    public void error(String msg) {
        error(msg,null);
    }
    
    public void error(Throwable e) {
        error(null,e);
    }
    
    public void error(String msg, Throwable e) {
        Logger.getLogger(sourseCalss.getName()).log(Level.SEVERE, dateToString(new Date()) + prefix + msg, e);
    }

    public void debug(String msg) {
        Logger.getLogger(sourseCalss.getName()).log(Level.FINEST, dateToString(new Date()) + prefix + msg);
    }
    
    
    
}
