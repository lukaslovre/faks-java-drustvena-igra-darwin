package hr.tvz.darwin.client;

import hr.tvz.darwin.client.network.TcpClient;
import hr.tvz.darwin.client.ui.GameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    private TcpClient tcpClient;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/hr/tvz/darwin/client/ui/Game.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        GameController controller = fxmlLoader.getController();

        try {
            tcpClient = new TcpClient("localhost", 8080);
            tcpClient.connect();
            tcpClient.setOnMessage(controller::handleDTO);
            controller.setTcpClient(tcpClient);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Could not connect to server.");
            alert.setContentText("Please ensure the server is running on localhost:8080.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        stage.setTitle("Darwin's Journey");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (tcpClient != null) {
            tcpClient.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
