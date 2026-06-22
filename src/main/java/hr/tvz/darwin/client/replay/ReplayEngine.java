package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import javafx.application.Platform;
import javafx.scene.shape.Circle;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

/**
 * Consumes parsed moves on a virtual thread. All scene graph access is posted
 * to the JavaFX Application Thread with {@link Platform#runLater(Runnable)}.
 */
public class ReplayEngine {
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
    public void startReplay(Queue<MoveRequestDTO> moves, int localPlayerId,
                            ObjIntConsumer<Track> updateResearch,
                            Runnable onComplete) {
        if (running) return;
        running = true;

        Thread.ofVirtual().start(() -> {
            Map<String, Integer> workerLevels = new HashMap<>();
            workerLevels.put("1-0", 1);
            workerLevels.put("1-1", 1);
            workerLevels.put("2-0", 1);
            workerLevels.put("2-1", 1);
            Map<Track, Integer> research = new EnumMap<>(Track.class);

            for (MoveRequestDTO move : moves) {
                if (!running) break;

                String key = move.playerId() + "-" + move.workerId();
                int currentLevel = workerLevels.getOrDefault(key, 1);
                int newLevel = Math.min(currentLevel + 1, 3);
                workerLevels.put(key, newLevel);
                CountDownLatch animationFinished = new CountDownLatch(1);

                Platform.runLater(() -> {
                    try {
                        Circle worker = workerCircles.get(move.playerId()).get(move.workerId());
                        double[] pos = islandPositions.apply(move.targetIsland());
                        animationHelper.playTurnAnimation(worker, pos[0], pos[1], newLevel,
                                () -> finishMove(move, localPlayerId, research,
                                        updateResearch, animationFinished));
                    } catch (RuntimeException _) {
                        animationFinished.countDown();
                    }
                });

                try {
                    // Comparable to awaiting a Promise resolved by the FX animation callback.
                    animationFinished.await();
                } catch (InterruptedException _) {
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

    private void finishMove(MoveRequestDTO move, int localPlayerId,
                            Map<Track, Integer> research,
                            ObjIntConsumer<Track> updateResearch,
                            CountDownLatch animationFinished) {
        try {
            if (move.playerId() == localPlayerId) {
                Track track = move.targetIsland().reward;
                int value = research.merge(track, 1, Integer::sum);
                updateResearch.accept(track, value);
            }
        } finally {
            animationFinished.countDown();
        }
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
