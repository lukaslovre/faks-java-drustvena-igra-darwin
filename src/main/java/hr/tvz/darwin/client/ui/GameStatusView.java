package hr.tvz.darwin.client.ui;

import hr.tvz.darwin.shared.dto.GameStateDTO;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.util.Map;

/** Renders connection, turn, worker, and winner information on the FX thread. */
public class GameStatusView {
    private final Label statusLabel;
    private final Map<Integer, Map<Integer, Label>> workerLabels;
    private int alertedWinnerId;

    public GameStatusView(Label statusLabel,
                          Map<Integer, Map<Integer, Label>> workerLabels) {
        this.statusLabel = statusLabel;
        this.workerLabels = workerLabels;
    }

    public void showConnecting() {
        statusLabel.setText("Connecting to server...");
    }

    public void showWaiting(int playerId) {
        statusLabel.setText("You are Player " + playerId + "  •  Waiting for match to start...");
    }

    public void showOpponentDisconnected() {
        statusLabel.setText("Game ended  •  Opponent disconnected");
    }

    public void updateStatus(GameStateDTO state, int localPlayerId) {
        if (state.winnerId() != 0) {
            String result = state.winnerId() == localPlayerId ? "You won" : "Player " + state.winnerId() + " won";
            statusLabel.setText("Game over  •  " + result);
            return;
        }
        alertedWinnerId = 0;
        String turn = state.activePlayerId() == localPlayerId ? "Your turn" : "Opponent's turn";
        statusLabel.setText("You are Player " + localPlayerId + "  •  " + turn);
    }

    public void updateWorkers(GameStateDTO state) {
        updatePlayerWorkers(1, state.player1());
        updatePlayerWorkers(2, state.player2());
    }

    public void showWinnerOnce(GameStateDTO state, int localPlayerId) {
        if (state.winnerId() == 0 || alertedWinnerId == state.winnerId()) return;
        alertedWinnerId = state.winnerId();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(state.winnerId() == localPlayerId ? "You won!" : "Player " + state.winnerId() + " won!");
        alert.setContentText("The first research track reached 5 points.");
        alert.initOwner(statusLabel.getScene().getWindow());
        alert.show();
    }

    private void updatePlayerWorkers(int playerId, PlayerStateDTO player) {
        workerLabels.get(playerId).get(0).setText("Worker 0 • Level " + player.worker0().level());
        workerLabels.get(playerId).get(1).setText("Worker 1 • Level " + player.worker1().level());
    }
}
