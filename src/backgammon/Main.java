package backgammon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 720;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ui/JogoView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

        stage.setTitle("Backgammon");
        stage.setScene(scene);

        // ✅ Define tamanho mínimo (proporcional) e tamanho inicial
        stage.setMinWidth(DEFAULT_WIDTH);
        stage.setMinHeight(DEFAULT_HEIGHT);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);

        // ❌ NÃO usamos stage.setWidth()/setHeight() dinâmico (isso causa lag)
        // ✅ Deixamos o conteúdo interno manter a proporção

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
