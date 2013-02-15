/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.util.persistence;

import jviewer.ejb.ApplicationRemote;
import jviewer.util.logging.Logging;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;



/**
 *
 * @author Andrey
 */
public class JDBCUtils {
    
    private static OracleConnectionPoolDataSource ocpds = null;
    private static ApplicationRemote application = null;
    private static Logging log = new Logging(JDBCUtils.class);
    
    private JDBCUtils() {}

    static {
        System.out.println("OracleDataSource Initialization");
        try {
            // TODO: work with .properties
            Context c = new InitialContext();
            
            application = (ApplicationRemote) 
                    c.lookup("java:global/JeViewerEnterprise-ejb/Application!jviewer.ejb.ApplicationRemote");
            
            ocpds = new OracleConnectionPoolDataSource();
            String dbURL = application.getConfigProperty("dbUrl");
            String dbUser = application.getConfigProperty("dbUser");
            String dbPassword = application.getConfigProperty("dbPassword");
            
            ocpds.setURL(dbURL);
            ocpds.setUser(dbUser);
            ocpds.setPassword(dbPassword);
            
            //application.reloadConfigProperties();
            log.info(application.getConfigProperty("frontendHost"));
            
            //application.getConfigProperty()
            
        } catch (NamingException ex) { 
            log.error(ex);
        }
        catch (SQLException e) {
            log.error(e);
        }
    }


    public static Connection getConnection(String env) throws SQLException {

            log.info("Request connection for " + env);
            if (ocpds == null) {        // а если соединение закрыли??
                throw new SQLException("OracleDataSource is null.");
            }
            
            PooledConnection pc = ocpds.getPooledConnection();
            Connection con = pc.getConnection();
            con.setAutoCommit(false);
            return con;
    }

    public static void closePooledConnections() throws SQLException{
      if (ocpds != null ) {
          //ocpds.close();    // ??
      }
    }
}
