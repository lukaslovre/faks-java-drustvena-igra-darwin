package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * The MASTER state object — the entire game in one record.
 * <p>
 * This is what the Server broadcasts to ALL clients after every valid move.
 * The client replaces its entire local state with this.
 * <p>
 * winnerId CONVENTION:
 * 0 = game is still ongoing (no winner yet)
 * 1 = Player 1 has won (reached Level 5 on any track)
 * 2 = Player 2 has won
 * <p>
 * WHY NOT USE AN ENUM FOR winnerId?
 * An Optional<PlayerId> would be "cleaner" but Optional isn't Serializable.
 * A nullable Integer works but primitives are cheaper on the wire.
 * Using 0 as "no winner" is the simplest convention that works.
 * <p>
 * IMMUTABILITY:
 * Java records are inherently immutable — you cannot change their fields
 * after construction (like `const` in JS, but enforced by the compiler).
 * The Server creates a NEW GameStateDTO every time the state changes,
 * rather than mutating an existing one. This prevents bugs where one part
 * of the code modifies the state and another part doesn't see the change.
 * <p>
 * COMPACT CANONICAL CONSTRUCTOR (advanced record feature):
 * Records let you add validation inside the constructor without rewriting
 * all the parameters. This is called a "compact constructor":
 * <p>
 * public GameStateDTO {
 * if (winnerId < 0 || winnerId > 2) {
 * throw new IllegalArgumentException("winnerId must be 0, 1, or 2");
 * }
 * }
 * <p>
 * Notice there are NO parentheses or parameter list — Java automatically
 * fills those in from the record header. You only write the validation.
 * This is like a TypeScript constructor with `this.field = field` being
 * auto-generated, and you only writing the guard clauses.
 */
public record GameStateDTO(
        PlayerStateDTO player1,
        PlayerStateDTO player2,
        int activePlayerId,
        int winnerId
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
