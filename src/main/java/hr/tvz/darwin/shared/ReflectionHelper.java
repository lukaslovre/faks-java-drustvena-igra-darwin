package hr.tvz.darwin.shared;

import java.lang.reflect.Field;

/** 
 * Reads @GameRule annotations at runtime using Java Reflection.
 * <p>
 * Opens up the Island enum class, finds all fields tagged with @GameRule,
 * reads their description strings, and generates a "Help Manual".
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
 */
public final class ReflectionHelper {

    private ReflectionHelper() {
        // Utility class; no instances are needed.
    }

    public static String generateGameRules() {
        StringBuilder manual = new StringBuilder("Darwin's Journey — Rule Manual\n\n");
        Field[] fields = Island.class.getDeclaredFields();

        for (Field field : fields) {
            // .getAnnotation() retrieves the actual annotation instance
            // so we can read its properties (like .description())
            GameRule rule = field.getAnnotation(GameRule.class);

            if (rule != null) {
                manual.append(formatIslandName(field.getName()))
                        .append(" — ")
                        .append(rule.description())
                        .append(System.lineSeparator());
            }
        }
        return manual.toString();
    }

    private static String formatIslandName(String enumConstantName) {
        return enumConstantName.replace('_', ' ');
    }
}
