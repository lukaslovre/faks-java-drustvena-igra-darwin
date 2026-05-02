package hr.tvz.darwin.shared.dto;

import hr.tvz.darwin.shared.Island;

import java.io.Serial;
import java.io.Serializable;

/**
 * Client → Server: "I want to move this worker to that island."
 * <p>
 * This is the ONLY input the player sends during their turn.
 * The Server receives this, validates it (is it your turn? is the worker
 * high enough level for that island?), and either:
 * - Accepts → updates game state, broadcasts new GameStateDTO
 * - Rejects → sends back an ErrorDTO explaining why
 * <p>
 * PATTERN MATCHING ON THE RECEIVING END (Java 25 preview):
 * When the server reads this from the TCP stream, it uses the
 * `switch` pattern matching.
 */
public record MoveRequestDTO(int playerId, int workerId, Island targetIsland) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
