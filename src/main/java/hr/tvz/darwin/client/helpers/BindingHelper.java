package hr.tvz.darwin.client.helpers;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import javafx.scene.control.ProgressBar;

import java.util.HashMap;
import java.util.Map;

/**
 * Unpacks a subset of GameStateDTO into JavaFX UI widgets — specifically the
 * research progress bars and island coordinate map.
 * <p>
 * This class has been stripped down after Phase 5: worker token positioning (which
 * used to live here via layoutX/layoutY mutations) was moved to AnimationHelper's
 * translation-based system. Now BindingHelper owns only:
 * <ul>
 *   <li>3 {@link ProgressBar} widgets (botany, zoology, geology)</li>
 *   <li>A lookup map of {@link Island} → [x, y] center coordinates for animation targets</li>
 * </ul>
 * Thread safety: all methods are designed to be called from the JavaFX
 * Application Thread (i.e. inside {@code Platform.runLater()}).
 */
public class BindingHelper {

    // Max research value before a player wins (matches GameEngine.MAX_TRACK_VALUE)
    private static final double MAX_TRACK = 5.0;

    // The player's own progress bars
    private final ProgressBar botanyProgress;
    private final ProgressBar zoologyProgress;
    private final ProgressBar geologyProgress;

    private final Map<Island, double[]> islandPositions = new HashMap<>();

    public BindingHelper(ProgressBar botanyProgress, ProgressBar zoologyProgress,
                         ProgressBar geologyProgress) {
        this.botanyProgress = botanyProgress;
        this.zoologyProgress = zoologyProgress;
        this.geologyProgress = geologyProgress;

        // Island button centers (offset by ~30px for button center)
        islandPositions.put(Island.ISABELA, new double[]{180.0, 102.0});
        islandPositions.put(Island.SANTA_CRUZ, new double[]{168.0, 134.0});
        islandPositions.put(Island.SAN_CRISTOBAL, new double[]{160.0, 166.0});
    }

    public void updateProgressBars(PlayerStateDTO player) {
        botanyProgress.setProgress(clampProgress(player.botany()));
        zoologyProgress.setProgress(clampProgress(player.zoology()));
        geologyProgress.setProgress(clampProgress(player.geology()));
    }

    public double[] getIslandPosition(Island island) {
        return islandPositions.get(island);
    }

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