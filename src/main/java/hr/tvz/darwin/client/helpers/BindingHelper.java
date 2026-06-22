package hr.tvz.darwin.client.helpers;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.util.EnumMap;
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
    private final Map<Track, Label> trackLabels;

    private final Map<Island, double[]> islandPositions = new EnumMap<>(Island.class);

    public BindingHelper(ProgressBar botanyProgress, ProgressBar zoologyProgress,
                         ProgressBar geologyProgress, Map<Track, Label> trackLabels) {
        this.botanyProgress = botanyProgress;
        this.zoologyProgress = zoologyProgress;
        this.geologyProgress = geologyProgress;
        this.trackLabels = trackLabels;

        // Centers of the island buttons in the board coordinate system.
        islandPositions.put(Island.ISABELA, new double[]{255.0, 107.0});
        islandPositions.put(Island.SANTA_CRUZ, new double[]{255.0, 183.0});
        islandPositions.put(Island.SAN_CRISTOBAL, new double[]{255.0, 259.0});
    }

    public void updateProgressBars(PlayerStateDTO player) {
        botanyProgress.setProgress(clampProgress(player.botany()));
        zoologyProgress.setProgress(clampProgress(player.zoology()));
        geologyProgress.setProgress(clampProgress(player.geology()));
        updateTrackLabels(player.botany(), player.zoology(), player.geology());
    }

    /** Clears live values before a validated replay reconstructs local research. */
    public void resetProgressBars() {
        botanyProgress.setProgress(0);
        zoologyProgress.setProgress(0);
        geologyProgress.setProgress(0);
        updateTrackLabels(0, 0, 0);
    }

    /** Updates exactly one replay track without disturbing the other two. */
    public void updateTrack(Track track, int value) {
        ProgressBar progressBar = switch (track) {
            case BOTANY -> botanyProgress;
            case ZOOLOGY -> zoologyProgress;
            case GEOLOGY -> geologyProgress;
        };
        progressBar.setProgress(clampProgress(value));
        trackLabels.get(track).setText(formatTrack(track, value));
    }

    public double[] getIslandPosition(Island island) {
        return islandPositions.get(island);
    }

    private static double clampProgress(int value) {
        return Math.clamp(value / MAX_TRACK, 0.0, 1.0);
    }

    private void updateTrackLabels(int botany, int zoology, int geology) {
        trackLabels.get(Track.BOTANY).setText(formatTrack(Track.BOTANY, botany));
        trackLabels.get(Track.ZOOLOGY).setText(formatTrack(Track.ZOOLOGY, zoology));
        trackLabels.get(Track.GEOLOGY).setText(formatTrack(Track.GEOLOGY, geology));
    }

    private static String formatTrack(Track track, int value) {
        String name = track.name().charAt(0) + track.name().substring(1).toLowerCase();
        return name + "  " + value + " / " + (int) MAX_TRACK;
    }
}
