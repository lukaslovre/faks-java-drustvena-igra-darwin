package hr.tvz.darwin.client.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

import java.net.URL;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // placeholder: log that the UI is ready
        chatHistoryArea.appendText("Game board ready.\n");
    }

    @FXML
    private void onIslandClick(ActionEvent event) {
        Button source = (Button) event.getSource();
        String islandId = source.getId();
        chatHistoryArea.appendText("Clicked: " + islandId + "\n");
    }

    @FXML
    private void onSendMessage(ActionEvent event) {
        String message = chatInputField.getText();
        if (message == null || message.isBlank()) return;
        chatHistoryArea.appendText("You: " + message + "\n");
        chatInputField.clear();
    }
}
