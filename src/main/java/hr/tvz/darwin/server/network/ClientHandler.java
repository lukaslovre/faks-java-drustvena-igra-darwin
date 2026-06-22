package hr.tvz.darwin.server.network;

import hr.tvz.darwin.server.core.GameEngine;
import hr.tvz.darwin.server.core.InvalidMoveException;
import hr.tvz.darwin.shared.dto.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** Reads serialized DTOs for one client on its own virtual thread. */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int playerId;
    private final GameEngine engine;
    private final TcpServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, int playerId, GameEngine engine, TcpServer server) throws IOException {
        this.socket = socket;
        this.playerId = playerId;
        this.engine = engine;
        this.server = server;

        if (socket != null) {
            // Both peers create output first; otherwise each input constructor can
            // wait forever for the other side's serialization header.
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }
    }

    /** The main loop — runs in a Virtual Thread until client disconnects. */
    @Override
    public void run() {
        try {
            // 1. THE HANDSHAKE: Tell the client who they are
            send(new WelcomeDTO(playerId));
            System.out.println("Sent WelcomeDTO to Player " + playerId);

            // If this is Player 2, broadcast initial state to start the game
            if (playerId == 2) {
                System.out.println("Both players connected! Broadcasting initial state...");
                server.broadcast(engine.getCurrentState());
            } else {
                System.out.println("Player 1 waiting for opponent...");
            }

            // 2. THE INFINITE LISTENING LOOP — processes DTOs until disconnect
            while (true) {
                // BLOCKS until client sends an object
                Object payload = in.readObject();
                switch (payload) {
                    case MoveRequestDTO move -> {
                        System.out.println("Player " + move.playerId() + " move request: Worker "
                                + move.workerId() + " -> " + move.targetIsland());
                        try {
                            engine.processMove(move);
                        } catch (InvalidMoveException e) {
                            send(new ErrorDTO(e.getMessage()));
                        }
                    }
                    case ChatMessageDTO chat -> {
                        System.out.println("Player " + chat.playerId() + " chat: " + chat.message());
                        server.broadcast(chat);
                    }
                    case null -> System.out.println("Received null payload");
                    default -> System.out.println("Unknown payload type: " + payload.getClass().getName());
                }
            }

        } catch (Exception _) {
            System.out.println("Player " + playerId + " disconnected.");
            server.handleDisconnect();
        }
    }

    /** Serializes writes because ObjectOutputStream is not thread-safe. */
    public synchronized void send(Object payload) {
        // Guard: skip if no output stream (testing mode)
        if (out == null) {
            return;
        }
        try {
            out.writeObject(payload);
            out.flush();
            // Forget prior object identities so each DTO graph is written afresh.
            out.reset();
        } catch (IOException e) {
            System.err.println("Error sending to Player " + playerId + ": " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // The connection is already being discarded.
        }
    }

    /** Returns the player ID for this handler. */
    public int getPlayerId() {
        return playerId;
    }

    /** Returns reference to the TcpServer (for broadcasting). */
    public TcpServer getServer() {
        return server;
    }
}
