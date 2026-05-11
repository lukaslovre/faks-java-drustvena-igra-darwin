package hr.tvz.darwin.server.network;

import hr.tvz.darwin.server.core.GameEngine;
import hr.tvz.darwin.shared.dto.GameStateDTO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Host — accepts TCP connections and assigns them to ClientHandlers.
 *
 * BLOCKING I/O:
 * The `serverSocket.accept()` call is BLOCKING. The code pauses entirely
 * until a client connects (like `await` in async JS). This is fine because
 * we're inside a Virtual Thread — it doesn't block the entire process,
 * just this thread. Other Virtual Threads can run while this one waits.
 *
 * VIRTUAL THREADS (Java 21+ / 25):
 * Virtual Threads are lightweight threads (like `async` tasks in JS).
 * They cost almost zero memory, so we can create one per connected client
 * without exhausting resources. `Thread.ofVirtual().start(handler)` spawns
 * a new virtual thread that runs the ClientHandler's run() method.
 *
 * THREAD-SAFE CLIENT LIST:
 * CopyOnWriteArrayList is used instead of ArrayList because if Player 2
 * disconnects while we're broadcasting to the list, an ArrayList would
 * throw ConcurrentModificationException. CopyOnWriteArrayList is designed
 * for exactly this scenario — safe iteration while others modify it.
 */
public class TcpServer {

    /** Port where the TCP server listens for connections. */
    private static final int PORT = 8080;

    /** Thread-safe list of all connected client handlers. */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /** The game logic engine — validates moves and manages state. */
    private final GameEngine engine;

    /** Flag to track if game has started (both players connected). */
    private boolean gameStarted = false;

    public TcpServer() {
        this.engine = new GameEngine();
        this.engine.setServer(this);
    }

    /**
     * Starts the TCP server and waits for exactly 2 players to connect.
     * Once both players are connected, the game begins and no more
     * connections are accepted.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Darwin's Journey Server started on port " + PORT + "...");
            System.out.println("Waiting for Player 1 to connect...");

            // Accept exactly 2 clients, then stop accepting new connections
            while (clients.size() < 2) {
                Socket socket = serverSocket.accept();  // BLOCKS until a client connects

                // Assign player ID based on connection order (1 or 2)
                int playerId = clients.size() + 1;
                System.out.println("Player " + playerId + " connected from " + socket.getRemoteSocketAddress());

                // Create the "waiter" (ClientHandler) for this client
                ClientHandler handler = new ClientHandler(socket, playerId, engine, this);

                // Add to the list BEFORE starting the thread (avoid race on the list)
                clients.add(handler);

                // Start the virtual thread — it begins running the ClientHandler's run() method
                Thread.ofVirtual().start(handler);
            }

            // Exit the accept loop — both players are connected.
            // Virtual threads are DAEMON threads — they don't keep the JVM alive.
            // Without this loop, main() would return and the JVM would exit
            // immediately, killing both player handlers mid-execution.
            // This infinite loop keeps the main thread (non-daemon) alive
            // so the virtual threads can actually run the game.
            // When you kill the server process (Ctrl+C), this loop dies too.
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to ALL connected clients.
     * Called by GameEngine after every valid move.
     *
     * WHY SYNCHRONIZED?
     * The `synchronized` here isn't strictly necessary since clients
     * list is CopyOnWriteArrayList (already thread-safe for iteration).
     * But it's a good defensive practice — it ensures that even if
     * someone adds a new synchronization mechanism later, broadcast()
     * remains safe. Think of it as belt-and-suspenders coding.
     *
     * @param payload The DTO to send to all clients (GameStateDTO, ChatMessageDTO, etc.)
     */
    public synchronized void broadcast(Object payload) {
        for (ClientHandler client : clients) {
            client.send(payload);
        }
    }

    /**
     * Returns the current game state (for testing purposes).
     */
    public GameStateDTO getCurrentState() {
        return engine.getCurrentState();
    }

    /**
     * Returns the list of connected clients (for testing).
     */
    public List<ClientHandler> getClients() {
        return clients;
    }
}