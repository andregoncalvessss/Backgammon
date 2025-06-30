package backgammon;

import backgammon.ui.ClienteController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClienteMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente.fxml"));
        Scene scene = new Scene(loader.load());
        ClienteController controller = loader.getController();
        primaryStage.setTitle("Cliente Backgammon");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}