package hr.tvz.darwin.client.helpers;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import javafx.scene.control.ProgressBar;

import java.util.HashMap;
import java.util.Map;

/** Maps authoritative state values and island positions to JavaFX controls. 
 * <p>
 * Thread safety: all methods are designed to be called from the JavaFX
 * Application Thread (i.e. inside {@code Platform.runLater()}).
*/
public class BindingHelper {

    // ProgressBar values are normalized to 0.0..1.0; a track wins at 5.
    private static final double MAX_TRACK = 5.0; // (matches GameEngine.MAX_TRACK_VALUE)
    private final ProgressBar botanyProgress;
    private final ProgressBar zoologyProgress;
    private final ProgressBar geologyProgress;

    private final Map<Island, double[]> islandPositions = new HashMap<>();

    public BindingHelper(ProgressBar botanyProgress, ProgressBar zoologyProgress,
                         ProgressBar geologyProgress) {
        this.botanyProgress = botanyProgress;
        this.zoologyProgress = zoologyProgress;
        this.geologyProgress = geologyProgress;

        // Centers of the island buttons in the board coordinate system.
        islandPositions.put(Island.ISABELA, new double[]{180.0, 102.0});
        islandPositions.put(Island.SANTA_CRUZ, new double[]{168.0, 134.0});
        islandPositions.put(Island.SAN_CRISTOBAL, new double[]{160.0, 166.0});
    }

    public void updateProgressBars(PlayerStateDTO player) {
        botanyProgress.setProgress(clampProgress(player.botany()));
        zoologyProgress.setProgress(clampProgress(player.zoology()));
        geologyProgress.setProgress(clampProgress(player.geology()));
    }

    /** Clears live values before a validated replay reconstructs local research. */
    public void resetProgressBars() {
        botanyProgress.setProgress(0);
        zoologyProgress.setProgress(0);
        geologyProgress.setProgress(0);
    }

    /** Updates exactly one replay track without disturbing the other two. */
    public void updateTrack(Track track, int value) {
        ProgressBar progressBar = switch (track) {
            case BOTANY -> botanyProgress;
            case ZOOLOGY -> zoologyProgress;
            case GEOLOGY -> geologyProgress;
        };
        progressBar.setProgress(clampProgress(value));
    }

    public double[] getIslandPosition(Island island) {
        return islandPositions.get(island);
    }

    private static double clampProgress(int value) {
        return Math.max(0.0, Math.min(1.0, value / MAX_TRACK));
    }
}
