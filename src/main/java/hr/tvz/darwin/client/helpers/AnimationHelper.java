package hr.tvz.darwin.client.helpers;

import javafx.animation.FillTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Plays a single coordinated animation for one player's turn on the game board.
 * <p>
 * The choreography is a {@link SequentialTransition} of four stages:
 * <ol>
 *   <li><b>go</b> — TranslateTransition: slides the worker Circle toward the target island</li>
 *   <li><b>levelUp</b> — FillTransition: brightens the Circle's fill to reflect the new level</li>
 *   <li><b>pause</b> — PauseTransition: holds on the island for 500ms so the user can see it</li>
 *   <li><b>back</b> — TranslateTransition: resets translateX/Y to 0, snapping the worker home</li>
 * </ol>
 * <p>
 * Thread safety: call from the JavaFX Application Thread only
 * (i.e. inside {@code Platform.runLater()}).
 */
public class AnimationHelper {

    public void playTurnAnimation(Circle worker, double targetX, double targetY,
                                  int newLevel, Runnable onComplete) {
        // Compute displacement from worker's home to target island
        double dx = targetX - worker.getLayoutX();
        double dy = targetY - worker.getLayoutY();

        // Derive a brighter fill color based on the new level
        // Level 2 -> brightness 0.7, Level 3 -> brightness 0.9
        Color originalColor = (Color) worker.getFill();
        Color leveledColor = originalColor.deriveColor(0, 1,
                0.3 + (newLevel * 0.2), 1);

        // Step 1: Slide worker from home toward the target island
        TranslateTransition go = new TranslateTransition(Duration.millis(600), worker);
        go.setByX(dx);
        go.setByY(dy);

        // Step 2: Brighten the worker's fill to indicate the level-up
        FillTransition levelUp = new FillTransition(Duration.millis(300),
                worker, originalColor, leveledColor);

        // Step 3: Pause so the player can see the worker on the island
        PauseTransition pause = new PauseTransition(Duration.millis(500));

        // Step 4: Snap worker back home by resetting translate offset to zero
        TranslateTransition back = new TranslateTransition(Duration.millis(600), worker);
        back.setToX(0);
        back.setToY(0);

        // Fire the callback after the worker returns home
        if (onComplete != null) {
            back.setOnFinished(e -> onComplete.run());
        }

        // Chain all four stages sequentially and play
        SequentialTransition seq = new SequentialTransition(go, levelUp, pause, back);
        seq.play();
    }
}