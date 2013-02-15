/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.persistence;

import jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder;
import jviewer.objectdomain.Client;
import jviewer.objectdomain.eClientInfo;
import jviewer.util.Holder;
import jviewer.util.logging.Logging;
import jviewer.util.utils;
import org.omg.CORBA.StringHolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrey
 */
public class ClientDBBroker implements ClientBroker {

    private Logging log = new Logging(this.getClass());
    private static Connection connection;

    private Connection getConnRef() {
        try {
            if (connection == null || connection.isClosed()) {
                return connection =  JDBCUtils.getConnection(getClass().toString());
            } else {
                return connection;
            }
        } catch (SQLException ex) {
            log.error("Error occurred while getting DB connection: " + ex.getLocalizedMessage(), ex);
        }
        return null;
    }

    @Override
    public String[] getHistory(int code) {
        return getClientHistory(code);
    }

    @Override
    public boolean login(String user, String password) {

        return login(user, password, new ClientInfoHolder(), new StringHolder());
    }

    @Override
    public boolean logout(int clientId) {
        return ClientLogout(clientId, new Holder<String>(null));
    }

    @Override
    public boolean getClient() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration<Client> getClients() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean ClientLogout(int client_id, Holder<String> error) {
        Connection conn = getConnRef();
        CallableStatement cs = null;
        int ret;
        try {
            cs = conn.prepareCall("{? = call ClientUtils.ClientLogout(?,?)}");
            cs.setInt(2, client_id);
            cs.registerOutParameter(1, oracle.jdbc.OracleTypes.INTEGER);
            cs.registerOutParameter(3, oracle.jdbc.OracleTypes.VARCHAR);
            cs.execute();
            
            ret = cs.getInt(1);
            error.value = cs.getString(3);
            return ret==1;
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            try {
                cs.close();
            } catch (SQLException ex1) {
                log.error(ex1.getMessage(), ex1);
            }
        }
        return false;
    }

    @Override
    public boolean login(String login, String password, ClientInfoHolder clientInfoArr,
            StringHolder strResult) {
        HashMap<eClientInfo, String> clientInfo;
        // "=" - hardcoded :(
        clientInfo = utils.toHashMap(clientInfoArr.value, "=");
        Connection conn = getConnRef();
        CallableStatement cs = null;
        int clientId;
        try {
            String server_name = clientInfo.get(eClientInfo.server_name);
            String client_ip = clientInfo.get(eClientInfo.client_ip);
            String application_id = clientInfo.get(eClientInfo.application_id);
            String app_version = clientInfo.get(eClientInfo.app_version);
            String app_info = clientInfo.get(eClientInfo.app_info);

            log.info("Call DB with following params: "
                    + "\nserver_name=" + server_name
                    + "\nclient_ip=" + client_ip
                    + "\napplication_id=" + application_id
                    + "\napp_version=" + app_version
                    + "\napp_info=" + app_info);

            int sessionId;

            cs = conn.prepareCall("{? = call ClientUtils.ClientLogin(?,?,?,?,?,?,?,?,?,?)}");

            cs.setString(2, login);
            cs.setString(3, password);
            cs.setString(4, server_name);
            cs.setString(5, client_ip);
            cs.setString(6, application_id);
            cs.setString(7, app_version);
            cs.setString(8, app_info);

            cs.registerOutParameter(1, oracle.jdbc.OracleTypes.INTEGER);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.registerOutParameter(10, Types.INTEGER); // session_id
            cs.registerOutParameter(11, Types.INTEGER); // client_id

            cs.execute();

            int ret = cs.getInt(1);
            String loginResult = cs.getString(9);
            strResult.value = loginResult;
            if (ret == 1) {
                sessionId = cs.getInt(10);
                clientId = cs.getInt(11);
                clientInfo.put(eClientInfo.session_id, Integer.toString(sessionId));
                clientInfo.put(eClientInfo.login_result, loginResult);  // при успехе в принципе не нужен
                clientInfo.put(eClientInfo.client_id, String.valueOf(clientId));
                // в случае успеха тут должна добавиться информация о номере сессии
                clientInfoArr.value = utils.mapToString(clientInfo, "=");
                log.info("Login successful for client: " + login
                        + ". Session_id=" + sessionId);
                return true;
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            try {
                cs.close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        } finally {// тут цикл  и контроль в другом потоке!
            try {
                cs.close();
            } catch (SQLException ex1) {
                log.error(ex1.getMessage(), ex1);
            }
        }

        log.info("Logon denied for login: " + login + ". Reason: " + strResult.value);
        return false;
    }

    private String[] getClientHistory(int code) {
        Connection conn = getConnRef();
        CallableStatement cs = null;
        try {
            log.info("Call DB with following params: "
                    + "\nclient_id=" + code);
            cs = conn.prepareCall("{? = call ClientUtils.GetClientHistory(?)}");
            cs.setInt(2, code);
            cs.registerOutParameter(1, oracle.jdbc.OracleTypes.CURSOR);

            cs.executeQuery();

            ResultSet rs = (ResultSet) cs.getObject(1);
            ArrayList<String> strArr = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();

            // добавляем имена колонок
            String d = ", ";
            String columnNames = md.getColumnLabel(1) + d + md.getColumnLabel(2) + d + md.getColumnLabel(3) + d
                    + md.getColumnLabel(4) + d + md.getColumnLabel(5) + d + md.getColumnLabel(6);
            strArr.add(columnNames);
            while (rs.next()) {
                int id = rs.getInt(1);
                String position = rs.getString(2);
                String manager = rs.getString(3);
                java.util.Date hire_date = rs.getTimestamp(4);
                java.util.Date dismiss_date = rs.getTimestamp(5);
                String employee_id = rs.getString(6);

                String strDismissDate = "";
                if (dismiss_date != null) {
                    strDismissDate = dismiss_date.toString();
                }
                String row = id + d + position + d + manager
                        + d + hire_date.toString() + d + strDismissDate + d
                        + employee_id;
                strArr.add(row);
            }
            
            String[] resArr = new String[strArr.size()];
            strArr.toArray(resArr);
            log.info("Returing following info to client: \n");
            for (String str : strArr) {
                log.info(str);
            }
            return resArr;
        } catch (SQLException ex) {
            Logger.getLogger(ClientDBBroker.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                cs.close();
            } catch (SQLException ex) {
                log.error(ex);
            }
        }
        return null;
    }
}