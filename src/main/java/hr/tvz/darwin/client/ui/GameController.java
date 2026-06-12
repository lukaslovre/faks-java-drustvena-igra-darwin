package hr.tvz.darwin.client.ui;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.client.helpers.BindingHelper;
import hr.tvz.darwin.client.network.TcpClient;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    private enum ClientState {DISCONNECTED, WAITING, PLAYING, GAME_OVER}

    @FXML
    private ProgressBar botanyProgress;

    @FXML
    private ProgressBar zoologyProgress;

    @FXML
    private ProgressBar geologyProgress;

    @FXML
    private TextArea chatHistoryArea;

    @FXML
    private TextField chatInputField;

    @FXML
    private Button sendChatBtn;

    @FXML
    private Button islandIsabela;

    @FXML
    private Button islandSantaCruz;

    @FXML
    private Button islandSanCristobal;

    @FXML
    private Circle p1Worker0;

    @FXML
    private Circle p1Worker1;

    @FXML
    private Circle p2Worker0;

    @FXML
    private Circle p2Worker1;

    private BindingHelper bindingHelper;
    private AnimationHelper animationHelper;
    private TcpClient tcpClient;
    private int myPlayerId = -1;
    private ClientState clientState = ClientState.DISCONNECTED;

    // Tracks if it is currently our turn based on the last valid GameStateDTO.
    // Used to recover button state if the server rejects our move.
    private boolean isMyTurn = false;

    // Not volatile: Thread Confinement guarantees this is only ever read/written
    // by the JavaFX Application Thread inside Platform.runLater().
    private boolean isAnimating = false;
    private GameStateDTO pendingState = null;

    private static final Map<String, Island> BUTTON_TO_ISLAND = Map.of(
            "islandIsabela", Island.ISABELA,
            "islandSantaCruz", Island.SANTA_CRUZ,
            "islandSanCristobal", Island.SAN_CRISTOBAL
    );

    public void setTcpClient(TcpClient client) {
        this.tcpClient = client;
    }

    public void handleDTO(Object dto) {
        Platform.runLater(() -> {
            switch (dto) {
                case WelcomeDTO w -> {
                    myPlayerId = w.playerId();
                    clientState = ClientState.WAITING;
                    disableIslandButtons();
                    chatHistoryArea.appendText("Connected as Player " + myPlayerId + ". Waiting for opponent...\n");
                }
                // Java 25 Guard Clauses: 'when' keyword eliminates nested if/else chains
                // The switch becomes 100% declarative - no early returns needed
                case GameStateDTO s when s.lastMove() == null -> {
                    // Initial state from server — no move has been made yet
                    bindingHelper.updateProgressBars(myPlayerId == 1 ? s.player1() : s.player2());
                    updateGamePhase(s);
                }
                case GameStateDTO s when isAnimating -> {
                    // Safety guard: if an animation is still running, stash the latest
                    // state and skip. The animation callback will use pendingState instead
                    // of the original captured state, ensuring no intermediate state is lost.
                    pendingState = s;
                }
                case GameStateDTO s -> {
                    // Normal animation flow: play the move and update UI on completion
                    isAnimating = true;
                    disableIslandButtons();

                    // Extract move data from the state
                    MoveRequestDTO move = s.lastMove();
                    Circle movingWorker = getWorkerCircle(move.playerId(), move.workerId());
                    double[] targetPos = bindingHelper.getIslandPosition(move.targetIsland());
                    WorkerDTO updatedWorker = getWorkerData(s, move.playerId(), move.workerId());

                    // Play the turn animation; on completion, refresh UI with the latest state.
                    // Note: JavaFX Transitions guarantee the callback runs on the FX Thread,
                    // so we do not need to wrap this in Platform.runLater().
                    animationHelper.playTurnAnimation(
                            movingWorker, targetPos[0], targetPos[1], updatedWorker.level(),
                            () -> {
                                GameStateDTO stateToApply = (pendingState != null) ? pendingState : s;
                                pendingState = null;
                                bindingHelper.updateProgressBars(
                                        myPlayerId == 1 ? stateToApply.player1() : stateToApply.player2());
                                updateGamePhase(stateToApply);
                                isAnimating = false;
                            }
                    );
                }
                case ErrorDTO e when e.errorMessage().contains("Opponent disconnected") -> {
                    clientState = ClientState.GAME_OVER;
                    disableIslandButtons();
                    chatHistoryArea.appendText("[SERVER] " + e.errorMessage() + "\n");
                }
                case ErrorDTO e when clientState == ClientState.PLAYING -> {
                    // If it's a normal validation error (e.g., "Worker level too low"),
                    // release the optimistic lock and restore buttons so the user can try again.
                    setButtonsEnabled(isMyTurn);
                    chatHistoryArea.appendText("[SERVER] " + e.errorMessage() + "\n");
                }
                case ErrorDTO e -> {
                    chatHistoryArea.appendText("[SERVER] " + e.errorMessage() + "\n");
                }
                case ChatMessageDTO c -> {
                    chatHistoryArea.appendText("Player " + c.playerId() + ": " + c.message() + "\n");
                }
                default -> chatHistoryArea.appendText("[UNKNOWN] " + dto.getClass().getSimpleName() + "\n");
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindingHelper = new BindingHelper(
                botanyProgress, zoologyProgress, geologyProgress
        );
        animationHelper = new AnimationHelper();
        disableIslandButtons();
        chatHistoryArea.appendText("Darwin's Journey — connecting to server...\n");
    }

    @FXML
    private void onIslandClick(ActionEvent event) {
        if (clientState != ClientState.PLAYING || tcpClient == null) return;
        Button source = (Button) event.getSource();
        Island target = BUTTON_TO_ISLAND.get(source.getId());
        if (target == null) return;

        // OPTIMISTIC LOCK: Disable buttons instantly to prevent double-clicks
        // while waiting for the server to validate the move.
        disableIslandButtons();

        tcpClient.send(new MoveRequestDTO(myPlayerId, 0, target));
        chatHistoryArea.appendText("Sent move: Worker 0 -> " + target.name() + "\n");
    }

    @FXML
    private void onSendMessage(ActionEvent event) {
        if (tcpClient == null) return;
        String message = chatInputField.getText();
        if (message == null || message.isBlank()) return;
        tcpClient.send(new ChatMessageDTO(myPlayerId, message));
        chatInputField.clear();
    }

    private void disableIslandButtons() {
        islandIsabela.setDisable(true);
        islandSantaCruz.setDisable(true);
        islandSanCristobal.setDisable(true);
    }

    private void setButtonsEnabled(boolean enabled) {
        islandIsabela.setDisable(!enabled);
        islandSantaCruz.setDisable(!enabled);
        islandSanCristobal.setDisable(!enabled);
    }

    private Circle getWorkerCircle(int playerId, int workerId) {
        if (playerId == 1) return workerId == 0 ? p1Worker0 : p1Worker1;
        else return workerId == 0 ? p2Worker0 : p2Worker1;
    }

    private WorkerDTO getWorkerData(GameStateDTO state, int playerId, int workerId) {
        PlayerStateDTO p = playerId == 1 ? state.player1() : state.player2();
        return workerId == 0 ? p.worker0() : p.worker1();
    }

    private void updateGamePhase(GameStateDTO s) {
        if (s.winnerId() != 0) {
            clientState = ClientState.GAME_OVER;
            isMyTurn = false;
            disableIslandButtons();
            chatHistoryArea.appendText("GAME OVER! Player " + s.winnerId() + " wins!\n");
        } else {
            clientState = ClientState.PLAYING;
            isMyTurn = (s.activePlayerId() == myPlayerId);
            setButtonsEnabled(isMyTurn);
        }
    }
}
