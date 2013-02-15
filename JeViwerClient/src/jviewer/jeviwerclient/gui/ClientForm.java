/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.jeviwerclient.gui;

import jviewer.controller.ClientFormListener;
import jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder;
import jviewer.util.logging.Logging;
import org.omg.CORBA.StringHolder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 *
 * @author Andrey
 */
public class ClientForm extends ClientDesignForm implements ActionListener {

    private final Logging log = new Logging(ClientForm.class);

    private String login, password;
    private int clientId;
    private ClientInfoHolder clientInfo;
    private StringHolder strLoginResult;
    private ClientFormListener formListener;
    private boolean isConnected;


    public ClientForm() {
        super();
        btnLogin.addActionListener(this);
        btnLogout.addActionListener(this);
        txtServerUrl.addActionListener(this);
        btnGetHistory.addActionListener(this);
        txtAreaHistory.setEditable(false);
        //formController = new ClientFormListener();
        
        clientInfo = new ClientInfoHolder();
        strLoginResult = new StringHolder();
        log.info("Client form started.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        switch (e.getActionCommand()) {
            case "Login":
                onLogin();

                break;
            case "Logout":
                onLogout();

                break;
            case "Get History":
                onGetHistory();

                break;
        }

    }

    private void onLogin() {
        if (isConnected) {
            return;
        }
        setServerInfo();
        getUserInfo();
        
        if (formListener.login(login, password, clientInfo, strLoginResult)) {
            try {
                setConnected(true);
                formListener.setSoTimeout(6000);
                formListener.start();
                log.info("Form Listener started.");
            } catch (SocketException ex) {
                log.error(ex);
            }
        } else {
            setConnected(false);
            //
        }

    }

    private void setServerInfo() {
        String urlStr = txtServerUrl.getText().trim();
        String serverHost = null;
        int serverPort = 0;
        try {    
            URL url = new URL(urlStr);
            URLConnection uConn = url.openConnection();
            uConn.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("frontendHost")) {
                    serverHost = line.substring("frontendHost".length() + 1);
                } else if (line.startsWith("frontendPort")) {
                    serverPort = Integer.parseInt(line.substring("frontendPort".length() + 1));
                }
            }
            if (serverHost!=null && serverPort!= 0) {
                //formListener.setServerInfo(serverHost, serverPort);
                Socket s = new Socket(serverHost, serverPort);
                s.setKeepAlive(true);
                log.info("Server info set. Port=" + serverPort +
                        " host=" + serverHost);
                
                formListener = new ClientFormListener(s, this);
                
            } else  {
                log.warn("Could not determine server parameters: port " + serverPort +
                        " host " + serverHost);
            }
        } catch (MalformedURLException ex) {
            log.error(ex.getMessage());
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }

    private void getUserInfo() {
        login = txtLogin.getText();
        password = new String(txtPassword.getPassword());
    }

    

    private void onGetHistory() {
        if (txtAreaHistory.isEditable()) {
            String[] arrHistory = new String[1];
            arrHistory = formListener.getHistory(clientId);
            for (int i = 0; i < arrHistory.length; i++) {
                txtAreaHistory.append(arrHistory[i]+ "\n");
            }
        }
    }

    private void onLogout() {
        if (formListener.logout(0)) {   // на самом деле тут нам этот параметр не нужен=(
            setConnected(false);
        } else {
            
        }
        
    }
    
    public void setConnected(boolean isConnected) {
        if (isConnected) {
            txtAreaHistory.setEditable(true);
            lblStatusInfo.setText("Connected");
            lblStatusInfo.setForeground(Color.GREEN);
            this.isConnected = true;
        } else {
            txtAreaHistory.setEditable(false);
            lblStatusInfo.setText("Disconnected");
            lblStatusInfo.setForeground(Color.RED);
            this.isConnected = false;
            if (formListener!=null) {
                try {
                    formListener.close();
                } catch (IOException ex) {
                    log.error(ex);
                }
            }
        }
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
        lblClientIdVal.setText(Integer.toString(clientId));
    }
}
