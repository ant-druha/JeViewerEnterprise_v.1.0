/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.jeviwerclient;

import jviewer.jeviwerclient.gui.ClientForm;
import jviewer.util.logging.Logging;

/**
 *
 * @author Andrey
 */
public class JeViwerClient {
    
    private static Logging log = new Logging(JeViwerClient.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        log.info("Starting Client form");
        new ClientForm().setVisible(true);
        
    }
}
