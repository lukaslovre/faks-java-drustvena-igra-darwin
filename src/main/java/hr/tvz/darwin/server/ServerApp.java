package hr.tvz.darwin.server;

import hr.tvz.darwin.server.network.TcpServer;
import hr.tvz.darwin.server.rmi.DarwinArchiveImpl;

import javax.naming.InitialContext;
import java.rmi.registry.LocateRegistry;

public class ServerApp {

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("Darwin's Journey - Server Starting...");
        System.out.println("=".repeat(50));

        try {
            System.out.println("Starting RMI Registry and JNDI bindings...");

            DarwinArchiveImpl archiveService = DarwinArchiveImpl.getInstance();
            LocateRegistry.createRegistry(1099);

            var ctx = new InitialContext();
            ctx.rebind("rmi://localhost:1099/DarwinArchive", archiveService);

            System.out.println("Darwin Archive service bound in JNDI.");
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to initialize RMI/JNDI services: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        TcpServer server = new TcpServer();
        server.start();

        System.out.println("Server shutdown complete.");
    }
}