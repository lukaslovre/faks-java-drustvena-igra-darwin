package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.client.helpers.BindingHelper;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

/** Owns replay file selection and replay-specific JavaFX visual state. */
public class ReplayUiCoordinator {
    private final SaxReplayParser parser = new SaxReplayParser();
    private final ReplayEngine replayEngine;
    private final BindingHelper bindingHelper;
    private final Map<Integer, Map<Integer, Circle>> workerCircles;

    public ReplayUiCoordinator(AnimationHelper animationHelper, BindingHelper bindingHelper,
                               Map<Integer, Map<Integer, Circle>> workerCircles,
                               Function<Island, double[]> islandPositions) {
        this.bindingHelper = bindingHelper;
        this.workerCircles = workerCircles;
        this.replayEngine = new ReplayEngine(
                animationHelper, workerCircles, islandPositions);
    }

    /** Opens a replay and reports status on the JavaFX Application Thread. */
    public void watchReplay(Window owner, Consumer<String> report,
                            int localPlayerId, Runnable onStart, Runnable onComplete) {
        if (replayEngine.isRunning()) {
            return;
        }

        FileChooser chooser = createFileChooser();
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }

        try {
            Queue<MoveRequestDTO> moves = parser.parse(file);
            if (moves.isEmpty()) {
                report.accept("[REPLAY] No moves found in file.\n");
                return;
            }

            resetWorkers();
            bindingHelper.resetProgressBars();
            onStart.run();
            report.accept("[REPLAY] Starting replay of " + moves.size() + " moves...\n");
            replayEngine.startReplay(moves, localPlayerId, bindingHelper::updateTrack, () -> {
                report.accept("[REPLAY] Replay finished.\n");
                onComplete.run();
            });
        } catch (Exception exception) {
            report.accept("[REPLAY ERROR] " + exception.getMessage() + "\n");
        }
    }

    private FileChooser createFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Replay File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Replay XML", "*.xml"));
        File replayDirectory = new File("replays");
        if (replayDirectory.isDirectory()) {
            chooser.setInitialDirectory(replayDirectory);
        }
        return chooser;
    }

    /** Clears animation offsets and restores the colors declared in FXML. */
    private void resetWorkers() {
        workerCircles.values().stream()
                .flatMap(workers -> workers.values().stream())
                .forEach(worker -> {
                    worker.setTranslateX(0);
                    worker.setTranslateY(0);
                });
        workerCircles.get(1).values()
                .forEach(worker -> worker.setFill(Color.DODGERBLUE));
        workerCircles.get(2).values()
                .forEach(worker -> worker.setFill(Color.web("#ff3d1f")));
    }
}
