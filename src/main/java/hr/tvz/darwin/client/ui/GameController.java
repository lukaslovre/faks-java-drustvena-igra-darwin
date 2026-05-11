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

    private enum ClientState { DISCONNECTED, WAITING, PLAYING, GAME_OVER }

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
                case GameStateDTO s -> {
                    // TODO: Should this have a further branch inside? Becase GameStateDTO can also bring game end, right? Analyze this.
                    clientState = ClientState.PLAYING;
                    bindingHelper.updateUI(s);
                    boolean myTurn = s.activePlayerId() == myPlayerId;
                    setButtonsEnabled(myTurn);
                }
                case ErrorDTO e -> {
                    if (e.errorMessage().contains("Opponent disconnected")) {
                        clientState = ClientState.GAME_OVER;
                        disableIslandButtons();
                    }
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
                botanyProgress, zoologyProgress, geologyProgress,
                p1Worker0, p1Worker1, p2Worker0, p2Worker1
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
}
