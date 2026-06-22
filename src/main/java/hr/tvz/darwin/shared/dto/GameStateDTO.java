package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Complete authoritative match state. Records make the state shallowly
 * immutable, so the server publishes a new DTO instead of mutating one.
 * {@code winnerId == 0} means that the match is still active.
 */
public record GameStateDTO(
        PlayerStateDTO player1,
        PlayerStateDTO player2,
        int activePlayerId,
        int winnerId,
        MoveRequestDTO lastMove
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * COMPACT CANONICAL CONSTRUCTOR (Java 25 Idiomatic)
     * Records let you add validation inside the constructor without rewriting
     * all the parameters. Notice there are NO parentheses or parameter list —
     * Java automatically fills those in from the record header.
     *
     * This is "Fail-Fast" validation: invalid state can NEVER exist in memory,
     * let alone be sent over the network. Like TypeScript's `asserts` but
     * enforced at runtime by the constructor itself.
     */
    public GameStateDTO {
        if (winnerId < 0 || winnerId > 2) {
            throw new IllegalArgumentException("winnerId must be 0, 1, or 2");
        }
        if (activePlayerId != 1 && activePlayerId != 2) {
            throw new IllegalArgumentException("activePlayerId must be 1 or 2");
        }
    }
}
