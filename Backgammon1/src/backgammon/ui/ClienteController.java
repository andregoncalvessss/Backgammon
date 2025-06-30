package backgammon.ui;

import backgammon.rede.Cliente;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClienteController implements Cliente.MessageListener {
    @FXML private TextField campoNome;
    @FXML private Button botaoJogar;
    @FXML private Label labelEstado;
    @FXML private HBox chatBox;
    @FXML private TextField campoChat;
    @FXML private Button botaoEnviarChat;

    private Cliente cliente;
    private String nomeLocal;
    private String cor; // New field to store player color
    private final List<String> messageQueue = new ArrayList<>(); // Message buffer

    @FXML
    public void initialize() {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Inicializando ClienteController");
        cliente = new Cliente();
        cliente.setMessageListener(this);
        cliente.conectar("192.168.195.4", 5555);
        labelEstado.setText("Aguardando conexão ao servidor...");

        new Thread(() -> {
            int tentativas = 0;
            final int maxTentativas = 100;
            while (!cliente.isConectado() && tentativas < maxTentativas) {
                try {
                    Thread.sleep(100);
                    tentativas++;
                } catch (InterruptedException e) {
                    System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro na verificação de conexão: " + e.getMessage());
                }
            }
            Platform.runLater(() -> {
                if (cliente.isConectado()) {
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Conexão com servidor estabelecida com sucesso");
                    labelEstado.setText("Ligado ao servidor. Insere o nome e clica em Jogar.");
                    botaoJogar.setDisable(false);
                } else {
                    System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Falha ao conectar após " + maxTentativas + " tentativas");
                    labelEstado.setText("Falha ao conectar ao servidor. Verifica o IP/porta.");
                    botaoJogar.setDisable(true);
                }
            });
        }).start();

        if (campoChat != null && botaoEnviarChat != null) {
            botaoEnviarChat.setOnAction(e -> enviarMensagemChat());
            campoChat.setOnAction(e -> enviarMensagemChat());
        }
    }

    @Override
    public void onMessageReceived(String mensagem) {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] ClienteController recebeu: " + mensagem);
        Platform.runLater(() -> {
            synchronized (messageQueue) {
                messageQueue.add(mensagem); // Buffer all messages
                if (mensagem.startsWith("INICIAR_JOGO:")) {
                    String[] partes = mensagem.substring("INICIAR_JOGO:".length()).split(",");
                    if (partes.length == 2) {
                        String nome1 = partes[0].trim();
                        String nome2 = partes[1].trim();
                        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando INICIAR_JOGO: " + nome1 + ", " + nome2);
                        abrirInterfaceJogo(nome1, nome2);
                    } else {
                        System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Mensagem INICIAR_JOGO inválida: " + mensagem);
                        labelEstado.setText("Erro: Formato de INICIAR_JOGO inválido.");
                    }
                } else if (mensagem.startsWith("COR:")) {
                    cor = mensagem.substring(4).trim(); // Store player color
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Cor recebida: " + cor);
                    labelEstado.setText("Cor atribuída: " + (cor.equals("BRANCO") ? "Brancas" : "Vermelhas"));
                } else if (mensagem.equals("SORTEIO_VEZ")) {
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] SORTEIO_VEZ recebido");
                    labelEstado.setText("Aguardando sorteio inicial...");
                } else if (mensagem.equals("EMPATE_SORTEIO")) {
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando EMPATE_SORTEIO");
                    labelEstado.setText("Empate no sorteio! Aguardando novo sorteio...");
                } else if (mensagem.startsWith("CHAT:")) {
                    String msg = mensagem.substring("CHAT:".length());
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem de chat recebida: " + msg);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("Mensagem do adversário:");
                    alert.setContentText(msg);
                    alert.showAndWait();
                } else if (mensagem.startsWith("ERRO_CONEXAO:") || mensagem.startsWith("SERVIDOR_LOTADO:")) {
                    System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro de conexão: " + mensagem);
                    labelEstado.setText("Erro: " + mensagem.substring(mensagem.indexOf(":") + 1));
                    botaoJogar.setDisable(true);
                } else if (mensagem != null && !mensagem.isBlank()) {
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem desconhecida armazenada na fila: " + mensagem);
                } else {
                    System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem vazia recebida");
                    labelEstado.setText("Erro: Mensagem vazia do servidor.");
                }
            }
        });
    }

    @FXML
    public void aoClicarJogar() {
        String nome = campoNome.getText().trim();
        if (nome.isEmpty()) {
            System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Nome não preenchido");
            labelEstado.setText("Por favor, insere o teu nome.");
            return;
        }

        if (!cliente.isConectado()) {
            System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente não conectado ao tentar enviar PRONTO");
            labelEstado.setText("Não conectado ao servidor. Tente novamente.");
            return;
        }

        nomeLocal = nome;
        cliente.enviarPronto(nome);
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado PRONTO: " + nome);
        labelEstado.setText("À espera do adversário...");
        botaoJogar.setDisable(true);
        campoNome.setDisable(true);
    }

    private void enviarMensagemChat() {
        String msg = campoChat.getText().trim();
        if (!msg.isEmpty()) {
            cliente.enviarMensagemChat(msg);
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem de chat enviada: " + msg);
            campoChat.clear();
        } else {
            System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Tentativa de enviar mensagem de chat vazia");
        }
    }

    private void abrirInterfaceJogo(String nome1, String nome2) {
        try {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Carregando JogoView.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JogoView.fxml"));
            Scene jogoScene = new Scene(loader.load());

            JogoController controller = loader.getController();
            if (controller == null) {
                System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: JogoController não foi carregado do FXML");
                labelEstado.setText("Erro: Falha ao carregar a interface do jogo.");
                return;
            }

            controller.setCliente(cliente);
            controller.setPlayerNames(nome1, nome2);
            controller.setMeuNome(nomeLocal);
            controller.setCor(cor); // Pass player color
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] JogoController configurado: nomeLocal=" + nomeLocal + ", jogadores=" + nome1 + "," + nome2 + ", cor=" + cor);

            cliente.removeMessageListener();
            cliente.setMessageListener(controller);
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] MessageListener transferido para JogoController");

            // Pass buffered messages to JogoController
            synchronized (messageQueue) {
                for (String msg : new ArrayList<>(messageQueue)) {
                    controller.onMessageReceived(msg);
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Reenviando mensagem para JogoController: " + msg);
                }
                messageQueue.clear();
            }

            Stage stage = (Stage) campoNome.getScene().getWindow();
            stage.setScene(jogoScene);
            stage.setTitle("Backgammon - Jogo");
            stage.centerOnScreen();
            stage.show();
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Interface do jogo exibida com sucesso");
        } catch (IOException e) {
            System.err.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao carregar JogoView.fxml: " + e.getMessage());
            e.printStackTrace();
            labelEstado.setText("Erro ao abrir a interface do jogo: " + e.getMessage());
        }
    }
}