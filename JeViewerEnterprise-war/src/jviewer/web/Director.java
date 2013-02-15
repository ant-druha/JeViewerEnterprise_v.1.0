/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.web;

import jviewer.ejb.ApplicationRemote;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Andrey
 */
public class Director extends HttpServlet {
    
    @EJB
    private ApplicationRemote application;

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //response.setContentType("text/html;charset=UTF-8");
        response.setContentType("text;charset=UTF-8");
        PrintWriter out = response.getWriter();
        //frontendHost.append(application.getConfigProperty("frontendHost"));
        String frontendPort = application.getConfigProperty("frontendPort");
        // this is also we should get either dinamically or from appl.xml file
        // host ip - from java method, rest of the url from descriptor
        String frontendUrl = application.getConfigProperty("frontendUrl");
        String frontendHost = application.getConfigProperty("frontendHost");
        try {
            /* TODO output your page here. You may use following sample code. */
            
            // тут узнаем конецную точку входа для клиента и отправляем ее на страницу
            // клиент ее парсит и подключается
            out.println("frontendHost=" + frontendHost);
            out.println("frontendPort=" + frontendPort);
            out.println("frontendUrl=" + frontendUrl);
            
        } finally {            
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
