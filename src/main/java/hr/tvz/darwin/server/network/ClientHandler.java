package hr.tvz.darwin.server.network;

import hr.tvz.darwin.server.core.GameEngine;
import hr.tvz.darwin.server.core.InvalidMoveException;
import hr.tvz.darwin.shared.dto.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reads serialized DTOs for one client on its own virtual thread. */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private static final String PLAYER_PREFIX = "Player ";

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
            LOGGER.info(() -> "Sent WelcomeDTO to " + PLAYER_PREFIX + playerId);

            // If this is Player 2, broadcast initial state to start the game
            if (playerId == 2) {
                LOGGER.info("Both players connected! Broadcasting initial state...");
                server.broadcast(engine.getCurrentState());
            } else {
                LOGGER.info(PLAYER_PREFIX + "1 waiting for opponent...");
            }

            // 2. THE INFINITE LISTENING LOOP — processes DTOs until disconnect
            while (true) {
                // BLOCKS until client sends an object
                Object payload = in.readObject();
                switch (payload) {
                    case MoveRequestDTO move -> processMove(move);
                    case ChatMessageDTO chat -> {
                        LOGGER.info(() -> PLAYER_PREFIX + chat.playerId() + " chat: " + chat.message());
                        server.broadcast(chat);
                    }
                    case null -> LOGGER.info("Received null payload");
                    default -> LOGGER.info(() -> "Unknown payload type: " + payload.getClass().getName());
                }
            }

        } catch (Exception _) {
            LOGGER.info(() -> PLAYER_PREFIX + playerId + " disconnected.");
            server.handleDisconnect();
        }
    }

    private void processMove(MoveRequestDTO move) {
        LOGGER.info(() -> PLAYER_PREFIX + move.playerId() + " move request: Worker "
                + move.workerId() + " -> " + move.targetIsland());
        try {
            engine.processMove(move);
        } catch (InvalidMoveException e) {
            send(new ErrorDTO(e.getMessage()));
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
            LOGGER.log(Level.WARNING, e, () -> "Error sending to Player " + playerId);
        }
    }

    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException _) {
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
