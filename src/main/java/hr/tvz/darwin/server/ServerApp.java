package hr.tvz.darwin.server;

import hr.tvz.darwin.server.network.TcpServer;

/**
 * Server entry point — launches the TCP server and RMI registry.
 * <p>
 * IMPORTANT: Both TCP (port 8080) and RMI (port 1099) are started here.
 * RMI will be fully implemented in Phase 6, but the skeleton is here.
 */
public class ServerApp {

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("Darwin's Journey - Server Starting...");
        System.out.println("=".repeat(50));

        // Start the TCP server (listens on port 8080)
        // This call blocks — the server runs until you kill the process
        TcpServer server = new TcpServer();
        server.start();

        System.out.println("Server shutdown complete.");
    }
}