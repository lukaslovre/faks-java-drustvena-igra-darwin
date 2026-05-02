package hr.tvz.darwin.shared;

/**
 * The three Research Tracks in our simplified game.
 * <p>
 * In JS/TS this would just be: type Track = "BOTANY" | "ZOOLOGY" | "GEOLOGY"
 * In Java, enums are full-blown classes — they can have fields, methods,
 * and even implement interfaces. Here we keep it simple: just the names.
 * <p>
 * Each Track maps 1:1 to an Island (Isabela → Botany, Santa Cruz → Zoology,
 * San Cristobal → Geology). That mapping lives in the Island enum, not here,
 * because the Island is the one that "knows" what it rewards.
 * <p>
 * JAVA ENUM SINGLETON GUARANTEE:
 * Each enum constant (BOTANY, ZOOLOGY, GEOLOGY) is instantiated EXACTLY ONCE
 * by the JVM when the class is first loaded. There will never be two
 * Track.BOTANY objects. This is like using `const` + `Object.freeze()` in JS,
 * but enforced at the JVM level — perfect for representing a fixed set of
 * possible values.
 * <p>
 * BUILT-IN METHODS (like Array.prototype methods but for enums):
 * Track.values()   → [Track.BOTANY, Track.ZOOLOGY, Track.GEOLOGY]
 * (like Object.values() — returns all constants as an array)
 * Track.valueOf("BOTANY") → Track.BOTANY
 * (like looking up a key in an object — case-sensitive)
 * Track.BOTANY.ordinal() → 0
 * (the 0-based index of this constant in declaration order)
 * Track.BOTANY.name() → "BOTANY"
 * (the exact string of the constant name — like .toString())
 * These come free with every enum, no extra code needed.
 */
public enum Track {
    BOTANY,
    ZOOLOGY,
    GEOLOGY
}
