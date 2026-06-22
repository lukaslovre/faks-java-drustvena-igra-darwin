package hr.tvz.darwin.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionHelperTest {

    @Test
    void generatedManualContainsEveryAnnotatedIsland() {
        String manual = ReflectionHelper.generateGameRules();

        assertAll(
                () -> assertTrue(manual.contains("ISABELA")),
                () -> assertTrue(manual.contains("SANTA CRUZ")),
                () -> assertTrue(manual.contains("SAN CRISTOBAL"))
        );
    }
}
