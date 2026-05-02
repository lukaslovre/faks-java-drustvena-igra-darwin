package hr.tvz.darwin.shared.dto;

import hr.tvz.darwin.shared.Island;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a single worker token on the board.
 * <p>
 * In JS you'd model this as a plain object:
 * { id: 0, level: 1, currentIsland: null }
 * <p>
 * KEY DESIGN DECISION — `currentIsland` is nullable:
 * - null = the worker is at the player's base (not placed on any island)
 * - non-null = the worker is currently sitting on that island
 * <p>
 * Java's Optional<T> exists but is NOT Serializable (can't go over TCP),
 * and Java records don't support null in their compact constructor well.
 * So we use nullable Island — it's the standard pattern for network DTOs.
 * <p>
 * NESTED DTOs:
 * This DTO references `Island` (an enum). Since Java enums implement
 * Serializable by default, this works seamlessly over TCP. If Island
 * were a regular class, it would also need to implement Serializable
 * for this to work — the entire object graph must be serializable.
 * <p>
 * RECORDS CANNOT EXTEND OTHER CLASSES:
 * Every record already implicitly extends java.lang.Record.
 * Java has single inheritance (like TS but enforced), so a record
 * can only implement interfaces, not extend a parent class.
 * WorkerDTO extends SomeBaseClass   // ❌ COMPILE ERROR
 * WorkerDTO implements Serializable  // ✅ OK — interface, not class
 * This is fine for DTOs — they're data carriers, not behavior hierarchies.
 * <p>
 * RECORDS CANNOT HAVE INSTANCE FIELDS IN THE BODY:
 * All fields of a record MUST be declared in the header (the parenthesized
 * part after the name). You cannot add extra fields inside the braces:
 * public record WorkerDTO(...) {
 * int extraField;  // ❌ COMPILE ERROR — records don't allow this
 * }
 * You CAN add static fields, static methods, and instance methods
 * that derive their value from the header fields.
 */
public record WorkerDTO(int id, int level, Island currentIsland) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
