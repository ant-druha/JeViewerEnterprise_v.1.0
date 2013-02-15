/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util;

import jviewer.objectdomain.eClientInfo;

import java.util.HashMap;

/**
 *
 * @author Andrey
 */
public class ClientItemsMap {
    
    private static final HashMap<eClientInfo, String> clientInfo = new HashMap<>();
//    server_name, client_ip, application_id, app_version, app_info, sessionId, login_result
    private ClientItemsMap() {}
    static {
        clientInfo.put(eClientInfo.server_name, "server_name");
        clientInfo.put(eClientInfo.client_ip, "client_ip");
        clientInfo.put(eClientInfo.client_id, "client_id");
        clientInfo.put(eClientInfo.application_id, "application_id");
        clientInfo.put(eClientInfo.app_version, "app_version");
        clientInfo.put(eClientInfo.app_info, "app_info");
        clientInfo.put(eClientInfo.session_id, "session_id");
        clientInfo.put(eClientInfo.login_result, "login_result");
    }
    

    public static HashMap<eClientInfo, String> items() {
        return clientInfo;
    }
    
    public static String get(eClientInfo key) {
        return clientInfo.get(key);
    }
}
