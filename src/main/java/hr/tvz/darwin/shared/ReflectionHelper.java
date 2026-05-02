package hr.tvz.darwin.shared;

import java.lang.reflect.Field;

/**
 * Reads @GameRule annotations at runtime using Java Reflection.
 * <p>
 * Opens up the Island enum class, finds all fields tagged with @GameRule,
 * reads their description strings, and prints a "Help Manual".
 * <p>
 * HOW REFLECTION WORKS:
 * 1. `Island.class` → gets the Class object (like `Island` constructor in JS,
 * but it represents the *type itself*, not an instance)
 * 2. `.getDeclaredFields()` → returns all fields declared in this class
 * (enum constants are fields, so ISABELA, SANTA_CRUZ, SAN_CRISTOBAL show up)
 * 3. `.getAnnotation(GameRule.class)` → checks if that field has our annotation
 * 4. `.description()` → reads the string value we put in the annotation
 * <p>
 * GETDECLAREDFIELDS() vs GETFIELDS() — A COMMON CONFUSION:
 * getDeclaredFields() = "give me ALL fields, including private ones,
 * declared directly in THIS class" (the one we want — catches @GameRule)
 * getFields() = "give me only PUBLIC fields, including inherited ones
 * from parent classes" (would miss enum constants since they're tricky)
 * For enum reflection, always use getDeclaredFields().
 * <p>
 * FIELD OBJECTS ARE POWERFUL TOOLS:
 * A Field object isn't just for checking annotations — you can also
 * READ and WRITE the field's value on any instance:
 * Island isabela = Island.ISABELA;
 * int level = (int) field.get(isabela);      // read requiredLevel
 * field.set(isabela, 5);                      // write (if not final)
 * This is how dependency injection frameworks (like Spring) work
 * internally — they use reflection to "inject" values into private fields
 * that you never declared a setter for.
 */
public final class ReflectionHelper {

    private ReflectionHelper() {
        // Utility class — prevent instantiation.
        // In JS you'd just export functions; in Java, making the constructor
        // private is the standard pattern for "this class only has static methods."
    }

    /**
     * Scans the Island enum for @GameRule annotations and prints
     * a formatted help manual to the console.
     */
    public static void printGameRules() {
        System.out.println("=== Darwin's Journey — Rule Manual (via Reflection) ===");
        System.out.println();

        // Island.class is the "class literal" — it gives us the Class object
        // representing the Island enum type itself (not an instance of it)
        Field[] fields = Island.class.getDeclaredFields();

        for (Field field : fields) {
            // .getAnnotation() retrieves the actual annotation instance
            // so we can read its properties (like .description())
            GameRule rule = field.getAnnotation(GameRule.class);

            if (rule == null) continue;

            // field.getName() gives us the enum constant name (e.g. "ISABELA")
            System.out.printf("  %-15s → %s%n", field.getName(), rule.description());

        }

        System.out.println();
        System.out.println("=== End of Rule Manual ===");
    }
}
