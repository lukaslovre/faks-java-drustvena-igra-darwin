package hr.tvz.darwin.server;

import hr.tvz.darwin.server.network.TcpServer;
import hr.tvz.darwin.server.rmi.DarwinArchiveImpl;

import javax.naming.InitialContext;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApp {
    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());

    public static void main(String[] args) {
        LOGGER.fine(() -> "Startup arguments: " + String.join(" ", args));
        LOGGER.info("Darwin's Journey - Server Starting...");

        try {
            LOGGER.info("Starting RMI Registry and JNDI bindings...");

            DarwinArchiveImpl archiveService = DarwinArchiveImpl.getInstance();
            LocateRegistry.createRegistry(1099);

            var ctx = new InitialContext();
            ctx.rebind("rmi://localhost:1099/DarwinArchive", archiveService);

            LOGGER.info("Darwin Archive service bound in JNDI.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize RMI/JNDI services", e);
            System.exit(1);
        }

        TcpServer server = new TcpServer();
        server.start();

        LOGGER.info("Server shutdown complete.");
    }
}
