package hr.tvz.darwin.client.helpers;

import javafx.animation.TranslateTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Triggers JavaFX Transitions for worker token movement and level-up.
 * <p>
 * In JS you'd do: element.animate([{ transform: 'translateX(0)' }, { transform: 'translateX(100px)' }], { duration: 500 })
 * In JavaFX, you create a TranslateTransition object, configure it, and call play().
 * <p>
 * These are "fire-and-forget" — once play() is called, JavaFX runs the animation
 * on its own rendering thread (the JavaFX Application Thread).
 * <p>
 * IMPORTANT (UI Thread Safety):
 * All methods here are designed to be called from Platform.runLater().
 * Never call them from a background/Virtual Thread directly.
 * <p>
 * METHODS:
 * - animateTokenToIsland() — Slides a Circle from its current position to an island
 * - animateTokenBack() — Slides a Circle back to the player's base
 * - animateLevelUp() — Changes the Circle's fill color (lighter shade) to show level up
 * - createDummyTest() — Moves a token in a loop for quick visual testing
 */
public class AnimationHelper {

    // Duration constants — tweak these for pacing
    private static final Duration TRAVEL_TIME = Duration.millis(600);
    private static final Duration RETURN_TIME = Duration.millis(600);

    /**
     * Moves a worker Circle from its current position to an island position.
     * This uses TranslateTransition which applies a TRANSLATION (offset) to the node,
     * NOT an absolute layout change.
     * <p>
     * The caller must pass the total displacement (dx, dy) to move the token.
     * These are computed by the BindingHelper as:
     * dx = islandX - worker.getLayoutX()
     * dy = islandY - worker.getLayoutY()
     *
     * @param worker The Circle node to animate
     * @param toX    Absolute X destination within the parent pane
     * @param toY    Absolute Y destination within the parent pane
     * @param onDone Runnable to execute AFTER the animation finishes (or null)
     */
    public void animateTokenToIsland(Circle worker, double toX, double toY, Runnable onDone) {
        // Calculate the offset needed to reach the target
        double dx = toX - worker.getLayoutX();
        double dy = toY - worker.getLayoutY();

        TranslateTransition tt = new TranslateTransition(TRAVEL_TIME, worker);
        tt.setByX(dx);
        tt.setByY(dy);

        // setOnFinished fires when the animation completes — like .then() in JS Promises
        if (onDone != null) {
            tt.setOnFinished(e -> onDone.run());
        }

        tt.play();
    }

    /**
     * Moves a worker Circle back to its "home" base position.
     * Resets the TranslateTransform to 0,0 which snaps the token back
     * to its original layoutX/layoutY (the base position).
     * <p>
     * Why reset to (0,0)?
     * TranslateTransition adds an OFFSET to the node's layout position.
     * After moving to an island, the token has a non-zero translateX/Y.
     * Animating back to (0,0) "slides it home" to where it started.
     *
     * @param worker The Circle to animate back
     * @param onDone Callback after animation finishes
     */
    public void animateTokenBack(Circle worker, Runnable onDone) {
        TranslateTransition tt = new TranslateTransition(RETURN_TIME, worker);
        // Animate from current offset back to 0,0 (the base position)
        tt.setToX(0);
        tt.setToY(0);

        if (onDone != null) {
            tt.setOnFinished(e -> onDone.run());
        }

        tt.play();
    }

    /**
     * Changes the worker token's fill color to indicate a level-up.
     * <p>
     * Level 1 -> Level 2: lighter shade of the base color
     * Level 2 -> Level 3: even lighter shade (gold tint)
     * <p>
     * In Phase 5, this will be combined with a timeline animation.
     * For now, it's an instant color swap.
     *
     * @param worker   The Circle to re-color
     * @param newLevel The worker's level AFTER leveling up (2 or 3)
     */
    public void animateLevelUp(Circle worker, int newLevel) {
        Color baseColor = (Color) worker.getFill();

        // Derive a lighter shade based on level
        double brightness = 0.3 + (newLevel * 0.2); // Level 2 -> 0.7, Level 3 -> 0.9
        Color leveledColor = baseColor.deriveColor(0, 1, brightness, 1);

        worker.setFill(leveledColor);
    }

    /**
     * Resets the worker's translate offset to (0,0) instantly (no animation).
     * Useful when restoring state from a GameStateDTO (BindingHelper does this).
     */
    public void resetPosition(Circle worker) {
        worker.setTranslateX(0);
        worker.setTranslateY(0);
    }
}
