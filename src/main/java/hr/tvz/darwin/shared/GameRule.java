package hr.tvz.darwin.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation that tags game rules onto enum fields.
 * <p>
 * Think of this like a JSDoc @decorator in TypeScript.
 * The key difference: Java annotations can be READ AT RUNTIME
 * via the Reflection API, which is exactly what we need for
 * the "Help Manual" feature.
 *
 * @Retention(RUNTIME) = "keep this annotation around after compilation
 * so we can read it with reflection." The default is CLASS, which would
 * discard it before runtime — that would silently break everything.
 * @Target(FIELD) = "this annotation can only be placed on fields"
 * (enum constants are technically fields in Java, hence why this works).
 * <p>
 * WHY `@interface` AND NOT JUST `interface`?
 * In TS, you write `interface` to describe a shape.
 * In Java, `@interface` is a completely different keyword — it declares
 * an ANNOTATION TYPE, not a regular interface. The leading `@` signals
 * "this is metadata, not behavior." You can think of it as Java's way
 * of saying "this class defines a decorator factory."
 * Regular `interface` in Java is like TS interface: it defines methods
 * that a class must implement. Mixing them up is a common Java gotcha.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GameRule {
    String description();
}
