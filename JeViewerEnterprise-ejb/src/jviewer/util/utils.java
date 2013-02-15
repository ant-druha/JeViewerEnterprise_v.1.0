/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util;

import jviewer.objectdomain.eClientInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Andrey
 */
public class utils {

    public static HashMap<eClientInfo, String> toHashMap(String[] arrString, String delimeter) {

        HashMap<eClientInfo, String> map = ClientItemsMap.items();
        HashMap<eClientInfo, String> newMap = new HashMap<>();

        for (String string : arrString) {
            String[] token = string.split(delimeter);
            if (token.length > 1) {

                for (Map.Entry<eClientInfo, String> entry : map.entrySet()) {

                    if (entry.getValue().equals(token[0])) {
                        // если токен после = совпадает c определенным нами - заносим его в newMap
                        // TODO: Избавиться от констант!!! (token[1]...)
                        newMap.put(entry.getKey(), token[1]);
                        continue;
                    }
                }
            }
        }
        return newMap;
    }
//Client info: app_info=jviewer.controller.ClientFormController,  app_version=1, client_ip=

//Array example:    app_info=jviewer.controller.ClientFormController
//                  app_version=1
//                  client_ip=
    public static String[] mapToString(HashMap<eClientInfo, String> clientMap, String delimeter) {

        ArrayList<String> strArrList = new ArrayList<>();
        HashMap<eClientInfo, String> map = ClientItemsMap.items();
//        clientInfo.put(eClientInfo.server_name, "server_name");
//        clientInfo.put(eClientInfo.client_ip, "client_ip");
//        clientInfo.put(eClientInfo.application_id, "application_id");
//        clientInfo.put(eClientInfo.app_version, "app_version");
//        clientInfo.put(eClientInfo.session_id, "session_id");
//        clientInfo.put(eClientInfo.login_result, "login_result");        
        
        for (Map.Entry<eClientInfo, String> entry : clientMap.entrySet()) {
            String pairLeft = map.get(entry.getKey());
            String pairRight = entry.getValue();
            String line = pairLeft + delimeter + pairRight;
            strArrList.add(line);
            
        }
        String[] strResult= new String[strArrList.size()];  
        strArrList.toArray(strResult);
        
        return strResult;
    }
}
