package backgammon.ui;

import backgammon.rede.Servidor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServidorController {

    @FXML private Label labelIp;
    @FXML private Label labelPorta;
    @FXML private Label labelStatus;
    @FXML private Label labelJogadores; // New label for connected players

    private final int porta = 5555;
    private final List<String> nomesConectados = Collections.synchronizedList(new ArrayList<>());

    @FXML
    public void initialize() {
        labelPorta.setText(String.valueOf(porta));

        Servidor servidor = new Servidor(porta, this::atualizarEstadoJogador);
        servidor.start();

        labelIp.setText(servidor.getIp());
    }

    private void atualizarEstadoJogador(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("DISCONNECTED:")) {
                String nomeJogador = message.substring("DISCONNECTED:".length());
                nomesConectados.remove(nomeJogador);
            } else {
                nomesConectados.add(message);
            }
            labelJogadores.setText("Jogadores conectados: " + String.join(", ", nomesConectados));

            StringBuilder status = new StringBuilder();
            for (String nome : nomesConectados) {
                status.append(nome).append(": conectado ✅\n");
            }
            if (nomesConectados.size() < 2) {
                status.append("À espera do segundo jogador...");
            }
            labelStatus.setText(status.toString().trim());
        });
    }
}