package hr.tvz.darwin.server.core;

/**
 * Thrown by the GameEngine when a business rule is violated.
 */
public class InvalidMoveException extends Exception {
    public InvalidMoveException(String message) {
        super(message);
    }
}