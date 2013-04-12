/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.objectdomain;

/**
 *
 * @author Andrey
 */
public class Client {
    
    private final int id;
    private String login;
    private String password;
    private String first_name;
    private String last_name;
    
    public Client(int id, String login, String password, String first_name, String last_name) {
        this.id = id;                   // ? как хранить - брать только из базы и сразу создавать ?
        this.login = login;
        this.password = password;
        this.first_name = first_name;



        this.last_name = last_name;
    }

    

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

//    public void setPassword(String password) {
//        this.password = password;
//    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    
    
    
}
