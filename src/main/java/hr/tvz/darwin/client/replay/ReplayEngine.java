package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import javafx.application.Platform;
import javafx.scene.shape.Circle;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Producer-consumer engine that replays historical moves onto the JavaFX scene
 * graph with a fixed delay between each move.
 *
 * <h3>Architecture — Producer / Consumer</h3>
 * The {@link SaxReplayParser} (producer) fills the queue synchronously. This
 * class consumes the queue on a dedicated Virtual Thread, one move at a time.
 *
 * <h3>UI thread safety</h3>
 * JavaFX components (Nodes, Stage, Scene) are NOT thread-safe. Only the
 * JavaFX Application Thread may touch them. {@link Platform#runLater(Runnable)}
 * is the bridge. Every call that reads or writes a {@link Circle} must be wrapped
 * in it, or the JVM may crash with an {@code IllegalStateException}.
 */
public class ReplayEngine {
    private static final long MOVE_DELAY_MS = 1500;

    private final AnimationHelper animationHelper;

    // Nested Map: outer key = playerId, inner key = workerId, value = the
    // JavaFX Circle Node that represents that worker on screen.
    // JS equivalent: Map<number, Map<number, Circle>> =
    //   { 1: { 0: <Circle>, 1: <Circle> }, 2: { ... } }
    private final Map<Integer, Map<Integer, Circle>> workerCircles;

    // Functional interface (Java's version of a callback type alias).
    // In TS: type PositionProvider = (island: Island) => [number, number]
    private final Function<Island, double[]> islandPositions;

    /**
     * {@code volatile} guarantees that writes to {@code running} by one
     * thread are immediately visible to every other thread. Without it the
     * JVM is allowed to cache the value per-core, which means
     * {@link #stop()} might never be "seen" by the replay loop.
     *
     * JS analogy: like a {@code SharedArrayBuffer} with
     * {@code Atomics.load()} / {@code Atomics.store()} — skip the CPU cache.
     */
    private volatile boolean running = false;

    public ReplayEngine(AnimationHelper animationHelper,
                        Map<Integer, Map<Integer, Circle>> workerCircles,
                        java.util.function.Function<Island, double[]> islandPositions) {
        this.animationHelper = animationHelper;
        this.workerCircles = workerCircles;
        this.islandPositions = islandPositions;
    }

    /**
     * Starts replaying the given move queue on a new Virtual Thread.
     *
     * <h3>Virtual Threads (Java 21+)</h3>
     * {@code Thread.ofVirtual().start(...)} spawns a lightweight thread
     * managed by the JVM rather than the OS. They're cheap (millions can
     * coexist) and block without tying up an OS carrier thread. Conceptually
     * they're the closest Java gets to Go's goroutines or JS's async
     * functions — except they use real blocking calls instead of
     * {@code await}.
     *
     * @param moves      the queue produced by {@link SaxReplayParser}
     * @param onComplete callback posted to the JavaFX thread after the last
     *                   move finishes; {@code null} is safe (no-op)
     */
    public void startReplay(Queue<MoveRequestDTO> moves, Runnable onComplete) {
        if (running) return; // guard: no overlapping replays
        running = true;

        Thread.ofVirtual().start(() -> {

            // Track each worker's level locally because the replay XML only
            // stores what *happened* (Move), not the resulting game state.
            // Key format: "playerId-workerId", e.g. "1-0".  All workers
            // start at level 1 (the game default).
            //
            // ConcurrentHashMap is used not because multiple threads write
            // here (only the replay thread does), but as a defensive habit
            // when the map is inside a Virtual Thread lambda — it costs
            // virtually nothing and prevents future bugs if the engine is
            // extended with concurrent access.
            Map<String, Integer> workerLevels = new ConcurrentHashMap<>();
            workerLevels.put("1-0", 1);
            workerLevels.put("1-1", 1);
            workerLevels.put("2-0", 1);
            workerLevels.put("2-1", 1);

            for (MoveRequestDTO move : moves) {
                if (!running) break; // allows stop() to cleanly halt mid-replay

                String key = move.playerId() + "-" + move.workerId();
                int currentLevel = workerLevels.getOrDefault(key, 1);
                int newLevel = Math.min(currentLevel + 1, 3); // cap at level 3
                workerLevels.put(key, newLevel);

                // PLATFORM BRIDGE — every JavaFX Node touch must happen on
                // the FX Application Thread.  Platform.runLater() schedules
                // a Runnable to execute on that thread.  The animation
                // itself is non-blocking: playTurnAnimation() starts a
                // SequentialTransition and returns immediately.
                Platform.runLater(() -> {
                    Circle worker = workerCircles
                            .get(move.playerId())
                            .get(move.workerId());
                    if (worker == null) return;
                    double[] pos = islandPositions.apply(move.targetIsland());
                    animationHelper.playTurnAnimation(
                            worker, pos[0], pos[1], newLevel, null);
                });

                // Pause between moves so the user can watch.
                // Thread.sleep() on a Virtual Thread is lightweight — it
                // releases the underlying OS carrier thread for other work.
                // In a traditional OS thread this would be wasteful; on a
                // Virtual Thread it's the idiomatic way to introduce a delay.
                try {
                    Thread.sleep(MOVE_DELAY_MS);
                } catch (InterruptedException e) {
                    // InterruptedException is Java's cooperative cancellation
                    // signal (like AbortSignal in JS fetch). We MUST either
                    // re-interrupt the thread or exit cleanly — swallowing
                    // interrupts breaks cancellation semantics higher up.
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            running = false;
            if (onComplete != null) {
                Platform.runLater(onComplete);
            }
        });
    }

    /**
     * Gracefully signals the replay loop to exit after the current
     * animation finishes. Because {@code running} is {@code volatile}, the
     * change is visible to the Virtual Thread on its next loop iteration.
     */
    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}