package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Client → Server → All Clients: A chat message between players.
 * <p>
 * Flow: Client A sends ChatMessageDTO → Server receives it and
 * broadcasts the SAME DTO to ALL connected clients (including the sender).
 * This way both players see the message appear in their chat UI.
 * <p>
 * RECORDS WITH String FIELDS:
 * String is special in Java — it's the ONLY non-primitive type that
 * works seamlessly with serialization, switch statements, and records.
 * Unlike custom classes, String is guaranteed to be serializable
 * (it implements Serializable internally). No extra work needed.
 */
public record ChatMessageDTO(int playerId, String message) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
