package hr.tvz.darwin.client.ui;

import hr.tvz.darwin.client.helpers.AnimationHelper;
import hr.tvz.darwin.client.helpers.BindingHelper;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
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

    // Maps JavaFX id to Island enum
    private static final Map<String, Island> BUTTON_TO_ISLAND = Map.of(
            "islandIsabela", Island.ISABELA,
            "islandSantaCruz", Island.SANTA_CRUZ,
            "islandSanCristobal", Island.SAN_CRISTOBAL
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindingHelper = new BindingHelper(
                botanyProgress, zoologyProgress, geologyProgress,
                p1Worker0, p1Worker1, p2Worker0, p2Worker1
        );
        animationHelper = new AnimationHelper();

        // Dummy test: set progress bars to show BindingHelper works
        bindingHelper.updateProgressBars(new PlayerStateDTO(2, 1, 0, null, null));

        chatHistoryArea.appendText("Game board ready. BindingHelper & AnimationHelper loaded.\n");
    }

    @FXML
    private void onIslandClick(ActionEvent event) {
        Button source = (Button) event.getSource();
        chatHistoryArea.appendText("Clicked: " + source.getId() + "\n");

        Island target = BUTTON_TO_ISLAND.get(source.getId());
        if (target == null) {
            chatHistoryArea.appendText("Unknown island: " + source.getId() + "\n");
            return;
        }

        double[] islandPos = bindingHelper.getIslandPosition(target);
        if (islandPos != null) {
            animationHelper.animateTokenToIsland(p1Worker0, islandPos[0], islandPos[1],
                    () -> animationHelper.animateTokenBack(p1Worker0, null));
            chatHistoryArea.appendText("Animating worker to " + target.name() + "\n");
        }
    }

    @FXML
    private void onSendMessage(ActionEvent event) {
        String message = chatInputField.getText();
        if (message == null || message.isBlank()) return;
        chatHistoryArea.appendText("You: " + message + "\n");
        chatInputField.clear();
    }
}