package backgammon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static final double DEFAULT_WIDTH = 1512;
    private static final double DEFAULT_HEIGHT = 982;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ui/LobbyView.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

        stage.setTitle("Backgammon Lobby");
        stage.setScene(scene);

        stage.setMinWidth(DEFAULT_WIDTH);
        stage.setMinHeight(DEFAULT_HEIGHT);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}