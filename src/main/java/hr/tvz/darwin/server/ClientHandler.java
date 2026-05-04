package hr.tvz.darwin.server;

import hr.tvz.darwin.shared.dto.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The Waiter — one per client, carries requests to the kitchen (GameEngine).
 *
 * VIRTUAL THREAD LIFECYCLE:
 * Each ClientHandler runs in its own Virtual Thread (created by TcpServer).
 * The run() method has an infinite loop that blocks on inputStream.readObject()
 * until data arrives from the client. This is similar to a JS async while(true)
 * loop with `await socket.recv()`.
 *
 * OBJECT STREAMS (Java Serialization vs JSON):
 * Unlike JS where you send JSON strings, Java sends raw objects over TCP.
 * ObjectOutputStream serializes objects to bytes (like JSON.stringify).
 * ObjectInputStream deserializes bytes back to objects (like JSON.parse).
 * Every object sent MUST implement Serializable (see DTO records).
 *
 * IMPORTANT: Output stream MUST be created BEFORE input stream in Java.
 * If you create input first, Java can deadlock waiting for the client to
 * send data while the client is waiting for your output. Always do:
 * 1. out = new ObjectOutputStream(socket.getOutputStream())
 * 2. in = new ObjectInputStream(socket.getInputStream())
 */
public class ClientHandler implements Runnable {

    /** The TCP socket connected to this client. */
    private final Socket socket;

    /** Which player this handler represents (1 or 2). */
    private final int playerId;

    /** The game logic engine (the chef/kitchen). */
    private final GameEngine engine;

    /** Reference back to TcpServer so we can broadcast chat messages. */
    private final TcpServer server;

    /** Input stream — read DTOs sent by the client. */
    private ObjectInputStream in;

    /** Output stream — send DTOs back to the client. */
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, int playerId, GameEngine engine, TcpServer server) throws IOException {
        this.socket = socket;
        this.playerId = playerId;
        this.engine = engine;
        this.server = server;

        // Only create streams if we have a real socket (null socket is allowed for testing)
        if (socket != null) {
            // IMPORTANT: Create output FIRST, then input — prevents deadlock
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }
    }

    /**
     * The main loop — runs in a Virtual Thread until client disconnects.
     *
     * BLOCKING I/O:
     * The `in.readObject()` call is blocking. This Virtual Thread sits idle
     * (zero CPU usage) until the client sends a DTO. This is like `await fetch()`
     * in JS — the thread sleeps until data arrives, then wakes up to process it.
     */
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

                // Java 25 Pattern Matching switch — like a JS switch on typeof/instanceof
                // This routes different DTO types to different handlers
                switch (payload) {
                    case MoveRequestDTO move -> {
                        System.out.println("Player " + move.playerId() + " move request: Worker "
                                + move.workerId() + " -> " + move.targetIsland());
                        engine.processMove(move, this);
                    }
                    case ChatMessageDTO chat -> {
                        System.out.println("Player " + chat.playerId() + " chat: " + chat.message());
                        server.broadcast(chat);  // Broadcast to ALL clients including sender
                    }
                    case null -> System.out.println("Received null payload");
                    default -> System.out.println("Unknown payload type: " + payload.getClass().getName());
                }
            }

        } catch (Exception e) {
            // Client disconnected — IOException is normal when client closes
            System.out.println("Player " + playerId + " disconnected: " + e.getMessage());

            // Tell the other player the game is over
            server.broadcast(new ErrorDTO("Opponent disconnected. Game Over."));

            // In a full implementation, we'd clean up the client list here
            // For this demo, we just let the remaining player see the error
        }
    }

    /**
     * Sends a DTO to this client over TCP.
     *
     * WHY SYNCHRONIZED?
     * If the GameEngine calls broadcast() while a ChatMessage is being sent,
     * two threads might try to write to the same socket simultaneously.
     * ObjectOutputStream is NOT thread-safe, so we synchronize access.
     * This is like how you might use a mutex in JS to prevent concurrent writes.
     *
     * out.reset() IS CRITICAL:
     * Java caches recently serialized objects. If you send the same object
     * twice, Java might just send a reference instead of re-serializing it.
     * reset() clears this cache so each send is a fresh serialization.
     * Without this, clients might see stale/garbage data.
     *
     * @param payload The DTO to send (WelcomeDTO, GameStateDTO, ErrorDTO, etc.)
     */
    public synchronized void send(Object payload) {
        // Guard: skip if no output stream (testing mode)
        if (out == null) {
            return;
        }
        try {
            out.writeObject(payload);
            out.flush();
            out.reset();  // Clear serialization cache — prevents stale references
        } catch (IOException e) {
            System.err.println("Error sending to Player " + playerId + ": " + e.getMessage());
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