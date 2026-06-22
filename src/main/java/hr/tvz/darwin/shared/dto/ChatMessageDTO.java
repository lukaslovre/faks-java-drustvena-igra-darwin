package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/** Client → Server → all clients: one chat message. */
public record ChatMessageDTO(int playerId, String message) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
