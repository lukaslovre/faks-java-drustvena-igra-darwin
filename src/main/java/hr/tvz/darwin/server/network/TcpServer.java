package hr.tvz.darwin.server.network;

import hr.tvz.darwin.server.core.GameEngine;
import hr.tvz.darwin.shared.dto.ErrorDTO;
import hr.tvz.darwin.shared.dto.GameStateDTO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Host — accepts TCP connections and assigns them to ClientHandlers.
 * <p>
 * BLOCKING I/O:
 * The `serverSocket.accept()` call is BLOCKING. The code pauses entirely
 * until a client connects (like `await` in async JS). This is fine because
 * we're inside a Virtual Thread — it doesn't block the entire process,
 * just this thread. Other Virtual Threads can run while this one waits.
 * <p>
 * VIRTUAL THREADS (Java 21+ / 25):
 * Virtual Threads are lightweight threads (like `async` tasks in JS).
 * They cost almost zero memory, so we can create one per connected client
 * without exhausting resources. `Thread.ofVirtual().start(handler)` spawns
 * a new virtual thread that runs the ClientHandler's run() method.
 * <p>
 * THREAD-SAFE CLIENT LIST:
 * CopyOnWriteArrayList is used instead of ArrayList because if Player 2
 * disconnects while we're broadcasting to the list, an ArrayList would
 * throw ConcurrentModificationException. CopyOnWriteArrayList is designed
 * for exactly this scenario — safe iteration while others modify it.
 */
public class TcpServer {

    /**
     * Port where the TCP server listens for connections.
     */
    private static final int PORT = 8080;

    /**
     * Thread-safe list of all connected client handlers.
     */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /**
     * The game logic engine — validates moves and manages state.
     */
    private final GameEngine engine;

    /**
     * Flag to track if game has started (both players connected).
     */
    private boolean gameStarted = false;

    public TcpServer() {
        this.engine = new GameEngine();
        this.engine.setOnStateChanged(this::broadcast);
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

            // INFINITE LOOP: The server stays alive forever here.
            // serverSocket.accept() blocks the main thread until someone connects.
            while (true) {
                Socket socket = serverSocket.accept();

                // If we already have 2 players, reject the 3rd connection
                if (clients.size() >= 2) {
                    System.out.println("Server full. Rejecting connection from " + socket.getRemoteSocketAddress());
                    socket.close();
                    continue; // Go back to waiting
                }

                // Determine Player ID safely (if list is empty -> 1, if size is 1 -> 2)
                int playerId = clients.isEmpty() ? 1 : 2;
                System.out.println("Player " + playerId + " connected from " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, playerId, engine, this);
                clients.add(handler);
                Thread.ofVirtual().start(handler);
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to ALL connected clients.
     * Called by GameEngine after every valid move.
     * <p>
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
     * Called by a ClientHandler when its socket throws an exception.
     * Cleans up the entire lobby and prepares for a fresh game.
     */
    public synchronized void handleDisconnect() {
        System.out.println("Initiating server teardown due to disconnect...");

        // 1. Tell anyone still connected that the game is over
        broadcast(new ErrorDTO("Opponent disconnected. Server resetting."));

        // 2. Force-close all sockets. This safely kills the remaining Virtual Threads.
        for (ClientHandler client : clients) {
            client.closeConnection();
        }

        // 3. Clear the lobby
        clients.clear();

        // 4. Reset the game rules
        engine.reset();

        System.out.println("Server reset complete. Waiting for new players...");
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