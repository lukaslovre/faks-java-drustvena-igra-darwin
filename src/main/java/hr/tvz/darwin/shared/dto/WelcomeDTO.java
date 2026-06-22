package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Server → client handshake sent before the first game state. All network DTOs
 * implement {@link Serializable}; an explicit {@code serialVersionUID} makes
 * incompatible serialized versions fail predictably.
 */
public record WelcomeDTO(int playerId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
