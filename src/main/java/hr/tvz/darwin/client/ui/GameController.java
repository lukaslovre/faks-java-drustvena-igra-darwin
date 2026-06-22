package hr.tvz.darwin.client.ui;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.client.helpers.BindingHelper;
import hr.tvz.darwin.client.network.ArchiveClient;
import hr.tvz.darwin.client.network.TcpClient;
import hr.tvz.darwin.client.replay.ReplayUiCoordinator;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.ReflectionHelper;
import hr.tvz.darwin.shared.dto.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
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

    @FXML private ProgressBar botanyProgress;
    @FXML private ProgressBar zoologyProgress;
    @FXML private ProgressBar geologyProgress;
    @FXML private TextArea chatHistoryArea;
    @FXML private TextField chatInputField;
    @FXML private Button islandIsabela;
    @FXML private Button islandSantaCruz;
    @FXML private Button islandSanCristobal;
    @FXML private Circle p1Worker0;
    @FXML private Circle p1Worker1;
    @FXML private Circle p2Worker0;
    @FXML private Circle p2Worker1;

    private BindingHelper bindingHelper;
    private TcpClient tcpClient;
    private ArchiveClient archiveClient;
    private ReplayUiCoordinator replayCoordinator;
    private GameStatePresenter gameStatePresenter;
    private int myPlayerId = -1;
    private ClientState clientState = ClientState.DISCONNECTED;

    // Needed to restore controls after the server rejects an optimistic move.
    private boolean isMyTurn = false;

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
                case GameStateDTO state -> gameStatePresenter.present(
                        state, myPlayerId, this::updateGamePhase, this::disableIslandButtons);
                case ErrorDTO e when e.errorMessage().contains("Opponent disconnected") -> {
                    clientState = ClientState.GAME_OVER;
                    disableIslandButtons();
                    chatHistoryArea.appendText("[SERVER] " + e.errorMessage() + "\n");
                }
                case ErrorDTO e when clientState == ClientState.PLAYING -> {
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
        AnimationHelper animationHelper = new AnimationHelper();
        Map<Integer, Map<Integer, Circle>> workers = Map.of(
                1, Map.of(0, p1Worker0, 1, p1Worker1),
                2, Map.of(0, p2Worker0, 1, p2Worker1));
        archiveClient = new ArchiveClient();
        gameStatePresenter = new GameStatePresenter(bindingHelper, animationHelper, workers);
        replayCoordinator = new ReplayUiCoordinator(animationHelper, workers,
                island -> bindingHelper.getIslandPosition(island));
        disableIslandButtons();
        chatHistoryArea.appendText("Darwin's Journey — connecting to server...\n");
    }

    @FXML
    private void onIslandClick(ActionEvent event) {
        if (clientState != ClientState.PLAYING || tcpClient == null) return;
        Button source = (Button) event.getSource();
        Island target = BUTTON_TO_ISLAND.get(source.getId());
        if (target == null) return;

        // Prevent duplicate requests while the authoritative server validates the move.
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

    @FXML
    private void onShowHelp() {
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("Darwin's Journey Help");
        helpAlert.setHeaderText("Island rules generated with Java Reflection");
        helpAlert.setContentText(ReflectionHelper.generateGameRules());
        helpAlert.initOwner(chatHistoryArea.getScene().getWindow());
        helpAlert.show();
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

    @FXML
    private void onShowGlobalArchive() {
        archiveClient.fetchStats(
                stats -> Platform.runLater(() -> chatHistoryArea.appendText(
                        "[ARCHIVE] Global Stats -> Games Played: " + stats.totalGames()
                                + " | Total Research Points: " + stats.totalPoints() + "\n")),
                error -> Platform.runLater(() -> chatHistoryArea.appendText(
                        "[ARCHIVE ERROR] Could not retrieve archive stats: "
                                + error.getMessage() + "\n")));
    }

    @FXML
    private void onWatchReplay() {
        replayCoordinator.watchReplay(
                chatHistoryArea.getScene().getWindow(),
                chatHistoryArea::appendText,
                this::disableIslandButtons,
                () -> {
                    if (clientState == ClientState.PLAYING) {
                        setButtonsEnabled(isMyTurn);
                    }
                });
    }
}
