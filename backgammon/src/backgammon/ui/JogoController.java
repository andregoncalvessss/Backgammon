package backgammon.ui;

import backgammon.modelo.JogoBackgammon;
import backgammon.modelo.Peca;
import backgammon.modelo.Campo;
import backgammon.rede.Cliente;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class JogoController implements Cliente.MessageListener {
    @FXML
    private TextField campoChat;
    @FXML
    private Button botaoEnviarChat;
    @FXML
    private VBox pecasCapturadasView;
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView backgroundView;
    @FXML
    private Pane aspectWrapper;
    @FXML
    private StackPane tabuleiroContainer;
    @FXML
    private ImageView dado1View;
    @FXML
    private ImageView dado2View;
    @FXML
    private ImageView dado3View;
    @FXML
    private ImageView dado4View;
    @FXML
    private Button botaoLancarDados;
    @FXML
    private Label labelTurno;
    @FXML
    private VBox chatMensagensBox;

    private final GridPane tabuleiro = new GridPane();
    private JogoBackgammon jogo = new JogoBackgammon();
    private final Map<Integer, StackPane> campoPecasMap = new HashMap<>();
    private final Map<Integer, Polygon> campoTrianguloMap = new HashMap<>();
    private Integer campoSelecionado = null;
    private Rectangle bearingOffOutline;

    private Cliente cliente;
    private final Object lock = new Object();
    private volatile boolean minhaVez = false;
    private volatile boolean aguardandoInicio = true;
    private String nomeJogador1;
    private String nomeJogador2;
    private String meuNome;
    private String cor;

    private volatile boolean jogoTerminado = false;
    private String vencedor = null;

    public JogoController() {
        aguardandoInicio = true;
    }

    public void setCliente(Cliente cliente) throws IOException {
        this.cliente = cliente;
        this.cliente.setMessageListener(this);
        Socket socket = cliente.getSocket();
        if (socket != null && socket.isConnected()) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] JogoController conectado ao socket: " + socket.getInetAddress());
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] JogoController conectado ao socket: " + socket.getInetAddress());
        } else {
            throw new IOException("Socket não conectado.");
        }
    }

    public void setPlayerNames(String nome1, String nome2) {
        this.nomeJogador1 = nome1 != null ? nome1.trim() : "";
        this.nomeJogador2 = nome2 != null ? nome2.trim() : "";
        jogo.inicializarJogo(nomeJogador1, nomeJogador2);
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Nomes dos jogadores definidos: Jogador1='" + nomeJogador1 + "', Jogador2='" + nomeJogador2 + "'");
    }

    public void setMeuNome(String nome) {
        this.meuNome = nome != null ? nome.trim() : "";
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Meu nome definido: '" + meuNome + "', NomeJogador1: '" + nomeJogador1 + "', NomeJogador2: '" + nomeJogador2 + "'");
    }

    public void setCor(String cor) {
        this.cor = cor;
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Cor definida: " + cor);
        updateLabelTurno();
    }

    public String getOpponentName() {
        if (meuNome == null || meuNome.trim().isEmpty()) {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: meuNome é null ou vazio ao obter nome do adversário.");
            return "";
        }
        String opponent = meuNome.equalsIgnoreCase(nomeJogador1) ? nomeJogador2 : nomeJogador1;
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] getOpponentName: meuNome='" + meuNome + "', opponent='" + opponent + "'");
        return opponent != null ? opponent : "";
    }

    @FXML
    public void initialize() {
        configureInterface();
        createBoard();
        createBearingOffOutline();
        configureListeners();
        if (botaoEnviarChat != null) {
            botaoEnviarChat.setOnAction(_ -> sendChatMessage());
        }
        if (campoChat != null) {
            campoChat.setOnAction(_ -> sendChatMessage());
        }
        if (botaoLancarDados != null) {
            botaoLancarDados.setOnAction(_ -> rollDice());
            botaoLancarDados.setDisable(true);
            labelTurno.setText("Aguardando início do jogo...");
        }
        updateLabelTurno();
    }

    @Override
    public void onMessageReceived(String estado) {
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] JogoController recebeu: " + estado);
        Platform.runLater(() -> {
            synchronized (lock) {
                if (estado.equals("SORTEIO_VEZ")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando SORTEIO_VEZ");
                    minhaVez = true;
                    aguardandoInicio = true;
                    updateLabelTurno();
                    botaoLancarDados.setDisable(false);
                    botaoLancarDados.setText("Lançar Dados para Sorteio");
                    addChatMessage("É a tua vez de lançar os dados para o sorteio!");
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados habilitado para sorteio. Estado: minhaVez=" + minhaVez + ", aguardandoInicio=" + aguardandoInicio);
                } else if (estado.equals("EMPATE_SORTEIO")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando EMPATE_SORTEIO");
                    minhaVez = true;
                    aguardandoInicio = true;
                    dado1View.setImage(null);
                    dado2View.setImage(null);
                    dado3View.setImage(null);
                    dado4View.setImage(null);
                    updateLabelTurno();
                    botaoLancarDados.setDisable(false);
                    botaoLancarDados.setText("Lançar Dados para Sorteio");
                    addChatMessage("Empate no sorteio! Lançar novamente!");
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados habilitado para novo sorteio. Estado: minhaVez=" + minhaVez + ", aguardandoInicio=" + aguardandoInicio);
                } else if (estado.startsWith("SORTEIO_CONFIRMADO:")) {
                    String[] partes = estado.split(":")[1].split(",");
                    int dado1 = Integer.parseInt(partes[0]);
                    int dado2 = Integer.parseInt(partes[1]);
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Sorteio confirmado: " + dado1 + ", " + dado2);
                    labelTurno.setText("Lançaste: " + dado1 + " e " + dado2 + ". Aguardando adversário...");
                    dado1View.setImage(getDiceImage(dado1));
                    dado2View.setImage(getDiceImage(dado2));
                    dado1View.setOpacity(1.0);
                    dado2View.setOpacity(1.0);
                    botaoLancarDados.setDisable(true);
                    minhaVez = false;
                    // Keep aguardandoInicio true until sortition is resolved
                    updateLabelTurno();
                    addChatMessage("Meu Sorteio: " + dado1 + " e " + dado2);
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados desabilitado após confirmação de sorteio.");
                } else if (estado.startsWith("SORTEIO_INVALIDO:")) {
                    String erro = estado.substring("SORTEIO_INVALIDO:".length());
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro no sorteio: " + erro);
                    labelTurno.setText("Erro no sorteio: " + erro);
                    botaoLancarDados.setDisable(false);
                    minhaVez = true;
                    aguardandoInicio = true;
                    addChatMessage("Erro no sorteio: " + erro);
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados habilitado para retry após erro no sorteio.");
                } else if (estado.startsWith("DADOS_SORTEIO:")) {
                    String[] partes = estado.split(":");
                    String[] valores = partes[1].split(",");
                    int d1 = Integer.parseInt(valores[0]);
                    int d2 = Integer.parseInt(valores[1]);
                    String tipo = partes.length > 2 ? partes[2] : "";
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Dados de sorteio: " + d1 + "," + d2 + " (" + tipo + "), meuNome='" + meuNome + "', nomeJogador1='" + nomeJogador1 + "', nomeJogador2='" + nomeJogador2 + "'");
                    if (tipo.equals("BRANCO")) {
                        dado3View.setImage(getDiceImage(d1));
                        dado4View.setImage(getDiceImage(d2));
                        dado3View.setOpacity(0.6);
                        dado4View.setOpacity(0.6);
                        dado3View.setVisible(true);
                        dado4View.setVisible(true);
                        String opponentName = (nomeJogador1 != null && !nomeJogador1.isEmpty()) ? nomeJogador1 : "Adversário";
                        if (!meuNome.equals(nomeJogador1)) {
                            addChatMessage("Sorteio do " + opponentName + ": " + d1 + " e " + d2);
                        } else {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Não adicionando mensagem de sorteio ao chat: sou jogador1.");
                        }
                    } else if (tipo.equals("VERMELHO")) {
                        dado3View.setImage(getDiceImage(d1));
                        dado4View.setImage(getDiceImage(d2));
                        dado3View.setOpacity(0.6);
                        dado4View.setOpacity(0.6);
                        dado3View.setVisible(true);
                        dado4View.setVisible(true);
                        String opponentName = (nomeJogador2 != null && !nomeJogador2.isEmpty()) ? nomeJogador2 : "Adversário";
                        if (!meuNome.equals(nomeJogador2)) {
                            addChatMessage("Sorteio do " + opponentName + ": " + d1 + " e " + d2);
                        } else {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Não adicionando mensagem de sorteio ao chat: sou jogador2.");
                        }
                    }
                    if (dado1View.getImage() != null && dado3View.getImage() != null) {
                        labelTurno.setText("Sorteio concluído. Aguardando resultado...");
                        updateLabelTurno();
                    }
                } else if (estado.startsWith("COR:")) {
                    cor = estado.substring(4).trim();
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Cor recebida: " + cor);
                    updateLabelTurno();
                } else if (estado.equals("VOCE_COMECA")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando VOCE_COMECA, meuNome: '" + meuNome + "'");
                    try {
                        minhaVez = true;
                        aguardandoInicio = false;
                        jogo.definirJogadorInicial(meuNome);
                        if (jogo.getJogadorAtual() == null) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: jogadorAtual é null após definirJogadorInicial('" + meuNome + "')");
                            addChatMessage("Erro: Não foi possível inicializar o jogador atual.");
                        } else {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador atual definido: " + jogo.getJogadorAtual().getNome());
                        }
                        updateGame();
                        labelTurno.setText("Foste selecionado para começar!");
                        botaoLancarDados.setDisable(true);
                        botaoLancarDados.setText("Aguardar Jogada");

                        addChatMessage(meuNome + " começa o jogo!");
                    } catch (Exception e) {
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao processar VOCE_COMECA: " + e.getMessage());
                        addChatMessage("Erro ao iniciar o jogo: " + e.getMessage());
                    }
                } else if (estado.equals("ADVERSARIO_COMECA")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando ADVERSARIO_COMECA, adversário: '" + getOpponentName() + "'");
                    try {
                        minhaVez = false;
                        aguardandoInicio = false;
                        String nomeAdv = getOpponentName();
                        jogo.definirJogadorInicial(nomeAdv);
                        if (jogo.getJogadorAtual() == null) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: jogadorAtual é null após definirJogadorInicial('" + nomeAdv + "')");
                            addChatMessage("Erro: Não foi possível inicializar o jogador atual.");
                        } else {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador atual definido: " + jogo.getJogadorAtual().getNome());
                        }
                        updateGame();
                        labelTurno.setText(nomeAdv + " começa o jogo!");
                        botaoLancarDados.setDisable(true);
                        botaoLancarDados.setText("Aguardar Vez");
                        addChatMessage(nomeAdv + " começa o jogo!");
                    } catch (Exception e) {
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao processar ADVERSARIO_COMECA: " + e.getMessage());
                        addChatMessage("Erro ao iniciar o jogo: " + e.getMessage());
                    }
                } else if (estado.equals("SUA_VEZ")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando SUA_VEZ");
                    minhaVez = true;
                    aguardandoInicio = false;
                    
                    // Ensure I am the current player in the local game state
                    if (jogo.getJogadorAtual() == null || !jogo.getJogadorAtual().getNome().equals(meuNome)) {
                        try {
                            jogo.definirJogadorInicial(meuNome);
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] SUA_VEZ: Forçado jogador atual para " + meuNome);
                        } catch (Exception e) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao forçar jogador atual: " + e.getMessage());
                        }
                    }
                    
                    updateGame();

                    // Ensure the player can roll dice if none are available
                    if (jogo.getJogadorAtual() != null) {
                        List<Integer> dadosAtual = jogo.getJogadorAtual().getDadosDisponiveis();
                        if (dadosAtual.isEmpty()) {
                            botaoLancarDados.setDisable(false);
                            botaoLancarDados.setText("Lançar Dados");
                            labelTurno.setText("É a tua vez! Lança os dados.");
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] SUA_VEZ: Sem dados disponíveis, habilitando lançamento.");
                        } else {
                            botaoLancarDados.setDisable(true);
                            botaoLancarDados.setText("Jogar");
                            labelTurno.setText("É a tua vez! Faz uma jogada.");
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] SUA_VEZ: Com dados disponíveis: " + dadosAtual);
                        }
                    } else {
                        botaoLancarDados.setDisable(false);
                        botaoLancarDados.setText("Lançar Dados");
                        labelTurno.setText("É a tua vez!");
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] SUA_VEZ: jogadorAtual é null, habilitando lançamento por segurança.");
                    }

                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão estado após SUA_VEZ: disabled=" + botaoLancarDados.isDisable() + ", text=" + botaoLancarDados.getText());
                } else if (estado.equals("VEZ_ADV")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando VEZ_ADV");
                    minhaVez = false;
                    aguardandoInicio = false;
                    
                    // Ensure opponent is the current player in the local game state
                    String nomeAdversario = getOpponentName();
                    if (jogo.getJogadorAtual() == null || !jogo.getJogadorAtual().getNome().equals(nomeAdversario)) {
                        try {
                            jogo.definirJogadorInicial(nomeAdversario);
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] VEZ_ADV: Forçado jogador atual para " + nomeAdversario);
                        } catch (Exception e) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao forçar jogador atual: " + e.getMessage());
                        }
                    }
                    
                    updateGame();
                    labelTurno.setText("Vez do adversário (" + getOpponentName() + ")");
                    botaoLancarDados.setDisable(true);
                    botaoLancarDados.setText("Aguardar Vez");
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados desabilitado para vez do adversário.");
                } else if (estado.startsWith("JOGADA:")) {
                    String[] partes = estado.split(":")[1].split(",");
                    if (partes.length == 2) {
                        try {
                            int origem = Integer.parseInt(partes[0]);
                            int destino = Integer.parseInt(partes[1]);
                            boolean sucesso = jogo.moverPeca(origem, destino);
                            if (sucesso) {
                                updateBoard();
                                // Removed: checkTurnCompletion() - don't auto-pass turn when receiving moves from server
                                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogada processada: " + origem + " -> " + destino);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao interpretar jogada recebida: " + estado);
                        }
                    }
                } else if (estado.startsWith("DADOS:")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando DADOS: " + estado);
                    String[] split = estado.split(":", 2);
                    boolean dadosVazios = split.length < 2 || split[1].isBlank();

                    if (dadosVazios) {
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] DADOS vazios recebidos.");

                        // Clear dice display when empty dice are received
                        if (jogo.getJogadorAtual() != null) {
                            jogo.getJogadorAtual().getDadosDisponiveis().clear();
                            jogo.getJogadorAtual().getUltimosDados().clear();
                        }
                        updateVisibleDice();

                        // If it's my turn and no dice, enable dice rolling
                        if (minhaVez && !aguardandoInicio) {
                            botaoLancarDados.setDisable(false);
                            botaoLancarDados.setText("Lançar Dados");
                            labelTurno.setText("É a tua vez! Lança os dados.");
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] DADOS vazios: Habilitando lançamento - minhaVez=" + minhaVez + ", aguardandoInicio=" + aguardandoInicio);
                        } else {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] DADOS vazios: NÃO habilitando lançamento - minhaVez=" + minhaVez + ", aguardandoInicio=" + aguardandoInicio);
                        }
                        return;
                    }

                    // Critical: Ensure the correct player is current before processing dice
                    if (minhaVez && (jogo.getJogadorAtual() == null || !jogo.getJogadorAtual().getNome().equals(meuNome))) {
                        try {
                            jogo.definirJogadorInicial(meuNome);
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] DADOS: Corrigido jogador atual para " + meuNome);
                        } catch (Exception e) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao corrigir jogador atual: " + e.getMessage());
                        }
                    } else if (!minhaVez && (jogo.getJogadorAtual() == null || !jogo.getJogadorAtual().getNome().equals(getOpponentName()))) {
                        try {
                            jogo.definirJogadorInicial(getOpponentName());
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] DADOS: Corrigido jogador atual para " + getOpponentName());
                        } catch (Exception e) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao corrigir jogador atual: " + e.getMessage());
                        }
                    }

                    if (jogo.getJogadorAtual() == null) {
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: jogadorAtual é null ao processar DADOS: " + estado);
                        addChatMessage("Erro: Jogador atual não inicializado. Aguardando inicialização.");
                        return;
                    }

                    try {
                        String[] valores = split.length > 1 ? split[1].split(",") : new String[0];
                        List<Integer> dados = new ArrayList<>();
                        for (String valor : valores) {
                            if (!valor.isBlank()) {
                                try {
                                    dados.add(Integer.parseInt(valor));
                                } catch (NumberFormatException ignored) {
                                    System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Valor de dado inválido: " + valor);
                                }
                            }
                        }

                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador atual: " + jogo.getJogadorAtual().getNome() + ", Dados recebidos: " + dados + ", minhaVez: " + minhaVez);

                        // Clear and set new dice
                        jogo.getJogadorAtual().getDadosDisponiveis().clear();
                        jogo.getJogadorAtual().getUltimosDados().clear();
                        dados.forEach(d -> {
                            jogo.getJogadorAtual().getDadosDisponiveis().add(d);
                            jogo.getJogadorAtual().getUltimosDados().add(d);
                        });

                        // Force recalculation of movable fields after setting dice
                        if (!dados.isEmpty()) {
                            jogo.iniciarTurno(); // This will recalculate movable fields with new dice
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Campos móveis recalculados: " + jogo.getCamposComPecasMoveis());
                        }

                        updateVisibleDice();
                        updateBoard(); // This should now highlight the correct pieces

                        // Handle dice logic for current player
                        if (minhaVez && !aguardandoInicio) {
                            if (dados.isEmpty()) {
                                // No dice received - allow rolling
                                botaoLancarDados.setDisable(false);
                                botaoLancarDados.setText("Lançar Dados");
                                labelTurno.setText("É a tua vez! Lança os dados.");
                                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Sem dados recebidos - permitindo lançamento.");
                            } else {
                                // Dice received - check for moves
                                botaoLancarDados.setDisable(true);
                                botaoLancarDados.setText("Jogar");
                                labelTurno.setText("É a tua vez! Faz uma jogada.");
                                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Dados recebidos - peças jogáveis: " + jogo.getCamposComPecasMoveis());
                            }
                        } else if (!minhaVez && !aguardandoInicio) {
                            // Not my turn - just display the dice
                            botaoLancarDados.setDisable(true);
                            botaoLancarDados.setText("Aguardar Vez");
                            if (!dados.isEmpty()) {
                                labelTurno.setText("Vez do adversário - dados: " + dados);
                            }
                        }

                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Dados aplicados com sucesso: " + dados);
                    } catch (Exception e) {
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao processar DADOS: " + estado + ", motivo: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (estado.equals("MOVIMENTO_INVALIDO")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando MOVIMENTO_INVALIDO");
                    labelTurno.setText("Movimento inválido. Tenta novamente.");
                    addChatMessage("Movimento inválido. Tenta novamente.");
                } else if (estado.equals("AGUARDANDO_JOGADOR")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando AGUARDANDO_JOGADOR");
                    minhaVez = false;
                    aguardandoInicio = true;
                    dado1View.setImage(null);
                    dado2View.setImage(null);
                    dado3View.setImage(null);
                    dado4View.setImage(null);
                    updateLabelTurno();
                    botaoLancarDados.setDisable(true);
                    botaoLancarDados.setText("Aguardar Novo Jogador");
                    addChatMessage("Um jogador desconectou-se. Aguardando novo jogador.");
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados desabilitado enquanto aguarda novo jogador.");
                } else if (estado.startsWith("CHAT:")) {
                    String mensagem = estado.substring("CHAT:".length()).trim();
                    addChatMessage(mensagem);
                } else if (estado.startsWith("CHAT_ENVIADO:")) {
                    String mensagem = estado.substring("CHAT_ENVIADO:".length()).trim();
                    addChatMessage("Tu: " + mensagem);
                } else if (estado.equals("Erro ao conectar ao servidor.")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Processando erro de conexão");
                    labelTurno.setText("Ligação perdida!");
                    botaoLancarDados.setDisable(true);
                    addChatMessage("Ligação perdida com o servidor.");
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Botão Lançar Dados desabilitado devido a perda de conexão.");
                } else if (estado.startsWith("INICIAR_JOGO:")) {
                    String[] partes = estado.split(":")[1].split(",");
                    if (partes.length == 2) {
                        setPlayerNames(partes[0].trim(), partes[1].trim());
                        updateLabelTurno();
                    } else {
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Mensagem INICIAR_JOGO inválida: " + estado);
                        labelTurno.setText("Erro: Mensagem de início inválida.");
                        addChatMessage("Erro: Mensagem de início inválida.");
                    }
                } else {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem desconhecida ou ignorada: " + estado);
                }
            }
        });
    }

    private void updateGame() {
        updateBoard();
        updateVisibleDice();
        updateLabelTurno();
    }

    private void updateLabelTurno() {
        synchronized (lock) {
            if (jogoTerminado && vencedor != null) {
                // Game ended - show winner/loser message
                if (vencedor.equals(meuNome)) {
                    labelTurno.setText("🏆 VITÓRIA! 🏆");
                    labelTurno.setStyle("-fx-font-size: 28px; -fx-text-fill: gold; -fx-font-weight: bold;");
                } else {
                    labelTurno.setText("💔 DERROTA! 💔");
                    labelTurno.setStyle("-fx-font-size: 28px; -fx-text-fill: red; -fx-font-weight: bold;");
                }
                botaoLancarDados.setText("Jogo Terminado");
                botaoLancarDados.setDisable(true);
                return;
            }
            
            if (aguardandoInicio) {
                labelTurno.setText(minhaVez ? "É a tua vez de lançar os dados para o sorteio." : "Aguardando resultado do sorteio...");
                botaoLancarDados.setDisable(!minhaVez);
                botaoLancarDados.setText(minhaVez ? "Lançar Dados para Sorteio" : "Aguardar Resultado");
                labelTurno.setStyle("-fx-font-size: 30px; -fx-text-fill: yellow; -fx-font-weight: bold;");
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Atualizado turno: " + (minhaVez ? "Minha vez de lançar dados para sorteio." : "Aguardando resultado do sorteio."));
            } else if (minhaVez) {
                String corDisplay = cor != null ? (cor.equals("BRANCO") ? "Brancas" : "Vermelhas") : "Jogador";
                labelTurno.setText("SUA VEZ (" + corDisplay + ")");
                labelTurno.setStyle("-fx-font-size: 30px; -fx-text-fill: white; -fx-font-weight: bold;");

                // Check if player needs to roll dice
                boolean needsToRoll = jogo.getJogadorAtual() == null || jogo.getJogadorAtual().getDadosDisponiveis().isEmpty();
                botaoLancarDados.setDisable(!needsToRoll);
                botaoLancarDados.setText(needsToRoll ? "Lançar Dados" : "Jogar");

                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] updateLabelTurno: Minha vez - needsToRoll=" + needsToRoll + ", dadosDisponiveis=" + (jogo.getJogadorAtual() != null ? jogo.getJogadorAtual().getDadosDisponiveis().size() : "null"));
            } else {
                String corAdv = cor != null ? (cor.equals("BRANCO") ? "Vermelhas" : "Brancas") : "Adversário";
                String nomeAdv = getOpponentName();
                labelTurno.setText("Vez do Adversário (" + corAdv + ": " + nomeAdv + ")");
                labelTurno.setStyle("-fx-font-size: 30px; -fx-text-fill: red; -fx-font-weight: bold;");
                botaoLancarDados.setDisable(true);
                botaoLancarDados.setText("Aguardar Vez");
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Atualizado turno: Vez do adversário (" + corAdv + ": " + nomeAdv + ").");
            }
        }
    }

    private boolean isMinhaVez() {
        synchronized (lock) {
            return minhaVez;
        }
    }

    @FXML
    private void rollDice() {
        synchronized (lock) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] rollDice chamado - minhaVez=" + minhaVez + ", aguardandoInicio=" + aguardandoInicio + ", botaoDisabled=" + botaoLancarDados.isDisable() + ", jogoTerminado=" + jogoTerminado);

            // Check if game ended and restart is requested
            if (jogoTerminado && botaoLancarDados.getText().equals("Jogar Novamente")) {
                restartGame();
                return;
            }

            if (!minhaVez) {
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Tentativa de lançar dados ignorada: Não é minha vez.");
                return;
            }

            // Ensure the button is re-enabled if stuck
            botaoLancarDados.setDisable(false);

            if (aguardandoInicio) {
                // Sortition phase logic
                if (!botaoLancarDados.getText().equals("Lançar Dados para Sorteio")) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Tentativa de lançar dados ignorada: Não está na fase de sorteio.");
                    return;
                }
                botaoLancarDados.setDisable(true);
                Timeline timeline = new Timeline();
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(50), _ -> {
                    Random r = new Random();
                    dado1View.setImage(getDiceImage(r.nextInt(6) + 1));
                    dado2View.setImage(getDiceImage(r.nextInt(6) + 1));
                }));
                timeline.setCycleCount(15);
                timeline.setOnFinished(_ -> {
                    int resultado1 = new Random().nextInt(6) + 1;
                    int resultado2 = new Random().nextInt(6) + 1;
                    dado1View.setImage(getDiceImage(resultado1));
                    dado2View.setImage(getDiceImage(resultado2));
                    if (cliente != null && cliente.isConectado()) {
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Preparando para enviar SORTEIO_INICIAL: " + resultado1 + "," + resultado2);
                        cliente.enviarSorteioInicial(resultado1, resultado2);
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado SORTEIO_INICIAL: " + resultado1 + "," + resultado2);
                        labelTurno.setText("Lançaste: " + resultado1 + " e " + resultado2 + ". Aguardando adversário...");
                        addChatMessage("Meu Sorteio: " + resultado1 + " e " + resultado2);
                        minhaVez = false;
                        // Keep aguardandoInicio true until server confirms sortition outcome
                        Timeline timeout = new Timeline(new KeyFrame(Duration.seconds(10), _ -> {
                            if (!minhaVez && aguardandoInicio) {
                                labelTurno.setText("Timeout aguardando sorteio. Tentando novamente...");
                                botaoLancarDados.setDisable(false);
                                botaoLancarDados.setText("Lançar Dados para Sorteio");
                                minhaVez = true;
                                aguardandoInicio = true;
                                addChatMessage("Timeout no sorteio. Lançar novamente.");
                                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Timeout aguardando sorteio. Reativando botão para novo sorteio.");
                            }
                        }));
                        timeout.setCycleCount(1);
                        timeout.play();
                    } else {
                        labelTurno.setText("Erro: Não conectado ao servidor.");
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente não conectado ao enviar SORTEIO_INICIAL");
                        botaoLancarDados.setDisable(true);
                    }
                });
                timeline.play();
            } else {
                // Game phase - ensure we can roll dice
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Lançando dados no jogo. Estado atual: minhaVez=" + minhaVez + ", aguardandoInicio=" + aguardandoInicio);

                // Check if we should be able to roll
                if (jogo.getJogadorAtual() != null && !jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Ignorando rollDice: jogador já tem dados disponíveis.");
                    labelTurno.setText("Já lançaste os dados! Faz uma jogada.");
                    return;
                }

                // Disable button immediately
                botaoLancarDados.setDisable(true);
                botaoLancarDados.setText("Lançando...");
                labelTurno.setText("Lançando dados...");

                // Animate dice rolling
                animateDice();

                // Send command to server
                if (cliente != null && cliente.isConectado()) {
                    cliente.enviarComando("LANCAR");
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado comando LANCAR para o servidor");
                    
                    // Set timeout to recover from stuck state after turn passing
                    Timeline timeout = new Timeline(new KeyFrame(Duration.seconds(5), _ -> {
                        if (minhaVez && !aguardandoInicio && 
                            (jogo.getJogadorAtual() == null || jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()) && 
                            botaoLancarDados.getText().equals("Lançando...")) {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Timeout lançando dados após passar turno. Reativando botão.");
                            botaoLancarDados.setDisable(false);
                            botaoLancarDados.setText("Lançar Dados");
                            labelTurno.setText("É a tua vez! Lança os dados.");
                        }
                    }));
                    timeout.setCycleCount(1);
                    timeout.play();
                } else {
                    labelTurno.setText("Erro: Não conectado ao servidor.");
                    System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente não conectado ao enviar LANCAR");
                    botaoLancarDados.setDisable(false);
                    botaoLancarDados.setText("Lançar Dados");
                }
            }
        }
    }

    private void animateDice() {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(100), _ -> {
            Random r = new Random();
            dado1View.setImage(getDiceImage(r.nextInt(6) + 1));
            dado2View.setImage(getDiceImage(r.nextInt(6) + 1));
        }));
        timeline.setCycleCount(10);
        timeline.setOnFinished(_ -> {
            updateVisibleDice();
            updateBoard();
            // NÃO chama checkTurnCompletion() aqui!
            // O correto é só verificar se há jogadas possíveis após receber os dados do servidor (DADOS:...)
        });
        timeline.play();
    }

    private void moveToDestination(int destinoId) {
        if (!isMinhaVez() || campoSelecionado == null) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Movimento ignorado: Não é minha vez ou campo não selecionado.");
            return;
        }
        if (cliente != null && cliente.isConectado()) {
            int origem = campoSelecionado;
            cliente.enviarComando("MOVER " + origem + " " + destinoId);
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado comando MOVER: " + origem + " -> " + destinoId);
            
            // Clear selection immediately to prevent double moves
            campoSelecionado = null;
            updateBoard(); // Update board to remove selection highlighting

            // Check for victory after move
            Platform.runLater(() -> {
                Timeline delayedCheck = new Timeline(new KeyFrame(Duration.millis(500), _ -> {
                    checkVictory();
                }));
                delayedCheck.setCycleCount(1);
                delayedCheck.play();
            });

            // Remove automatic turn completion check - let server handle it
            // Timeline delayedCheck = new Timeline(new KeyFrame(Duration.millis(500), _ -> {
            //     checkTurnCompletion();
            // }));
            // delayedCheck.setCycleCount(1);
            // delayedCheck.play();
        } else {
            labelTurno.setText("Erro: Não conectado ao servidor.");
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente não conectado ao enviar MOVER");
        }
    }

    private void selectField(int campoId) {
        if (!isMinhaVez()) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Seleção de campo ignorada: Não é minha vez.");
            return;
        }
        
        // If this field is already selected, deselect it
        if (campoSelecionado != null && campoSelecionado == campoId) {
            campoSelecionado = null;
            updateBoard();
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Campo " + campoId + " desselecionado.");
            return;
        }
        
        // Check if this field has movable pieces before allowing selection
        List<Integer> camposMoveis = jogo.getCamposComPecasMoveis();
        if (!camposMoveis.contains(campoId)) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Campo " + campoId + " não está na lista de campos móveis: " + camposMoveis);
            return;
        }
        
        // Check if the top piece belongs to me
        Campo campo = jogo.getCampo(campoId);
        if (campo == null || campo.getPecas().isEmpty()) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Campo " + campoId + " está vazio.");
            return;
        }
        
        Peca topPiece = campo.getPecas().get(campo.getPecas().size() - 1);
        if (!topPiece.getJogador().getNome().equals(meuNome)) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Peça do topo no campo " + campoId + " não é minha. Dono: " + topPiece.getJogador().getNome() + ", Meu nome: " + meuNome);
            return;
        }
        
        // Check if I'm the current player
        if (jogo.getJogadorAtual() == null || !jogo.getJogadorAtual().getNome().equals(meuNome)) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Não sou o jogador atual. Jogador atual: " + (jogo.getJogadorAtual() != null ? jogo.getJogadorAtual().getNome() : "null") + ", Meu nome: " + meuNome);
            return;
        }
        
        campoSelecionado = campoId;
        updateBoard();
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Campo selecionado: " + campoId);
    }

    private void configureInterface() {
        Image bgVerde = new Image(getClass().getResource("/background1.png").toExternalForm());
        backgroundView.setImage(bgVerde);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundView.fitHeightProperty().bind(rootPane.heightProperty());
        StackPane centralizador = new StackPane(tabuleiro);
        tabuleiroContainer.getChildren().add(centralizador);
        configureBoardSize();
    }

    private void configureBoardSize() {
        tabuleiro.maxWidthProperty().bind(Bindings.createDoubleBinding(() ->
                        Math.min(tabuleiroContainer.getWidth(), tabuleiroContainer.getHeight() * 16.0 / 9.0),
                tabuleiroContainer.widthProperty(), tabuleiroContainer.heightProperty()));
        tabuleiro.maxHeightProperty().bind(tabuleiro.maxWidthProperty().multiply(9.0 / 16.0));
        tabuleiro.prefWidthProperty().bind(tabuleiro.maxWidthProperty());
        tabuleiro.prefHeightProperty().bind(tabuleiro.maxHeightProperty());
        tabuleiro.setHgap(6);
    }

    private void createBoard() {
        configureColumnsAndRows();
        configureBoardBackground();
        createFields();
    }

    private void configureColumnsAndRows() {
        for (int i = 0; i < 16; i++) {
            ColumnConstraints col = new ColumnConstraints();
            if (i == 0) col.setPercentWidth(1.0);
            else if (i == 15) col.setPercentWidth(7.0);
            else col.setPercentWidth(92.0 / 14);
            tabuleiro.getColumnConstraints().add(col);
        }
        for (int i = 0; i < 2; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(50);
            tabuleiro.getRowConstraints().add(row);
        }
    }

    private void configureBoardBackground() {
        Image fundo = new Image(getClass().getResource("/tabuleiro.png").toExternalForm());
        BackgroundImage bgTabuleiro = new BackgroundImage(
                fundo, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, false));
        tabuleiro.setBackground(new Background(bgTabuleiro));
    }

    private void createFields() {
        for (int col = 1; col < 15; col++) {
            if (col == 7 || col == 8) continue;
            StackPane cima = createFieldContainer(col, true);
            StackPane baixo = createFieldContainer(col, false);
            tabuleiro.add(cima, col, 0);
            tabuleiro.add(baixo, col, 1);
            configureField(col, cima, true);
            configureField(col, baixo, false);
        }
    }

    private StackPane createFieldContainer(int coluna, boolean cima) {
        StackPane container = new StackPane();
        container.setPickOnBounds(false);
        Polygon triangulo = new Polygon();
        triangulo.setPickOnBounds(true);
        Pane trianguloPane = new Pane(triangulo);
        trianguloPane.setPickOnBounds(false);
        StackPane pecasContainer = new StackPane();
        pecasContainer.setPickOnBounds(false);
        pecasContainer.setAlignment(cima ? Pos.TOP_CENTER : Pos.BOTTOM_CENTER);
        container.getChildren().addAll(trianguloPane, pecasContainer);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        if (coluna <= 6) container.setPadding(new Insets(0, -40, 20, 40));
        else if (coluna >= 9) container.setPadding(new Insets(0, 40, 20, -40));
        container.layoutBoundsProperty().addListener((_, _, newB) -> {
            updateTriangleShape(triangulo, newB, cima);
            triangulo.setFill(coluna % 2 == 0 ? Color.SADDLEBROWN : Color.BURLYWOOD);
            triangulo.setStroke(Color.BLACK);
            triangulo.setStrokeWidth(1);
        });
        return container;
    }

    private void updateTriangleShape(Polygon triangulo, Bounds bounds, boolean cima) {
        double w = bounds.getWidth(), h = bounds.getHeight();
        double altura = h * 0.768;
        double margem = h * 0.116;
        if (cima) {
            triangulo.getPoints().setAll(
                    0.0, margem,
                    w, margem,
                    w / 2, margem + altura
            );
        } else {
            triangulo.getPoints().setAll(
                    0.0, h - margem,
                    w, h - margem,
                    w / 2, h - margem - altura
            );
        }
    }

    private void configureField(int col, StackPane container, boolean cima) {
        int campoId = mapColumnToField(col, cima);
        if (campoId >= 1 && campoId <= 24) {
            campoPecasMap.put(campoId, container);
            Polygon triangulo = (Polygon) ((Pane) container.getChildren().get(0)).getChildren().get(0);
            campoTrianguloMap.put(campoId, triangulo);
            triangulo.setOnMouseClicked(_ -> selectField(campoId));
        }
    }

    private int mapColumnToField(int col, boolean cima) {
        if (col == 7 || col == 8) return 0;
        if (!cima) return col <= 6 ? 7 + (6 - col) : 15 - col;
        return col <= 6 ? 13 + (col - 1) : 19 + (col - 9);
    }

    private void configureListeners() {
        tabuleiroContainer.widthProperty().addListener((_, _, _) -> {
            tabuleiro.requestLayout();
            if (!aguardandoInicio) {
                updateBoard();
            }
        });
        tabuleiroContainer.heightProperty().addListener((_, _, _) -> {
            tabuleiro.requestLayout();
            if (!aguardandoInicio) {
                updateBoard();
            }
        });
    }

    private void createBearingOffOutline() {
        bearingOffOutline = new Rectangle();
        bearingOffOutline.setArcWidth(24);
        bearingOffOutline.setArcHeight(24);
        bearingOffOutline.setFill(Color.TRANSPARENT);
        bearingOffOutline.setStroke(Color.LIMEGREEN);
        bearingOffOutline.setStrokeWidth(5);
        bearingOffOutline.setVisible(false);
        tabuleiroContainer.getChildren().add(bearingOffOutline);
    }

    private void updateBoard() {
        if (jogo == null || tabuleiroContainer.getHeight() <= 0) return;
        if (jogo.getJogadorAtual() == null) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] updateBoard ignorado: jogadorAtual é null.");
            return;
        }

        // Check for victory after updating board
        if (!jogoTerminado) {
            checkVictory();
        }

        double raioBase = Math.max(16, tabuleiroContainer.getHeight() * 0.032);
        double raioSelecionado = raioBase * 1.25;
        double spacing = Math.max(18, tabuleiroContainer.getHeight() * 0.04);
        double margemTopo = tabuleiroContainer.getHeight() * 0.04;
        double margemBase = tabuleiroContainer.getHeight() * 0.04;

        campoPecasMap.values().forEach(container -> {
            if (container.getChildren().size() > 2) {
                container.getChildren().removeIf(node -> node instanceof Circle && ((Circle) node).getStroke() == Color.LIMEGREEN && ((Circle) node).getFill() == Color.TRANSPARENT);
            }
        });

        boolean isMinhaVezLocal;
        synchronized (lock) {
            isMinhaVezLocal = minhaVez;
        }

        // Get movable fields from the game logic
        List<Integer> camposMoveis = jogo.getCamposComPecasMoveis();
        String jogadorAtualNome = jogo.getJogadorAtual().getNome();
        
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] updateBoard: jogadorAtual=" + jogadorAtualNome + ", meuNome=" + meuNome + ", minhaVez=" + isMinhaVezLocal + ", camposMoveis=" + camposMoveis);

        for (var entry : campoPecasMap.entrySet()) {
            int campoId = entry.getKey();
            StackPane container = entry.getValue();
            Campo campo = jogo.getCampo(campoId);
            if (campo == null || container.getChildren().size() < 2) continue;

            StackPane pecasContainer = (StackPane) container.getChildren().get(1);
            pecasContainer.getChildren().clear();

            List<Peca> pecas = campo.getPecas();
            int quantidade = pecas.size();

            for (int i = 0; i < quantidade; i++) {
                Peca peca = pecas.get(i);
                boolean topo = i == quantidade - 1;
                boolean selecionada = campoSelecionado != null && campoSelecionado == campoId && topo;
                boolean temCapturada = jogo.getJogadorAtual().temPecasCapturadas();

                // Check if this piece can be moved - must be my turn, my piece, on top, and in movable fields
                boolean minhaP = peca.getJogador().getNome().equals(meuNome);
                boolean jogadorAtualEhMeu = jogadorAtualNome.equals(meuNome);
                boolean campoEhMovel = camposMoveis.contains(campoId);
                
                boolean jogavel = isMinhaVezLocal
                        && jogadorAtualEhMeu  // I must be the current player
                        && minhaP             // The piece must be mine
                        && !temCapturada      // No captured pieces
                        && campoEhMovel       // Field must be in movable fields list
                        && topo;              // Must be top piece

                if (jogavel) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Peça jogável no campo " + campoId + ": minhaVez=" + isMinhaVezLocal + ", jogadorAtualEhMeu=" + jogadorAtualEhMeu + ", minhaP=" + minhaP + ", campoEhMovel=" + campoEhMovel + ", topo=" + topo);
                }

                Circle c = new Circle(selecionada ? raioSelecionado : raioBase);
                c.setFill(peca.getJogador() == jogo.getJogador1() ? Color.WHITE : Color.RED);

                if (selecionada) {
                    c.setStroke(Color.LIMEGREEN);
                    c.setStrokeWidth(3);
                } else if (jogavel) {
                    c.setStroke(Color.LIMEGREEN);
                    c.setStrokeWidth(2);
                } else {
                    c.setStroke(Color.BLACK);
                    c.setStrokeWidth(1);
                }
                
                // Only add click handler to top pieces that can be selected
                if (topo && (jogavel || selecionada)) {
                    c.setOnMouseClicked(_ -> selectField(campoId));
                } else {
                    c.setOnMouseClicked(null);
                }

                double translateY;
                if (campoId <= 12) {
                    translateY = -i * spacing - margemBase;
                } else {
                    translateY = i * spacing + margemTopo;
                }
                c.setTranslateY(translateY);
                pecasContainer.getChildren().add(c);
            }

            Polygon triangulo = campoTrianguloMap.get(campoId);
            if (triangulo != null) {
                boolean destinoValido =
                        (campoSelecionado != null && campoSelecionado >= 1 && campoId != campoSelecionado &&
                                jogo.getDestinosValidos(campoSelecionado).contains(campoId)) ||
                                (campoSelecionado != null && campoSelecionado == -1 &&
                                        jogo.getCamposEntradaDisponiveis().contains(campoId));

                if (destinoValido) {
                    triangulo.setStroke(Color.LIMEGREEN);
                    triangulo.setStrokeWidth(2);
                    triangulo.setOnMouseClicked(_ -> moveToDestination(campoId));
                } else {
                    triangulo.setStroke(Color.BLACK);
                    triangulo.setStrokeWidth(1);
                    triangulo.setOnMouseClicked(_ -> selectField(campoId)); // Allow field selection even when not valid destination
                }
            }
        }

        boolean showBearingOff = false;
        double rectX = 0, rectY = 0, rectWidth = 0, rectHeight = 0;

        if (campoSelecionado != null) {
            List<Integer> destinos = jogo.getDestinosValidos(campoSelecionado);
            if (jogo.getJogadorAtual() == jogo.getJogador1() && destinos.contains(25)) {
                showBearingOff = true;
                rectWidth = tabuleiroContainer.getWidth() * 0.067;
                rectHeight = tabuleiroContainer.getHeight() * 0.37;
                rectX = tabuleiroContainer.getWidth() - rectWidth - tabuleiroContainer.getWidth() * 0.036;
                rectY = tabuleiroContainer.getHeight() * 0.044;
                bearingOffOutline.setOnMouseClicked(_ -> moveToDestination(25));
            }
            if (jogo.getJogadorAtual() == jogo.getJogador2() && destinos.contains(0)) {
                showBearingOff = true;
                rectWidth = tabuleiroContainer.getWidth() * 0.067;
                rectHeight = tabuleiroContainer.getHeight() * 0.37;
                rectX = tabuleiroContainer.getWidth() - rectWidth - tabuleiroContainer.getWidth() * 0.036;
                rectY = tabuleiroContainer.getHeight() - rectHeight - tabuleiroContainer.getHeight() * 0.044;
                bearingOffOutline.setOnMouseClicked(_ -> moveToDestination(0));
            }
        }

        if (showBearingOff) {
            bearingOffOutline.setVisible(true);
            bearingOffOutline.setWidth(rectWidth);
            bearingOffOutline.setHeight(rectHeight);
            bearingOffOutline.setX(rectX);
            bearingOffOutline.setY(rectY);
            bearingOffOutline.toFront();
        } else {
            bearingOffOutline.setVisible(false);
            bearingOffOutline.setOnMouseClicked(null);
        }

        pecasCapturadasView.getChildren().clear();
        List<Peca> capturadas = jogo.getJogadorAtual().getPecasCapturadas();
        if (!capturadas.isEmpty()) {
            Circle c = new Circle(raioSelecionado);
            c.setFill(capturadas.get(0).getJogador() == jogo.getJogador1() ? Color.WHITE : Color.RED);
            c.setStroke(Color.LIMEGREEN);
            c.setStrokeWidth(3);
            c.setOnMouseClicked(_ -> {
                campoSelecionado = -1; // Use -1 to indicate captured pieces
                updateBoard();
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Peça capturada selecionada (campo -1)");
            });
            pecasCapturadasView.getChildren().add(c);
        }
        // checkTurnCompletion(); // NÃO verificar passagem de turno aqui!
    }

    private void updateVisibleDice() {
        if (jogo.getJogadorAtual() == null) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] updateVisibleDice ignorado: jogadorAtual é null.");
            // Clear all dice displays when no current player
            dado1View.setImage(null);
            dado2View.setImage(null);
            if (dado3View != null) {
                dado3View.setVisible(false);
                dado3View.setImage(null);
            }
            if (dado4View != null) {
                dado4View.setVisible(false);
                dado4View.setImage(null);
            }
            return;
        }

        List<Integer> dados = jogo.getJogadorAtual().getDadosDisponiveis();
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Atualizando exibição de dados: " + dados);

        // Update main dice displays
        dado1View.setImage(getDiceImage(dados.size() > 0 ? dados.get(0) : 0));
        dado2View.setImage(getDiceImage(dados.size() > 1 ? dados.get(1) : 0));

        // Update additional dice for doubles
        if (dado3View != null) {
            if (dados.size() > 2) {
                dado3View.setVisible(true);
                dado3View.setImage(getDiceImage(dados.get(2)));
            } else {
                dado3View.setVisible(false);
                dado3View.setImage(null);
            }
        }
        if (dado4View != null) {
            if (dados.size() > 3) {
                dado4View.setVisible(true);
                dado4View.setImage(getDiceImage(dados.get(3)));
            } else {
                dado4View.setVisible(false);
                dado4View.setImage(null);
            }
        }
    }

    private Image getDiceImage(int valor) {
        if (valor >= 1 && valor <= 6) {
            String caminho = "/dado" + valor + ".png";
            java.net.URL recurso = getClass().getResource(caminho);
            if (recurso == null) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Imagem de dado não encontrada: " + caminho);
                return null;
            }
            return new Image(recurso.toExternalForm(), true);
        } else {
            return null;
        }
    }


    @FXML
    public void sendChatMessage() {
        String texto = campoChat.getText().trim();
        if (!texto.isEmpty()) {
            if (cliente != null && cliente.isConectado()) {
                cliente.enviarMensagemChat(texto);
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem de chat enviada: " + texto);
                campoChat.clear();
            } else {
                labelTurno.setText("Erro: Não conectado ao servidor.");
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente não conectado ao enviar mensagem de chat");
            }
        }
    }

    private void addChatMessage(String mensagem) {
        Label label = new Label(mensagem);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-background-color: rgba(255,255,255,0.8); " +
                "-fx-padding: 6px; " +
                "-fx-border-radius: 5px; " +
                "-fx-background-radius: 5px; " +
                "-fx-border-color: #ccc; " +
                "-fx-font-size: 12px;");
        chatMensagensBox.getChildren().add(label);
        if (chatMensagensBox.getParent() instanceof ScrollPane scrollPane) {
            scrollPane.setVvalue(1.0);
        }
    }

    // Só deve ser chamada após uma jogada (moveToDestination) ou após receber dados do servidor (DADOS:...)
    private void checkTurnCompletion() {
        if (!isMinhaVez() || jogo.getJogadorAtual() == null) return;

        List<Integer> dadosDisponiveis = jogo.getJogadorAtual().getDadosDisponiveis();
        List<Integer> camposMoveis = jogo.getCamposComPecasMoveis();

        boolean allDiceUsed = dadosDisponiveis.isEmpty();
        boolean noMovesPossible = camposMoveis.isEmpty();

        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] checkTurnCompletion: allDiceUsed=" + allDiceUsed + ", noMovesPossible=" + noMovesPossible + ", dadosDisponiveis=" + dadosDisponiveis.size() + ", camposMoveis=" + camposMoveis.size());

        // Only pass turn if all dice are used OR if there are truly no moves possible
        if (allDiceUsed || noMovesPossible) {
            if (cliente != null && cliente.isConectado()) {
                cliente.enviarComando("PASSAR_TURNO");
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado comando PASSAR_TURNO - motivo: " + (allDiceUsed ? "dados esgotados" : "sem movimentos possíveis"));

                // Disable dice button and update UI to show turn is ending
                botaoLancarDados.setDisable(true);
                botaoLancarDados.setText("Aguardando...");
                labelTurno.setText("Turno concluído. Aguardando adversário...");
            } else {
                labelTurno.setText("Erro: Não conectado ao servidor.");
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente não conectado ao enviar PASSAR_TURNO");
            }
        } else {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Turno não será passado - ainda há dados ou movimentos possíveis");
        }
    }

    // Improved move validation
    private boolean canMakeAnyMoveInternal() {
        if (jogo.getJogadorAtual() == null) return false;

        List<Integer> dados = new ArrayList<>(jogo.getJogadorAtual().getDadosDisponiveis());
        if (dados.isEmpty()) return false;

        List<Integer> campos = jogo.getCamposComPecasMoveis();

        // Check if player has captured pieces that need to be moved first
        if (jogo.getJogadorAtual().temPecasCapturadas()) {
            List<Integer> entradaDisponiveis = jogo.getCamposEntradaDisponiveis();
            boolean canEnter = !entradaDisponiveis.isEmpty();
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Verificando peças capturadas: " + canEnter);
            return canEnter;
        }

        // Check each field with movable pieces
        for (int campo : campos) {
            List<Integer> destinosValidos = jogo.getDestinosValidos(campo);
            if (!destinosValidos.isEmpty()) {
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Campo " + campo + " tem destinos válidos: " + destinosValidos.size());
                return true;
            }
        }

        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Nenhum movimento possível encontrado");
        return false;
    }

    // Add manual turn pass button functionality (optional)
    @FXML
    private void manualPassTurn() {
        if (isMinhaVez() && !aguardandoInicio) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador solicitou passar turno manualmente");
            checkTurnCompletion();
        }
    }

    private boolean checkVictory() {
        if (jogoTerminado) return true;

        // Check if player 1 (white pieces) has won
        boolean player1Won = true;
        for (int i = 1; i <= 24; i++) {
            Campo campo = jogo.getCampo(i);
            if (campo != null && !campo.getPecas().isEmpty()) {
                for (Peca peca : campo.getPecas()) {
                    if (peca.getJogador() == jogo.getJogador1()) {
                        player1Won = false;
                        break;
                    }
                }
                if (!player1Won) break;
            }
        }
        
        // Check captured pieces for player 1
        if (player1Won && jogo.getJogador1() != null && !jogo.getJogador1().getPecasCapturadas().isEmpty()) {
            player1Won = false;
        }

        // Check if player 2 (red pieces) has won
        boolean player2Won = true;
        for (int i = 1; i <= 24; i++) {
            Campo campo = jogo.getCampo(i);
            if (campo != null && !campo.getPecas().isEmpty()) {
                for (Peca peca : campo.getPecas()) {
                    if (peca.getJogador() == jogo.getJogador2()) {
                        player2Won = false;
                        break;
                    }
                }
                if (!player2Won) break;
            }
        }

        // Check captured pieces for player 2
        if (player2Won && jogo.getJogador2() != null && !jogo.getJogador2().getPecasCapturadas().isEmpty()) {
            player2Won = false;
        }

        if (player1Won) {
            handleVictory(jogo.getJogador1().getNome());
            return true;
        } else if (player2Won) {
            handleVictory(jogo.getJogador2().getNome());
            return true;
        }

        return false;
    }

    private void handleVictory(String winnerName) {
        jogoTerminado = true;
        vencedor = winnerName;
        
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogo terminado! Vencedor: " + winnerName);
        
        // Show victory popup
        Platform.runLater(() -> {
            showVictoryPopup(winnerName);
            
            // Update UI to reflect victory state
            updateLabelTurno();
            
            // Add victory message to chat
            if (winnerName.equals(meuNome)) {
                addChatMessage("🏆 VITÓRIA! " + winnerName + " venceu o jogo! 🏆");
                addChatMessage("🎉 Parabéns! Venceste o jogo! 🎉");
            } else {
                addChatMessage("💔 DERROTA! " + winnerName + " venceu o jogo! 💔");
                addChatMessage("😔 " + winnerName + " venceu. Boa sorte na próxima! 🍀");
            }
        });
    }

    private void showVictoryPopup(String winnerName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        
        if (winnerName.equals(meuNome)) {
            alert.setTitle("🏆 Jogo Terminado 🏆");
            alert.setHeaderText("VITÓRIA!");
            alert.setContentText("Vencedor do Jogo: " + winnerName + "\n\n🎉 Parabéns! 🎉");
        } else {
            alert.setTitle("💔 Jogo Terminado 💔");
            alert.setHeaderText("DERROTA!");
            alert.setContentText("Vencedor do Jogo: " + winnerName + "\n\n🍀 Boa sorte na próxima! 🍀");
        }
        
        // Style the alert
        alert.getDialogPane().setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 20px;"
        );
        
        // Only show close button
        ButtonType fecharButton = new ButtonType("❌ Fechar");
        alert.getButtonTypes().setAll(fecharButton);
        
        alert.showAndWait();
    }

    private void restartGame() {
        // Remove this method since we no longer need restart functionality
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Funcionalidade de reiniciar removida.");
    }
}