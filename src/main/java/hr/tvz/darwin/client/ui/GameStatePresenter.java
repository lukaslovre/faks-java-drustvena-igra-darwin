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
    private final GameStatusView statusView;

    // This state is confined to the JavaFX Application Thread.
    private boolean animating;
    private GameStateDTO pendingState;

    public GameStatePresenter(BindingHelper bindingHelper,
                              AnimationHelper animationHelper,
                              Map<Integer, Map<Integer, Circle>> workerCircles,
                              GameStatusView statusView) {
        this.bindingHelper = bindingHelper;
        this.animationHelper = animationHelper;
        this.workerCircles = workerCircles;
        this.statusView = statusView;
    }

    public void present(GameStateDTO state, int localPlayerId,
                        Consumer<GameStateDTO> updatePhase,
                        Runnable disableControls) {
        statusView.updateStatus(state, localPlayerId);
        if (state.lastMove() == null) {
            applyState(state, localPlayerId);
            updatePhase.accept(state);
            statusView.showWinnerOnce(state, localPlayerId);
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
        applyState(stateToApply, localPlayerId);
        updatePhase.accept(stateToApply);
        statusView.showWinnerOnce(stateToApply, localPlayerId);
        animating = false;
    }

    private void applyState(GameStateDTO state, int localPlayerId) {
        PlayerStateDTO player = localPlayerId == 1 ? state.player1() : state.player2();
        bindingHelper.updateProgressBars(player);
        statusView.updateWorkers(state);
    }

    private int getWorkerLevel(GameStateDTO state, MoveRequestDTO move) {
        PlayerStateDTO player = move.playerId() == 1 ? state.player1() : state.player2();
        return move.workerId() == 0 ? player.worker0().level() : player.worker1().level();
    }
}
