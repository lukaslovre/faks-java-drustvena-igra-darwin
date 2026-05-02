package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Sent by the Server IMMEDIATELY after a client connects via TCP.
 * <p>
 * This is the "handshake" — the server tells the client: "You are Player 1"
 * or "You are Player 2." The client saves this ID and uses it to:
 * - Determine which UI color to show (Red vs Blue)
 * - Know when it's their turn (activePlayerId matches myPlayerId)
 * <p>
 * In JS/TS terms, think of this like a WebSocket "connection ack" message:
 * socket.onopen → server sends { type: "welcome", playerId: 1 }
 * <p>
 * WHY A SEPARATE DTO INSTEAD OF JUST GAME STATE?
 * Because the client needs to know its identity BEFORE the first
 * GameStateDTO arrives. Without this, the client can't tell which
 * player it is in the game state.
 * <p>
 * WHY `implements Serializable`?
 * Java's TCP sockets send raw bytes. Serializable is Java's built-in
 * mechanism to convert an object → bytes (serialization) and bytes → object
 * (deserialization). Think of it like JSON.stringify / JSON.parse, but
 * binary and Java-specific. Every DTO that goes over the network needs this.
 * <p>
 * WHY `serialVersionUID`?
 * This is a version number for the serialized format. If you later change
 * the DTO (add a field, rename something), you increment this number.
 * If the server sends version 1 but the client expects version 2, Java
 * throws an InvalidClassException instead of silently corrupting data.
 * The @Serial annotation tells the compiler "this field is part of
 * serialization, please warn me if I mess it up."
 * <p>
 * WHAT IS A `record`? (Java 25 feature, no TS equivalent)
 * A record is a CONCISE class that automatically generates:
 * - A constructor with all fields as parameters
 * - Public accessor methods (e.g., playerId() returns the playerId)
 * - equals() and hashCode() based on ALL fields (like deep comparison)
 * - toString() showing all fields (like JSON.stringify but for Java)
 */
public record WelcomeDTO(int playerId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
