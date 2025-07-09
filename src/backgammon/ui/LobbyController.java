package backgammon.ui;

import backgammon.modelo.JogoBackgammon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

public class LobbyController {

    @FXML
    private Button botaoJogar;

    @FXML
    private Label labelEstadoServidor;

    @FXML
    private Label labelEstadoCliente1;

    @FXML
    private Label labelEstadoCliente2;

    @FXML
    private ImageView backgroundView;

    private JogoBackgammon jogo;

    public void setJogo(JogoBackgammon jogo) {
        this.jogo = jogo;
    }

    @FXML
    public void initialize() {
        // Carrega o fundo
        Image bg = new Image(getClass().getResource("/background.jpg").toExternalForm());
        backgroundView.setImage(bg);
        backgroundView.setPreserveRatio(false);

        // Faz binding ao tamanho da cena
        backgroundView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                backgroundView.fitWidthProperty().bind(newScene.widthProperty());
                backgroundView.fitHeightProperty().bind(newScene.heightProperty());
            }
        });

        // Iniciar texto dos labels
        labelEstadoServidor.setText("Servidor: Disponível");
        labelEstadoCliente1.setText("Cliente 1: A conectar...");
        labelEstadoCliente2.setText("Cliente 2: A conectar...");
    }

    @FXML
    private void onJogarClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("JogoView.fxml"));
            Parent root = loader.load();

            JogoController jogoController = loader.getController();

            // Cria o modelo e passa para o controller
            JogoBackgammon jogo = new JogoBackgammon();
            jogoController.setJogo(jogo);

            Stage stage = (Stage) botaoJogar.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Backgammon - Jogo");

            stage.show();

            jogoController.atualizarVisualizacaoPecas();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Métodos para atualizar estado (útil para ligação de rede)
    public void setEstadoServidor(String texto) {
        labelEstadoServidor.setText(texto);
    }

    public void setEstadoCliente1(String texto) {
        labelEstadoCliente1.setText(texto);
    }

    public void setEstadoCliente2(String texto) {
        labelEstadoCliente2.setText(texto);
    }
}