package hr.tvz.darwin.client.ui;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.client.helpers.BindingHelper;
import hr.tvz.darwin.shared.dto.GameStateDTO;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import javafx.scene.shape.Circle;

import java.util.Map;
import java.util.function.Consumer;

/** Applies authoritative game states to the live board animation and tracks. */
public class GameStatePresenter {
    private final BindingHelper bindingHelper;
    private final AnimationHelper animationHelper;
    private final Map<Integer, Map<Integer, Circle>> workerCircles;

    // This state is confined to the JavaFX Application Thread.
    private boolean animating;
    private GameStateDTO pendingState;

    public GameStatePresenter(BindingHelper bindingHelper,
                              AnimationHelper animationHelper,
                              Map<Integer, Map<Integer, Circle>> workerCircles) {
        this.bindingHelper = bindingHelper;
        this.animationHelper = animationHelper;
        this.workerCircles = workerCircles;
    }

    public void present(GameStateDTO state, int localPlayerId,
                        Consumer<GameStateDTO> updatePhase,
                        Runnable disableControls) {
        if (state.lastMove() == null) {
            updateTracks(state, localPlayerId);
            updatePhase.accept(state);
            return;
        }
        if (animating) {
            // Keep the newest authoritative state until the current animation ends.
            pendingState = state;
            return;
        }

        animating = true;
        disableControls.run();
        MoveRequestDTO move = state.lastMove();
        Circle worker = workerCircles.get(move.playerId()).get(move.workerId());
        double[] target = bindingHelper.getIslandPosition(move.targetIsland());
        int workerLevel = getWorkerLevel(state, move);

        // JavaFX transition callbacks execute on the FX thread.
        animationHelper.playTurnAnimation(worker, target[0], target[1], workerLevel,
                () -> finishAnimation(state, localPlayerId, updatePhase));
    }

    private void finishAnimation(GameStateDTO animatedState, int localPlayerId,
                                 Consumer<GameStateDTO> updatePhase) {
        GameStateDTO stateToApply = pendingState != null ? pendingState : animatedState;
        pendingState = null;
        updateTracks(stateToApply, localPlayerId);
        updatePhase.accept(stateToApply);
        animating = false;
    }

    private void updateTracks(GameStateDTO state, int localPlayerId) {
        PlayerStateDTO player = localPlayerId == 1 ? state.player1() : state.player2();
        bindingHelper.updateProgressBars(player);
    }

    private int getWorkerLevel(GameStateDTO state, MoveRequestDTO move) {
        PlayerStateDTO player = move.playerId() == 1 ? state.player1() : state.player2();
        return move.workerId() == 0 ? player.worker0().level() : player.worker1().level();
    }
}
