package hr.tvz.darwin.server;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.GameStateDTO;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import hr.tvz.darwin.shared.dto.WorkerDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies a complete Java serialization round trip: write a game state to disk,
 * read it back, and compare the restored object with the original.
 *
 * No mocking is used. The test exercises the real object streams and filesystem,
 * while JUnit's {@code @TempDir} provides an isolated directory that is deleted
 * after the test. This avoids creating or overwriting the production save file.
 */
class GameStateSerializerTest {

    @Test
    void roundTripPreservesCompleteGameState(@TempDir Path tempDirectory) throws Exception {
        Path savePath = tempDirectory.resolve("latest-game.ser");
        GameStateSerializer serializer = new GameStateSerializer(savePath);

        // Use non-default values throughout the nested DTO graph so missing fields
        // cannot accidentally pass because both the original and restored values are zero.
        GameStateDTO expected = new GameStateDTO(
                new PlayerStateDTO(2, 1, 5,
                        new WorkerDTO(0, 3, Island.SAN_CRISTOBAL),
                        new WorkerDTO(1, 2, Island.SANTA_CRUZ)),
                new PlayerStateDTO(4, 2, 1,
                        new WorkerDTO(0, 3, Island.ISABELA),
                        new WorkerDTO(1, 1, null)),
                2,
                1,
                new MoveRequestDTO(1, 0, Island.SAN_CRISTOBAL)
        );

        serializer.save(expected);
        GameStateDTO actual = serializer.load();

        assertTrue(Files.size(savePath) > 0, "Serialized save should not be empty");
        // Java records implement value-based equals(), including their nested records.
        assertEquals(expected, actual);
    }
}
