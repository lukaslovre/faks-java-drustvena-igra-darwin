package hr.tvz.darwin.client.helpers;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.GameStateDTO;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import hr.tvz.darwin.shared.dto.WorkerDTO;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.Map;

/**
 * Unpacks GameStateDTO into JavaFX UI components.
 * <p>
 * In JS/TS you'd just do: setState(dto) and the framework re-renders.
 * In JavaFX, you must manually map each DTO field to its UI widget
 * using Platform.runLater() (see the grid helper methods below).
 * <p>
 * This class holds ZERO logic — it just shuffles data into widgets.
 * All game rules live on the Server (GameEngine), never here.
 */
public class BindingHelper {

    // Max research value before a player wins (matches GameEngine.MAX_TRACK_VALUE)
    private static final double MAX_TRACK = 5.0;

    // The player's own progress bars
    private final ProgressBar botanyProgress;
    private final ProgressBar zoologyProgress;
    private final ProgressBar geologyProgress;

    // The 4 worker Circles (P1: blue, P2: red)
    private final Circle p1Worker0;
    private final Circle p1Worker1;
    private final Circle p2Worker0;
    private final Circle p2Worker1;

    // TODO: Should these location based things be extracted somehow? Or unified? Made dynamic? Or is it not worth it?
    // Island button center positions (X, Y) within the AnchorPane
    // These match the layoutX/layoutY in Game.fxml + button half-width (~30px)
    private final Map<Island, double[]> islandPositions = new HashMap<>();

    // Worker base positions (where they sit when not on an island)
    private final Map<Circle, double[]> basePositions = new HashMap<>();

    public BindingHelper(ProgressBar botanyProgress, ProgressBar zoologyProgress,
                         ProgressBar geologyProgress, Circle p1Worker0,
                         Circle p1Worker1, Circle p2Worker0, Circle p2Worker1) {
        this.botanyProgress = botanyProgress;
        this.zoologyProgress = zoologyProgress;
        this.geologyProgress = geologyProgress;
        this.p1Worker0 = p1Worker0;
        this.p1Worker1 = p1Worker1;
        this.p2Worker0 = p2Worker0;
        this.p2Worker1 = p2Worker1;

        // Island button centers (offset by ~30px for button center)
        islandPositions.put(Island.ISABELA, new double[]{180.0, 102.0});
        islandPositions.put(Island.SANTA_CRUZ, new double[]{168.0, 134.0});
        islandPositions.put(Island.SAN_CRISTOBAL, new double[]{160.0, 166.0});

        // Save initial positions as each worker's "home" at base
        basePositions.put(p1Worker0, new double[]{p1Worker0.getLayoutX(), p1Worker0.getLayoutY()});
        basePositions.put(p1Worker1, new double[]{p1Worker1.getLayoutX(), p1Worker1.getLayoutY()});
        basePositions.put(p2Worker0, new double[]{p2Worker0.getLayoutX(), p2Worker0.getLayoutY()});
        basePositions.put(p2Worker1, new double[]{p2Worker1.getLayoutX(), p2Worker1.getLayoutY()});
    }

    /**
     * Called on every GameStateDTO received from the server.
     * Updates ALL progress bars — both your own and the opponent's (visual only).
     * The opponent's progress bars will be added in Phase 4 when we have both
     * players' data displayed.
     */
    public void updateUI(GameStateDTO state) {
        // Only update OUR progress bars (Player 1's bars for simplicity)
        // In Phase 4, the controller tells us which playerId "we" are
        updateProgressBars(state.player1());
        updateWorkerPositions(state.player1(), state.player2());
    }

    /**
     * Converts the int track values (0-5) to 0.0-1.0 for ProgressBar.
     */
    public void updateProgressBars(PlayerStateDTO player) {
        botanyProgress.setProgress(clampProgress(player.botany()));
        zoologyProgress.setProgress(clampProgress(player.zoology()));
        geologyProgress.setProgress(clampProgress(player.geology()));
    }

    /**
     * Moves worker Circles to their currentIsland location (or back to base).
     * This is a direct jump (no animation) — AnimationHelper handles the smooth
     * transitions over time.
     */
    public void updateWorkerPositions(PlayerStateDTO p1, PlayerStateDTO p2) {
        setWorkerPosition(p1Worker0, p1.worker0());
        setWorkerPosition(p1Worker1, p1.worker1());
        setWorkerPosition(p2Worker0, p2.worker0());
        setWorkerPosition(p2Worker1, p2.worker1());
    }

    private void setWorkerPosition(Circle worker, WorkerDTO dto) {
        if (dto.currentIsland() == null) {
            // Move worker back to its base position
            double[] home = basePositions.get(worker);
            if (home != null) {
                worker.setLayoutX(home[0]);
                worker.setLayoutY(home[1]);
            }
        } else {
            // Move worker to the island's center position
            double[] pos = islandPositions.get(dto.currentIsland());
            if (pos != null) {
                worker.setLayoutX(pos[0]);
                worker.setLayoutY(pos[1]);
            }
        }
    }

    // --- Helper methods for controllers & AnimationHelper ---

    /**
     * Returns the island center position for animation target.
     */
    public double[] getIslandPosition(Island island) {
        return islandPositions.get(island);
    }

    /**
     * Returns the base (home) position for a given worker Circle.
     */
    public double[] getBasePosition(Circle worker) {
        return basePositions.get(worker);
    }

    /**
     * Maps a Track enum to the correct ProgressBar.
     */
    public ProgressBar getProgressBar(Track track) {
        return switch (track) {
            case BOTANY -> botanyProgress;
            case ZOOLOGY -> zoologyProgress;
            case GEOLOGY -> geologyProgress;
        };
    }

    // Clamp int 0..5 to double 0.0..1.0 for ProgressBar
    private static double clampProgress(int value) {
        return Math.max(0.0, Math.min(1.0, value / MAX_TRACK));
    }
}
