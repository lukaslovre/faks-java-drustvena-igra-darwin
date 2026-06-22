package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import javafx.application.Platform;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

/**
 * Consumes parsed moves on a virtual thread. All scene graph access is posted
 * to the JavaFX Application Thread with {@link Platform#runLater(Runnable)}.
 */
public class ReplayEngine {
    private static final long MOVE_DELAY_MS = 1500;

    private final AnimationHelper animationHelper;

    // outer key = playerId, inner key = workerId, value = the JavaFX Circle Node
    private final Map<Integer, Map<Integer, Circle>> workerCircles;
    private final Function<Island, double[]> islandPositions;

    // stop() writes on the FX thread while the replay thread reads this flag.
    private volatile boolean running = false;

    public ReplayEngine(AnimationHelper animationHelper,
                        Map<Integer, Map<Integer, Circle>> workerCircles,
                        java.util.function.Function<Island, double[]> islandPositions) {
        this.animationHelper = animationHelper;
        this.workerCircles = workerCircles;
        this.islandPositions = islandPositions;
    }

    /** Starts one replay and posts its completion callback to the FX thread. */
    public void startReplay(Queue<MoveRequestDTO> moves, Runnable onComplete) {
        if (running) return;
        running = true;

        Thread.ofVirtual().start(() -> {
            // Replay XML stores moves rather than resulting worker levels.
            Map<String, Integer> workerLevels = new HashMap<>();
            workerLevels.put("1-0", 1);
            workerLevels.put("1-1", 1);
            workerLevels.put("2-0", 1);
            workerLevels.put("2-1", 1);

            for (MoveRequestDTO move : moves) {
                if (!running) break;

                String key = move.playerId() + "-" + move.workerId();
                int currentLevel = workerLevels.getOrDefault(key, 1);
                int newLevel = Math.min(currentLevel + 1, 3);
                workerLevels.put(key, newLevel);

                // The animation itself is non-blocking: 
                // playTurnAnimation() starts a SequentialTransition and returns immediately.
                Platform.runLater(() -> {
                    Circle worker = workerCircles
                            .get(move.playerId())
                            .get(move.workerId());
                    if (worker == null) return;
                    double[] pos = islandPositions.apply(move.targetIsland());
                    animationHelper.playTurnAnimation(
                            worker, pos[0], pos[1], newLevel, null);
                });

                try {
                    Thread.sleep(MOVE_DELAY_MS);
                } catch (InterruptedException e) {
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
     * Gracefully signals the replay loop to exit after the current animation finishes.
     */
    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
