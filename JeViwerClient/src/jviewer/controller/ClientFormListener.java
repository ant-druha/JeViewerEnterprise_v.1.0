/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.controller;

import jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder;
import jviewer.jeviwerclient.gui.ClientForm;
import jviewer.objectdomain.eClientInfo;
import jviewer.util.logging.Logging;
import jviewer.util.utils;
import org.omg.CORBA.StringHolder;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Andrey
 */
public class ClientFormListener extends Thread {

    private final Logging log = new Logging(ClientFormListener.class);
    private int clientId;
    private ClientForm clientForm;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isSocketClosed;
    private ReentrantLock lockForServerCall;
    private long keepAliveInterval;

    public ClientFormListener(Socket socket, ClientForm clientForm) {
        this(socket, clientForm, 5000);
    }
    
    public ClientFormListener(Socket socket, ClientForm clientForm, int keepAliveInterval) {
        this.socket = socket;
        this.clientForm = clientForm;
        lockForServerCall = new ReentrantLock();
        this.keepAliveInterval = keepAliveInterval;
    }

    public void close() throws IOException {
        isSocketClosed = true;
        disconnect();
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public void setSoTimeout(int soTimeout) throws SocketException {
        socket.setSoTimeout(soTimeout);
    }

    private boolean isConnected() {
        return !isSocketClosed;
    }

    private void serverRequestEnd() {
        lockForServerCall.unlock();
    }

    private void serverRequestStart() {
        lockForServerCall.lock();
    }

    @Override
    public void run() {

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            DataInputStream inTmp = new DataInputStream(socket.getInputStream());

            while (isConnected()) {
                // пока форма не попросила нас сделать вызов на сервер 
                // много ест CPU нужно его посылать поспать!
                // и пока сокет жив
                try {
                    sleep(keepAliveInterval);
                } catch (InterruptedException ex) {
                    log.error(ex);
                }
                serverRequestStart();
                keepAlive(in, out);
                serverRequestEnd();

            }
            
        } catch (SocketException ex) {
            log.error("Exception occurred while working with server's socket. Remote server port: "
                    + socket.getPort() + ". "
                    + ex.getMessage());
        } catch (IOException ex) {
            log.error("I/O Socket Error while working with remote server: " + ex.getMessage(), ex);

            // вот тут закрываем поток и говорим форме - disconnected!
        } finally {
            try {
                close();    // этого можно и не делать
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
            log.warn("ClientFormListener is exiting ...");
            clientForm.setConnected(false);
        }
    }

    public String[] getHistory(int clientId) {
        serverRequestStart();
        String[] result = getClientHistory(clientId);
        serverRequestEnd();
        return result;
    }

    public boolean login(String login, String password) {
        serverRequestStart();
        boolean result = sendLoginRequest(login, password);
        serverRequestEnd();
        isSocketClosed = !result;
        return result;
    }

    public boolean logout(int clientId) {
        serverRequestStart();
        PrintWriter pw = null;
        String outStr = "LOGOUT_REQ_START|LOGOUT_REQ_END";
        String inStr;
        try {
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            log.info("Sending logout request to server. InetAddress: " + socket.getInetAddress().toString());
            pw.println(outStr);

            log.info("Awation answer from server ...");
            while ((inStr = br.readLine()) != null) {
                String[] reqFields = inStr.split("\\|");
                log.info("Splitting clients response: " + inStr);

                if (reqFields.length > 2) {
                    if (reqFields[0].equals("LOGOUT_RESULT_START")
                            && reqFields[1].equals("SUCCESS")) {
                        clientForm.setConnected(false);
                        close();
                        return true;
                    }
                }   // else ???
            }
        } catch (IOException ex) {
            log.error("I/O Error while logout: " + ex.getMessage(), ex);
            clientForm.setConnected(false);
        } finally {
            pw.close();
            serverRequestEnd();
        }
        //clientForm.setConnected(false); ?? не понятно вылогинелись или нет ... хотя нормально этого не должно быть...
        return false;
    }

    int getClientId() {
        return clientId;
    }

    private boolean sendLoginRequest(String login, String password) {
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out), true);

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String loginRequest, loginResult;
            loginRequest = "LOGIN_REQUEST_START|"
                    + "clientInfo=" + getClass().getName()
                    + "|login=" + login + "|password=" + password
                    + "|LOGIN_REQUEST_END";
            log.info("Sending following login request to server. InetAdderss: "
                    + socket.getInetAddress().toString() + ". Request string:\n"
                    + loginRequest);

            pw.println(loginRequest);

            log.info("Awation answer from server ...");
            while ((loginResult = br.readLine()) != null) {
                String[] reqFields = splitServerRequest(loginRequest);
                String[] loginToken = reqFields[1].split("=");
                String respLogin = loginToken[1];
                if (respLogin.equals(login)) {
                    switch (reqFields[3]) {
                        case "SUCCESS":
                            String[] clientIdToken = reqFields[2].split("=");
                            if (clientIdToken.length > 1) {
                                clientId = Integer.parseInt(clientIdToken[1]);
                                log.info("Successful login for login=" + login + "(id=" + clientId + ")");
                            }
                            log.error("Null client_id parameter returned ...");
                            return true;

                        case "FAILURE":
                            log.info("Login failed for login: " + login);
                            return false;
                    }
                }
            }
        } catch (UnknownHostException ex) {
            log.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
        }
        log.error("Login failed for login: " + login);
        return false;
    }

    public boolean login(String login, String password, ClientInfoHolder clientInfo, StringHolder strResult) {
        serverRequestStart();
        boolean result = sendLoginRequest(login, password, clientInfo, strResult);
        serverRequestEnd();
        isSocketClosed = !result;
        return result;
    }

    // TODO: вынести все параметры в класс, а не таскать во все методы
    private boolean sendLoginRequest(String login, String password,
            ClientInfoHolder clientInfo, StringHolder strResult) {
        try {

            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String loginResult = null;
            // это можно сделать еще в клиентской форме
            HashMap<eClientInfo, String> mapInfo = new HashMap<>();
            mapInfo.put(eClientInfo.client_ip, socket.getLocalAddress().getHostAddress());
            mapInfo.put(eClientInfo.app_info, getClass().getName());
            mapInfo.put(eClientInfo.app_version, "1");
            //String[] clInfo = new String[]{"app_info=" + getClass().getName(), "app_version=1"};
            //clientInfo.value = clInfo;
            clientInfo.value = utils.mapToString(mapInfo, "=");
            log.info("Sending login request to server. InetAddress: " + socket.getInetAddress().toString());
            pw.println("LOGIN_REQUEST_START|"
                    + "clientInfo=" + Arrays.toString(clientInfo.value).replaceAll("\\s", "")
                    + "|login=" + login + "|password=" + password
                    + "|LOGIN_REQUEST_END");

            log.info("Awation answer from server ...");
            while ((loginResult = br.readLine()) != null) {
                String[] reqFields = splitServerRequest(loginResult);

                // добавить анализ обратного поля логин
                String[] loginToken = reqFields[1].split("=");
                String respLogin = loginToken[1];

                if (respLogin.equals(login)
                        && reqFields[3].equals("SUCCESS")) {
                    String[] clientIdToken = reqFields[2].split("=");
                    if (clientIdToken.length > 1) {
                        clientId = Integer.parseInt(clientIdToken[1]);
                        log.info("Successful login for login=" + login + "(id=" + clientId + ")");
                        clientForm.setClientId(clientId);
                        return true;
                    }
                    log.error("Null client_id parameter returned ...");
                }
            }
        } catch (UnknownHostException ex) {
            log.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Login failed for login: " + login);
        return false;
    }

    private void readFromSocket(DataInputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String inStr;
        log.info("perform reading from socket");
        while ((inStr = br.readLine()) != null) {
            log.info("(readFromSocket method) Got string from server in : " + inStr);
        }
        log.info("End reading from socket");
    }

    private String[] splitServerRequest(String inStr) {
        String[] inputTokens = inStr.split("\\|");
        log.info("Splitting clients request: " + Arrays.toString(inputTokens));
        for (int i = 0; i < inputTokens.length; i++) {
            log.info("Token " + i + ": " + inputTokens[i]);
        }
        return inputTokens;
    }

    private String[] getClientHistory(int clientId) {
        String[] strArrHistory = null;
        try {

            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String requestString = null;
            log.info("Sending request for a history to server. InetAddress: " + socket.getInetAddress().toString());
            requestString = "HIST_REQ_START||client_id=" + clientId + "|HIST_REQ_END";
            pw.println(requestString);

            String response = null;
            String[] strHistory = null;
            ArrayList<String> strArr = new ArrayList<>();
            log.info("Awation answer from server ...");
            while ((response = br.readLine()) != null) {
//HIST_REQ_START|SUCCESS|null|client_id=1|HIST_REQ_END
                String[] reqFields = splitServerRequest(response);
                if (reqFields[0].equals("HIST_REQ_START")
                        && reqFields[1].equals("SUCCESS")) {
                    log.info("Got positive responce for history from Server: ");
                    int lines = Integer.parseInt(reqFields[2]);
                    while (lines > 0 && (response = br.readLine()) != null) {
                        log.info(response);
                        strArr.add(response);
                        lines--;
                    }
                    strHistory = new String[strArr.size()];
                    strArr.toArray(strHistory);
                    log.info("History requesct succesfully processed");
                    return strHistory;
                }
            }
        } catch (UnknownHostException ex) {
            log.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return strArrHistory;

    }

    private void keepAlive(DataInputStream in, DataOutputStream out) {
// KEEP_ALIVE_REQ_START|KEEP_ALIVE_REQ_END
// KEEP_ALIVE_RESP_START|KEEP_ALIVE_RESP_END
        PrintWriter pw = null;
        String strRequest, strResponse;
        try {
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            strRequest = "KEEP_ALIVE_REQ_START|KEEP_ALIVE_REQ_END";
            pw.println(strRequest);
            log.info("Keepalive pack sent to server ...");
            String[] strTokens;

            try {
                while ((strResponse = br.readLine()) != null) {
                    strTokens = splitServerRequest(strResponse);
                    if (strTokens.length > 1) {
                        if (strTokens[0].equals("KEEP_ALIVE_RESP_START")) {
                            log.info("Got keepalive reply from server ...");
                            return;
                        } else {
                            log.warn("Got unrecognized response ...");
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                log.warn("Timeout while wating server reply ...", e);
                //clientForm.setConnected(false);
                close();
                clientForm.setConnected(false);
            }

        } catch (IOException ex) {
                log.error("I/O error while working with server ..." + ex.getMessage(), ex);
            try {
                close();
            } catch (IOException ex1) {
                log.error(ex1);
            }
                clientForm.setConnected(false);
        }  
    }
}