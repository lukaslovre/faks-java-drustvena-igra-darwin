package hr.tvz.darwin.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test to verify JUnit 5 is wired up correctly.
 * Tests basic properties of the Island enum.
 */
class IslandTest {

    @Test
    void isabelaRequiresLevel1() {
        assertEquals(1, Island.ISABELA.requiredLevel);
    }

    @Test
    void santaCruzRequiresLevel2() {
        assertEquals(2, Island.SANTA_CRUZ.requiredLevel);
    }

    @Test
    void sanCristobalRequiresLevel3() {
        assertEquals(3, Island.SAN_CRISTOBAL.requiredLevel);
    }

    @Test
    void isabelaRewardsBotany() {
        assertEquals(Track.BOTANY, Island.ISABELA.reward);
    }

    @Test
    void santaCruzRewardsZoology() {
        assertEquals(Track.ZOOLOGY, Island.SANTA_CRUZ.reward);
    }

    @Test
    void sanCristobalRewardsGeology() {
        assertEquals(Track.GEOLOGY, Island.SAN_CRISTOBAL.reward);
    }

    @Test
    void thereAreExactlyThreeIslands() {
        assertEquals(3, Island.values().length);
    }
}
