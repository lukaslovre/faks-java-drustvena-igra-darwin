package hr.tvz.darwin.server.network;

import hr.tvz.darwin.server.DomXmlWriter;
import hr.tvz.darwin.server.GameStateSerializer;
import hr.tvz.darwin.server.core.GameEngine;
import hr.tvz.darwin.server.rmi.DarwinArchiveImpl;
import hr.tvz.darwin.shared.dto.ErrorDTO;
import hr.tvz.darwin.shared.dto.GameStateDTO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/** Accepts TCP clients and assigns each connection to a virtual thread. */
public class TcpServer {
    private static final int PORT = 8080;

    // Broadcast iteration remains safe while a handler disconnects.
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final GameEngine engine;
    private final GameStateSerializer gameStateSerializer;

    public TcpServer() {
        this.engine = new GameEngine();
        this.gameStateSerializer = new GameStateSerializer();
        this.engine.setOnStateChanged(state -> {
            broadcast(state);
            // Save game state if game ended
            if (state.winnerId() != 0) {
                saveFinalState(state);
                new DomXmlWriter().saveGame(engine.getMoveHistory(), state.winnerId());
                DarwinArchiveImpl.getInstance().onGameEnded(state);
            }
        });
    }

    private void saveFinalState(GameStateDTO state) {
        try {
            gameStateSerializer.save(state);
            System.out.println("Final game state saved to saves/latest-game.ser");
        } catch (IOException e) {
            // A filesystem problem must not stop the TCP server or the other end-game tasks.
            System.err.println("Could not save final game state: " + e.getMessage());
        }
    }

    /** Accepts at most two players for the active match. */
    public void start() {
        // try-with-resources closes the socket and executor when start() exits.
        try (var serverSocket = new ServerSocket(PORT);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            System.out.println("Darwin's Journey Server started on port " + PORT + "...");
            System.out.println("Waiting for Player 1 to connect...");

            while (true) {
                Socket socket = serverSocket.accept();

                if (clients.size() >= 2) {
                    System.out.println("Server full. Rejecting connection from " + socket.getRemoteSocketAddress());
                    socket.close();
                    continue;
                }

                int playerId = clients.isEmpty() ? 1 : 2;
                System.out.println("Player " + playerId + " connected from " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, playerId, engine, this);
                clients.add(handler);
                executor.submit(handler);
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends one DTO to every client. Synchronization preserves a consistent
     * message order when game-state and chat broadcasts originate concurrently.
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
     * Returns the current game state (for testing).
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
