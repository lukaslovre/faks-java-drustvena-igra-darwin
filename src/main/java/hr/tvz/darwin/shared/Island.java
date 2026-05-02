package hr.tvz.darwin.shared;

/**
 * The three Galapagos Islands — the "rules engine" of our simplified game.
 * <p>
 * This is where the game rules live as DATA, not as if/else code.
 * Each Island carries its own requiredLevel and reward Track.
 * The @GameRule annotation lets ReflectionHelper read the rules at runtime
 * and print a "Help Manual" to the console.
 * <p>
 * In JS, you'd model this as a plain object:
 * const ISLANDS = {
 * ISABELA:       { requiredLevel: 1, reward: "BOTANY" },
 * SANTA_CRUZ:    { requiredLevel: 2, reward: "ZOOLOGY" },
 * SAN_CRISTOBAL: { requiredLevel: 3, reward: "GEOLOGY" },
 * }
 * <p>
 * Java enums are different: each constant can pass constructor args
 * that get stored as final fields. This is essentially the same idea,
 * but type-safe and compiled.
 * <p>
 * The fields are `public final` because:
 * - `final` = immutable (can't change after construction) — like `const`
 * - `public` = no getter needed, direct access is idiomatic for Java enums
 * - Enums in Java are implicitly `final` classes (can't be subclassed)
 */
public enum Island {

    @GameRule(description = "Requires Level 1. Rewards +1 Botany.")
    ISABELA(1, Track.BOTANY),   // ← comma separates constants within the list

    @GameRule(description = "Requires Level 2. Rewards +1 Zoology.")
    SANTA_CRUZ(2, Track.ZOOLOGY),

    @GameRule(description = "Requires Level 3. Rewards +1 Geology.")
    SAN_CRISTOBAL(3, Track.GEOLOGY);  // ← semicolon ends the constant list!

    // ⚠ The semicolon above is REQUIRED. In TS/JS, a semicolon after
    //   an array of objects would be a syntax error. In Java enums,
    //   the semicolon tells the compiler: "constants are done, now here
    //   come the fields and methods." Forget it and Java refuses to compile.

    public final int requiredLevel;
    public final Track reward;

    /**
     * Enum constructor. Called automatically when the JVM loads this class.
     * You never call this yourself — it's like how JS class constructors
     * run when you define class fields.
     * <p>
     * ENUM CONSTRUCTORS ARE IMPLICITLY `private`:
     * You CANNOT write `new Island(1, Track.BOTANY)` anywhere in your code.
     * The only instances of an enum are the constants you define (ISABELA,
     * SANTA_CRUZ, SAN_CRISTOBAL). Java enforces this at compile time.
     * This is what makes enums true singletons — no external code can
     * create additional instances.
     * <p>
     * In JS, you'd simulate this with a frozen object or a class that
     * throws in the constructor. Java just makes it a language rule.
     */
    Island(int requiredLevel, Track reward) {
        this.requiredLevel = requiredLevel;
        this.reward = reward;
    }
}
