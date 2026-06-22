package hr.tvz.darwin.shared;

/** Island rules are stored with each enum constant and exposed through Help. */
public enum Island {

    @GameRule(description = "Requires Level 1. Rewards +1 Botany.")
    ISABELA(1, Track.BOTANY),

    @GameRule(description = "Requires Level 2. Rewards +1 Zoology.")
    SANTA_CRUZ(2, Track.ZOOLOGY),

    @GameRule(description = "Requires Level 3. Rewards +1 Geology.")
    SAN_CRISTOBAL(3, Track.GEOLOGY);

    public final int requiredLevel;
    public final Track reward;

    Island(int requiredLevel, Track reward) {
        this.requiredLevel = requiredLevel;
        this.reward = reward;
    }
}
