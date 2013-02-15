/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewer.dbserver;

import jviewer.controller.orbHistory.HistoryExtended;
import jviewer.controller.orbHistory.HistoryExtendedHelper;
import jviewer.ejb.ApplicationRemote;
import jviewer.util.logging.Logging;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author Andrey
 */
public class JeViewerDBServer {

    private static final Logging log = new Logging(JeViewerDBServer.class);

    private static ApplicationRemote application = null;
    private static String[] args;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //JeViewerDBServer.args = args;
        String[] ar = { "-ORBInitialPort", "1050", "-ORBInitialHost", "localhost" };
        JeViewerDBServer.args = ar;
        initProperties();
        
        String path = System.getProperty("java.class.path");
        log.info(path);
        log.info("\n");

        //startupDbServer();
        startupDbServer2();

    }

    private static void startupDbServer() {
        try {


            // Step 1: Instantiate the ORB
            ORB orb = ORB.init(args, application.getConfigProperties());


            // Step 2: Instantiate the servant
            PersistentHistoryServant servant = new PersistentHistoryServant(orb);

            // Step 3 : Create a POA with Persistent Policy
            // *******************  
            // Step 3-1: Get the rootPOA 
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            // Step 3-2: Create the Persistent Policy
            Policy[] persistentPolicy = new Policy[1];
            persistentPolicy[0] = rootPOA.create_lifespan_policy(
                    LifespanPolicyValue.PERSISTENT);
            // Step 3-3: Create a POA by passing the Persistent Policy
            POA persistentPOA = rootPOA.create_POA("childPOA", null, persistentPolicy);
            // Step 3-4: Activate PersistentPOA's POAManager, Without this
            // All calls to Persistent Server will hang because POAManager
            // will be in the 'HOLD' state.
            persistentPOA.the_POAManager().activate();
            // ***********************

            // Step 4: Associate the servant with PersistentPOA
            persistentPOA.activate_object(servant);

            // Step 5: Resolve RootNaming context and bind a name for the
            // servant.
            // NOTE: If the Server is persistent in nature then using Persistent
            // Name Service is a good choice. Even if ORBD is restarted the Name
            // Bindings will be intact. To use Persistent Name Service use
            // 'NameService' as the key for resolve_initial_references() when
            // ORBD is running.
            org.omg.CORBA.Object obj = orb.resolve_initial_references(
                    "NameService");
            NamingContextExt rootContext = NamingContextExtHelper.narrow(obj);

            NameComponent[] nc = rootContext.to_name(
                    "PersistentHistoryServant");
            rootContext.rebind(nc, persistentPOA.servant_to_reference(
                    servant));

            // Step 6: We are ready to receive client requests
            log.info("Persistent ORB server ready.");
            orb.run();

        } catch (Exception e) {
            log.error("Exception in Persistent Server Startup: " + e.getMessage(), e);
        }
    }

    private static void startupDbServer2() {
        try {
            // create and initialize the ORB
            //ORB orb = ORB.init(args, application.getConfigProperties());
            ORB orb = ORB.init(args, null);

            // get reference to rootpoa & activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            //HelloImpl helloImpl = new HelloImpl();
            PersistentHistoryServant servant = new PersistentHistoryServant(orb);

            // get object reference from the servant
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(servant);
            HistoryExtended href = HistoryExtendedHelper.narrow(ref);

            // get the root naming context
            // NameService invokes the name service
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            // Use NamingContextExt which is part of the Interoperable
            // Naming Service (INS) specification.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            String name = "History";
            NameComponent path[] = ncRef.to_name(name);
            ncRef.rebind(path, href);

            log.info("Db History server ready and waiting ...");

            // wait for invocations from clients
            orb.run();
        } catch (Exception e) {
            log.error("ERROR: " + e.getMessage(), e);
        }

        log.info("HelloServer Exiting ...");

    }

    private static void initProperties() {
        try {
            Context c = new InitialContext();
            application = (ApplicationRemote) //c.lookup("java:global/JeViewerEnterprise-ejb/Application/ApplicationRemote");
                    c.lookup("java:global/JeViewerEnterprise-ejb/Application!jviewer.ejb.ApplicationRemote");
            String dbHost = InetAddress.getLocalHost().getHostAddress();
            application.setConfigProperty("org.omg.CORBA.ORBInitialHost", dbHost);
        } catch (UnknownHostException | NamingException ex) {
            log.error("ERROR: " + ex.getMessage(), ex);
        }
    }
}
