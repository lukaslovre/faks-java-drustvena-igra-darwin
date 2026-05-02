package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Server → Client: "That move was invalid, and here's why."
 * <p>
 * When the server's GameEngine rejects a MoveRequestDTO (wrong turn,
 * worker too low level, etc.), it sends this back to the offending client.
 * The client shows the error message in a popup or status bar.
 * <p>
 * In JS you'd handle this like a fetch() error response:
 * fetch('/api/move', { body: moveRequest })
 * .then(res => { if (!res.ok) return res.json() })  // ← ErrorDTO
 * .catch(err => showError(err.errorMessage))
 * <p>
 * WHY NOT USE JAVA EXCEPTIONS OVER THE NETWORK?
 * Java exceptions ARE Serializable, but sending them over TCP is a
 * terrible practice — the receiving side needs the same class definitions
 * on its classpath, and exceptions carry stack traces (security risk).
 * A simple string message is all we need.
 * <p>
 * STATIC FACTORY METHODS (alternative to constructors):
 * Records can have static methods, just like regular classes.
 * These are like static factory functions in TS:
 * <p>
 * public static ErrorDTO of(String message) {
 * return new ErrorDTO("[ERROR] " + message);
 * }
 * <p>
 * Usage: ErrorDTO.of("Worker too low level")
 * vs     new ErrorDTO("Worker too low level")
 * <p>
 * The static factory lets you add pre-processing logic (like adding
 * a prefix) without the caller knowing. You can also use it to return
 * cached instances or different subtypes — like a constructor that
 * has superpowers.
 */
public record ErrorDTO(String errorMessage) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
