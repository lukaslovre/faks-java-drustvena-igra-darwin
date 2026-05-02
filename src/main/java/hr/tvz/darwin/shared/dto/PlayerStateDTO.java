package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents ONE player's complete state — their research progress and workers.
 * <p>
 * In JS this would be:
 * {
 * botany: 0, zoology: 0, geology: 0,
 * worker0: { id: 0, level: 1, currentIsland: null },
 * worker1: { id: 1, level: 1, currentIsland: null },
 * }
 * <p>
 * WHY NOT USE A `Map<Track, Integer>` INSTEAD OF THREE INT FIELDS?
 * Because HashMap is Serializable but bulky over the network, and
 * we'd need custom serialization logic. Three primitive ints are:
 * - Smaller on the wire (no map overhead)
 * - Type-safe (can't accidentally put a wrong key)
 * - Easier to use in JavaFX bindings later (botanyProperty.set(dto.botany()))
 * With only 3 tracks, a Map is overkill. YAGNI.
 * <p>
 * WHY WORKER0 / WORKER1 INSTEAD OF `List<WorkerDTO>`?
 * Same reasoning — exactly 2 workers means no list needed.
 * A List would also require ArrayList serialization and add overhead.
 * Named fields are clearer and cheaper for our fixed-size game.
 * <p>
 * RECORDS AS BUILDING BLOCKS:
 * PlayerStateDTO contains two WorkerDTO records. This is COMPOSITION —
 * the same pattern as nesting objects in JS. Records compose naturally
 * because they're just regular objects under the hood, but with all the
 * auto-generated boilerplate (equals, hashCode, toString).
 */
public record PlayerStateDTO(
        int botany,
        int zoology,
        int geology,
        WorkerDTO worker0,
        WorkerDTO worker1
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
