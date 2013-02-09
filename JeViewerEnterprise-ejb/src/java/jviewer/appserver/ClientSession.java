/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.appserver;

import jviewer.controller.orbHistory.HistoryExtended;
import jviewer.controller.orbHistory.HistoryExtendedHelper;
import jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder;
import jviewer.ejb.ApplicationLocal;
import jviewer.objectdomain.eClientInfo;
import jviewer.util.logging.Logging;
import jviewer.util.utils;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StringHolder;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author Andrey
 */
public class ClientSession extends Thread {

    //region Session Data
    //TODO: here should be unique sessionID not the session count => wrong thread name <name>-<sessisonCount>
    private static int clientSessionsCount;

    private int sessionId;
    private int clientId;
    private Socket socket;
    private boolean isSocketClosed;
    private boolean isConnected;
    private long reconnectDelay;
    private Logging log = new Logging(ClientSession.class, String.valueOf(clientSessionsCount));
    private HistoryExtended history;
    //    private int readTimeout;
    @EJB
    private static ApplicationLocal app;
    // client's protocol information
    String login, password;
    ClientInfoHolder clientInfoArr;
    StringHolder strResult;
    private DataInputStream in;
    private DataOutputStream out;
    // so-so /=
    String historyReply;
    //endregion

    public ClientSession(Socket clientSocket) {
        this.socket = clientSocket;
        clientInfoArr = new ClientInfoHolder();
        strResult = new StringHolder("Initial");
        isConnected = false;

        try {
            app = getApplicationRef();
        } catch (NamingException ex) {
            log.error("Exception while obtaining ref to EJB Application: " + ex.getMessage(), ex);
        }
        try {
            history = getHistoryExtendedRef();
        } catch (Exception ex) {
            log.error("Exception while obtaining ref to CORBA object: " + ex.getMessage(), ex);
        }
        clientSessionsCount++;
    }

    public static int getClientsOnline() {
        return clientSessionsCount;
    }

    public boolean isSocketClosed() {
        return isSocketClosed; // = socket.isInputShutdown();
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public void close() throws IOException {
        isSocketClosed = true;
        disconnect();
    }

    public void setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public int getSessionId() {
        return sessionId;
    }

    public synchronized boolean isConnected() {
        return isConnected;
    }

    public int getClientId() {
        return clientId;
    }

    @Override
    public void run() {
        try {
            //socket.setKeepAlive(true);
            //socket.setSoTimeout(readTimeout);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

			while (!isSocketClosed()) {
				readFromSocket(in);
			}

        } catch (SocketException ex) {
            String msg = ex.getMessage();
            log.error("Exception occured while working with client's socket. Remote port: "
                    + socket.getPort() + ". "
                    + msg);
        } catch (IOException ex) {
            log.error("I/O Error occured while reading from client socket. Remote port: "
                    + socket.getPort() + ". "
                    + ex.getMessage(),
                    ex);
        } finally {
            try {
                clientSessionsCount--;
                in.close();
                socket.close();
                // удаляем эту сессию в случае ошибки сокета. Application 
                // проверит вылогинелся ли клиент прежде чем ошибка возникла
                // если нет - пошлет логаут вызов к базе
                if (isConnected()) {    // если это не Application нас закрывает
                    app.removeClientSession(this);
                }
                // иначе - приложение уже о нас позаботилось 
                // без этой проверки - рекурсия с методом app.closeSessions() !!!! 
                // this.close()->socket exception и опять этот метод...
            } catch (IOException ex) {
                log.error(ex);
                app.removeClientSession(this);
            }
        }
    }

    protected void readFromSocket(DataInputStream in) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String inStr;
        while ((inStr = br.readLine()) != null) {
            String[] inputTokens = splitClientRequest(inStr);
// "HIST_REQ_START|
// "LOGOUT_REQ_START|

            switch (inputTokens[0]) {
                case "LOGIN_REQUEST_START":
                    onLoginRequest(inputTokens);

                    break;
                case "HIST_REQ_START":
                    onHistoryRequest(inputTokens);

                    break;

                case "LOGOUT_REQ_START":
                    onLogoutRequest();

                    break;

                case "KEEP_ALIVE_REQ_START":
                    onKeepAliveRequest(inputTokens);

                    break;

                default:
                    log.warn("Client's protocol is unknown ...");
            }
        }
    }

    private void onLogoutRequest() {
        String outStr;
        if (logout()) {
            // передаем клиенту - сессия завершилась
            outStr = onLogoutResponse(true);
        } else {    // ???? просто написать в лог и все?
            // иначе - передаем сообщение об ошибке и закрывать насильно
            outStr = onLogoutResponse(false);
        }
        writeToSocket(outStr);
        isConnected = false;
        // завершаем этот тред
        app.removeClientSession(this);
    }

    private void onKeepAliveRequest(String[] inputTokens) {

        String response;
        log.info("Got keep alive request from client: " + clientId + "Session_id=" + sessionId);
        response = keepAliveResp();
        writeToSocket(response);
    }

    private void onHistoryRequest(String[] inputTokens) {
        String response;
        ClientInfoHolder historyHolder = new ClientInfoHolder();
        log.info("Got request for history from client: " + clientId + "Session_id=" + sessionId);
        if (historyReqDispatch(inputTokens, historyHolder)) {
            historyReply = String.valueOf(historyHolder.value.length);
            response = onHistoryResponse(true);
            writeToSocket(response);
            for (int i = 0; i < historyHolder.value.length; i++) {
                writeToSocket(historyHolder.value[i]);
            }
        } else {
            response = onHistoryResponse(false);
            writeToSocket(response);
        }
    }

    private boolean historyReqDispatch(String[] inputTokens, ClientInfoHolder historyResult) {

        // добавить анализ обратного поля логин
        String[] clientIdToken = inputTokens[2].split("=");
        if (clientIdToken.length > 1) {
            int clientIdIn = Integer.parseInt(clientIdToken[1]);
            if (clientId == clientIdIn) {
                // TODO: Request for history here
                log.info("Processing ...");
                historyResult.value = history.getHistory(clientId);
                if (historyResult != null) {
                    log.info("Request for history successfully processed for client: ");
                    for (int i = 0; i < historyResult.value.length; i++) {
                        log.info(historyResult.value[i]);
                    }
                    log.info("Sending info to client now ...");
                } else {
                    log.info("Gor null result ...");
                }

            } else {
                log.info("Got null client_id parameter from client's request ...");
            }
            return true;
        }
        return false;
    }

    private String keepAliveResp() {
// KEEP_ALIVE_REQ_START|KEEP_ALIVE_REQ_END
// KEEP_ALIVE_RESP_START|KEEP_ALIVE_RESP_END
        String result = "KEEP_ALIVE_RESP_START|KEEP_ALIVE_RESP_END";
        return result;
    }

    private String onHistoryResponse(boolean success) {
// "HIST_REQ_START|SUCCESS|<lines_count>|client_id=<client_id>|HIST_REQ_END"
// "HIST_REQ_START|FAILURE|<reason>|client_id=<client_id>|HIST_REQ_END"
        // historyReply - в этой переменнов в случае успеха - число строк, в случае неудачи - причина
        String statusToken = success ? "SUCCESS|" : "FAILURE|";
        String result = "HIST_REQ_START|" + statusToken + historyReply + "|client_id=" + clientId + "|HIST_REQ_END";
        historyReply = null;   // если результат был не удачным - используем это поле, а потом обнуляем
        return result;
    }

    private String onLoginResponse(boolean status) {
// "LOGIN_RESULT_START|login=<login>|client_id=<client_id>|SUCCESS|LOGIN_RESULT_END"
        String s = status ? "SUCCESS" : "FAILURE";
        String respString = "LOGIN_RESULT_START|login="
                + login + "|client_id=" + clientId + "|" + s + "|LOGIN_RESULT_END";
        return respString;
    }

    private String onLogoutResponse(boolean status) {
// "LOGOUT_RESULT_START|SUCCESS|LOGOUT_RESULT_END"
        String statusToken = status ? "SUCCESS|" : "FAILURE|";
        return "LOGOUT_RESULT_START|" + statusToken + "LOGOUT_RESULT_END";
    }

    public void setToClose(boolean b) {
        isConnected = !b;
    }

    private void writeToSocket(String response) {
        if (response == null) {
            return;
        }

        log.info("Sending following info to client: " + response);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out), true);
        pw.println(response);
    }

    private void onLoginRequest(String[] inputTokens) {
        String response;
        if (loginReqDispatch(inputTokens)) {
            isConnected = true;
            response = onLoginResponse(true);
            writeToSocket(response);
        } else {
            isConnected = false;    // to be sure just yet
            response = onLoginResponse(false);
            writeToSocket(response);
            app.removeClientSession(this);
        }
    }

    private boolean loginReqDispatch(String[] inputTokens) {
        // раскукоживаем поле client_info
        log.info("Dispatching client's request ...");
        String[] clientInfoField = inputTokens[1].split("\\[", 2);
        if (clientInfoField.length > 1) {
            String[] clientInfoArrNew = clientInfoField[1].replaceAll("[\\]]|[\\[]", "").split(",");
            this.clientInfoArr.value = clientInfoArrNew;
            log.info("Dispatched \"client_info\" from app. server to Db server: "
                    + Arrays.toString(this.clientInfoArr.value));
        }

        String[] loginField = inputTokens[2].split("=");
        if (loginField.length > 1) {
            login = loginField[1];
        }

        String[] passwordField = inputTokens[3].split("=");
        if (passwordField.length > 1) {
            password = passwordField[1];
        }
        log.info("Logon request acknowledged for login: " + login);
        if (loginValid()) {
            log.info("Login request accepted for client: " + login
                    + ". Result: " + strResult.value);
            log.info("Client info (with session information): " + Arrays.toString(clientInfoArr.value));

            // приведим массив строк с информацией о клиенте в Map
            HashMap<eClientInfo, String> clientInfoMap;
            clientInfoMap = utils.toHashMap(clientInfoArr.value, "=");
            sessionId = Integer.parseInt(clientInfoMap.get(eClientInfo.session_id));
            clientId = Integer.parseInt(clientInfoMap.get(eClientInfo.client_id));
            // TODO: при закрытии клиентской сессии на сервере (процесса ClientSession)
            // проапдейтить таблицу sessions - вызвать логут!

            return true;
        } else {
            log.info("Logon declined by the server for login: " + login
                    + ". Reason: " + strResult.value);
            return false;
        }
    }

    private boolean loginValid() {
        log.info("Calling remote DB Server ...");
        return history.loginExt(login, password, clientInfoArr, strResult);
    }

    private String[] splitClientRequest(String inStr) {

        String[] inputTokens = inStr.split("\\|");
        log.info("Splitting clients request: " + Arrays.toString(inputTokens));
        for (int i = 0; i < inputTokens.length; i++) {
            log.info("Token " + i + ": " + inputTokens[i]);
        }
        return inputTokens;
    }

    private boolean logout() {
        log.info("Logout requested for the client: " + login + "(" + clientId + ")");
        if (history.logout(clientId)) {
            log.info("Successful logout for the client: " + login + "(" + clientId + ")");
            return true;
        }
        return false;
    }

    private HistoryExtended getHistoryExtendedRef() throws Exception {
        // Step 1: Instantiate the ORB
        String[] args = null;
        //String[] args = {" -ORBInitialPort", " 1050", " -ORBInitialHost", " 192.168.0.50"};
        Properties props = app.getConfigProperties();
        ORB orb = ORB.init(args, props);
        // get the root naming context
        org.omg.CORBA.Object objRef =
                orb.resolve_initial_references("NameService");
        // Use NamingContextExt instead of NamingContext. This is 
        // part of the Interoperable naming Service.  
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

        // resolve the Object Reference in Naming 
        String name = "History";
        history = HistoryExtendedHelper.narrow(ncRef.resolve_str(name));
        log.info("Obtained a handle on server object: " + history);
        return history;
//            // Step 2: Resolve the PersistentHelloServant by using INS's
//            // corbaname url. The URL locates the NameService running on
//            // localhost and listening on 1050 and resolve 
//            // 'PersistentServerTutorial' from that NameService
//            org.omg.CORBA.Object obj = orb.string_to_object( 
//                "corbaname::192.168.0.50:1050#PersistentHistoryServant");
// 
//            History history = HistoryHelper.narrow( obj );
//
//            // Step 3: Call the sayHello() method every 60 seconds and shutdown
//            // the server. Next call from the client will restart the server,
//            // because it is persistent in nature.
    }

    private ApplicationLocal getApplicationRef() throws NamingException {
        Context c = new InitialContext();
        return (ApplicationLocal) c.lookup("java:global/JeViewerEnterprise-ejb/Application!jviewer.ejb.ApplicationLocal");
    }
}
