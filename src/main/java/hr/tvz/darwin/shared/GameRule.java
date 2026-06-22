package hr.tvz.darwin.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation that tags game rules onto enum fields.
 *
 * @Retention(RUNTIME) = "keep this annotation around after compilation
 * so we can read it with reflection." The default is CLASS, which would
 * discard it before runtime — that would silently break everything.
 * @Target(FIELD) = "this annotation can only be placed on fields"
 * (enum constants are technically fields in Java, hence why this works).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GameRule {
    String description();
}
