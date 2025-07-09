package backgammon.ui;

import backgammon.modelo.Campo;
import backgammon.modelo.JogoBackgammon;
import backgammon.modelo.Peca;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;

public class JogoController {
    @FXML
    private VBox pecasCapturadasView;
    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView backgroundView;
    @FXML
    private Pane aspectWrapper;
    @FXML
    private StackPane tabuleiroContainer; // <<-- StackPane para sobrepor o bearing off
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

    private final GridPane tabuleiro = new GridPane();
    private JogoBackgammon jogo;
    private final Map<Integer, StackPane> campoPecasMap = new HashMap<>();
    private final Map<Integer, Polygon> campoTrianguloMap = new HashMap<>();
    private Integer campoSelecionado = null;

    // --- BEARING OFF OUTLINE ---
    private Rectangle bearingOffOutline;

    public void setJogo(JogoBackgammon jogo) {
        this.jogo = jogo;
        resetarEstadoJogo();
    }

    private void resetarEstadoJogo() {
        campoSelecionado = null;
        limparDestaques();
        atualizarVisualizacaoPecas();
        atualizarLabelTurno();
    }

    @FXML
    public void initialize() {
        configurarInterface();
        criarTabuleiro();
        criarBearingOffOutline();
        configurarListeners();
    }

    private void configurarInterface() {
        Image bgVerde = new Image(getClass().getResource("/background1.png").toExternalForm());
        backgroundView.setImage(bgVerde);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundView.fitHeightProperty().bind(rootPane.heightProperty());
        StackPane centralizador = new StackPane(tabuleiro);
        tabuleiroContainer.getChildren().add(centralizador);
        configurarTamanhoTabuleiro();
    }

    private void configurarTamanhoTabuleiro() {
        tabuleiro.maxWidthProperty().bind(Bindings.createDoubleBinding(() ->
                        Math.min(tabuleiroContainer.getWidth(), tabuleiroContainer.getHeight() * 16.0 / 9.0),
                tabuleiroContainer.widthProperty(), tabuleiroContainer.heightProperty()));

        tabuleiro.maxHeightProperty().bind(tabuleiro.maxWidthProperty().multiply(9.0 / 16.0));
        tabuleiro.prefWidthProperty().bind(tabuleiro.maxWidthProperty());
        tabuleiro.prefHeightProperty().bind(tabuleiro.maxHeightProperty());
        tabuleiro.setHgap(6);
    }

    private void criarTabuleiro() {
        configurarColunasELinhas();
        configurarFundoTabuleiro();
        criarCampos();
    }

    private void configurarColunasELinhas() {
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

    private void configurarFundoTabuleiro() {
        Image fundo = new Image(getClass().getResource("/tabuleiro.png").toExternalForm());
        BackgroundImage bgTabuleiro = new BackgroundImage(
                fundo, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, false));
        tabuleiro.setBackground(new Background(bgTabuleiro));
    }

    private void criarCampos() {
        for (int col = 1; col < 15; col++) {
            if (col == 7 || col == 8) continue;
            StackPane cima = criarCampoContainer(col, true);
            StackPane baixo = criarCampoContainer(col, false);
            tabuleiro.add(cima, col, 0);
            tabuleiro.add(baixo, col, 1);
            configurarCampo(col, cima, true);
            configurarCampo(col, baixo, false);
        }
    }

    private StackPane criarCampoContainer(int coluna, boolean cima) {
        StackPane container = new StackPane();
        container.setPickOnBounds(false);

        Polygon triangulo = new Polygon();
        triangulo.setPickOnBounds(true);
        Pane trianguloPane = new Pane(triangulo);
        trianguloPane.setPickOnBounds(false);

        StackPane pecasContainer = new StackPane();
        pecasContainer.setPickOnBounds(false);
        pecasContainer.setAlignment(cima ? Pos.TOP_CENTER : Pos.BOTTOM_CENTER);

        // Apenas adicionamos o triangulo e as peças (sem label)
        container.getChildren().addAll(trianguloPane, pecasContainer);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        if (coluna <= 6) container.setPadding(new Insets(0, -40, 20, 40));
        else if (coluna >= 9) container.setPadding(new Insets(0, 40, 20, -40));

        container.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            atualizarFormaTriangulo(triangulo, newB, cima);
            triangulo.setFill(coluna % 2 == 0 ? Color.SADDLEBROWN : Color.BURLYWOOD);
            triangulo.setStroke(Color.BLACK);
            triangulo.setStrokeWidth(1);
        });

        return container;
    }

    private void atualizarFormaTriangulo(Polygon triangulo, Bounds bounds, boolean cima) {
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

    private void configurarCampo(int col, StackPane container, boolean cima) {
        int campoId = mapearColunaParaCampo(col, cima);
        if (campoId >= 1 && campoId <= 24) {
            campoPecasMap.put(campoId, container);
            Polygon triangulo = (Polygon) ((Pane) container.getChildren().get(0)).getChildren().get(0);
            campoTrianguloMap.put(campoId, triangulo);
            triangulo.setOnMouseClicked(e -> selecionarCampo(campoId));
        }
    }

    private int mapearColunaParaCampo(int col, boolean cima) {
        if (col == 7 || col == 8) return 0;
        if (!cima) return col <= 6 ? 7 + (6 - col) : 15 - col;
        return col <= 6 ? 13 + (col - 1) : 19 + (col - 9);
    }

    private void configurarListeners() {
        tabuleiroContainer.widthProperty().addListener((obs, o, n) -> {
            tabuleiro.requestLayout();
            atualizarVisualizacaoPecas();
        });
        tabuleiroContainer.heightProperty().addListener((obs, o, n) -> {
            tabuleiro.requestLayout();
            atualizarVisualizacaoPecas();
        });
    }

    // ------------------------- BEARING OFF OUTLINE ---------------------
    private void criarBearingOffOutline() {
        bearingOffOutline = new Rectangle();
        bearingOffOutline.setArcWidth(24);
        bearingOffOutline.setArcHeight(24);
        bearingOffOutline.setFill(Color.TRANSPARENT);
        bearingOffOutline.setStroke(Color.LIMEGREEN);
        bearingOffOutline.setStrokeWidth(5);
        bearingOffOutline.setVisible(false);

        tabuleiroContainer.getChildren().add(bearingOffOutline); // No topo do StackPane
    }

    // ---------------------------------------------------------------

    public void atualizarVisualizacaoPecas() {
        if (jogo == null || tabuleiroContainer.getHeight() <= 0) return;

        double raioBase = Math.max(16, tabuleiroContainer.getHeight() * 0.032);
        double raioSelecionado = raioBase * 1.25;
        double spacing = Math.max(18, tabuleiroContainer.getHeight() * 0.04);

        double margemTopo = tabuleiroContainer.getHeight() * 0.04;
        double margemBase = tabuleiroContainer.getHeight() * 0.04;

        // Limpa outlines antigos de bearing off
        campoPecasMap.values().forEach(container -> {
            if (container.getChildren().size() > 2) {
                container.getChildren().removeIf(node -> node instanceof Circle && ((Circle) node).getStroke() == Color.LIMEGREEN && ((Circle) node).getFill() == Color.TRANSPARENT);
            }
        });

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
                boolean jogavel = !temCapturada && jogo.getCamposComPecasMoveis().contains(campoId) && topo &&
                        peca.getJogador() == jogo.getJogadorAtual();

                Circle c = new Circle(selecionada ? raioSelecionado : raioBase);
                c.setFill(peca.getJogador() == jogo.getJogadorServidor() ? Color.WHITE : Color.RED);

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
                c.setOnMouseClicked(e -> selecionarCampo(campoId));

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
                    triangulo.setOnMouseClicked(e -> moverParaDestino(campoId));
                } else {
                    triangulo.setStroke(Color.BLACK);
                    triangulo.setStrokeWidth(1);
                    triangulo.setOnMouseClicked(null);
                }
            }
        }

        // ---- OUTLINE DE BEARING OFF (LIVRAR PEÇA) ----
        boolean showBearingOff = false;
        double rectX = 0, rectY = 0, rectWidth = 0, rectHeight = 0;

        if (campoSelecionado != null) {
            List<Integer> destinos = jogo.getDestinosValidos(campoSelecionado);

            // Brancas: bearing off em cima, lado direito do tabuleiro (campos 19-24)
            if (jogo.getJogadorAtual() == jogo.getJogadorServidor() && destinos.contains(25)) {
                showBearingOff = true;
                rectWidth = tabuleiroContainer.getWidth() * 0.067;
                rectHeight = tabuleiroContainer.getHeight() * 0.37;
                rectX = tabuleiroContainer.getWidth() - rectWidth - tabuleiroContainer.getWidth() * 0.036;
                rectY = tabuleiroContainer.getHeight() * 0.044;
                bearingOffOutline.setOnMouseClicked(e -> moverParaDestino(25));
            }
            // Vermelhas: bearing off em baixo, lado direito do tabuleiro (campos 1-6)
            if (jogo.getJogadorAtual() == jogo.getJogadorCliente() && destinos.contains(0)) {
                showBearingOff = true;
                rectWidth = tabuleiroContainer.getWidth() * 0.067;
                rectHeight = tabuleiroContainer.getHeight() * 0.37;
                rectX = tabuleiroContainer.getWidth() - rectWidth - tabuleiroContainer.getWidth() * 0.036;
                rectY = tabuleiroContainer.getHeight() - rectHeight - tabuleiroContainer.getHeight() * 0.044;
                bearingOffOutline.setOnMouseClicked(e -> moverParaDestino(0));
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

        // Peças capturadas
        pecasCapturadasView.getChildren().clear();
        List<Peca> capturadas = jogo.getJogadorAtual().getPecasCapturadas();
        if (!capturadas.isEmpty()) {
            Circle c = new Circle(raioSelecionado);
            c.setFill(capturadas.get(0).getJogador() == jogo.getJogadorServidor() ? Color.WHITE : Color.RED);
            c.setStroke(Color.LIMEGREEN);
            c.setStrokeWidth(3);
            c.setOnMouseClicked(e -> {
                campoSelecionado = -1;
                atualizarVisualizacaoPecas();
            });
            pecasCapturadasView.getChildren().add(c);
        }
    }

    private void selecionarCampo(int campoId) {
        Campo campo = jogo.getCampo(campoId);
        if (campo == null) return;

        if (campo.getPecas().isEmpty()) {
            tentarMoverPeca(campoId);
        } else {
            selecionarPecaParaMover(campoId, campo);
        }
    }

    private void tentarMoverPeca(int campoId) {
        boolean moveu;
        if (campoSelecionado != null && campoSelecionado == -1) {
            moveu = jogo.moverPecaCapturada(campoId);
        } else {
            moveu = jogo.moverPeca(campoSelecionado, campoId);
        }
        if (moveu) {
            if (jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()) {
                alternarJogador();
            } else {
                campoSelecionado = null;
                limparDestaques();
            }
        }
        atualizarVisualizacaoPecas();
    }

    private void selecionarPecaParaMover(int campoId, Campo campo) {
        if (campo.getTopo().getJogador() == jogo.getJogadorAtual() && jogo.getCamposComPecasMoveis().contains(campoId)) {
            if (!Objects.equals(campoSelecionado, campoId)) {
                campoSelecionado = campoId;
            } else {
                campoSelecionado = null;
                limparDestaques();
            }
            atualizarVisualizacaoPecas();
        }
    }

    private void moverParaDestino(int destinoId) {
        boolean moveu;
        if (campoSelecionado != null && campoSelecionado == -1) {
            moveu = jogo.moverPecaCapturada(destinoId);
        } else {
            moveu = jogo.moverPeca(campoSelecionado, destinoId);
        }
        if (moveu) {
            // ADICIONA ESTA VERIFICAÇÃO:
            if (jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()
                    || jogo.getCamposComPecasMoveis().isEmpty()) {
                alternarJogador();
            } else {
                campoSelecionado = null;
                limparDestaques();
            }
        }
        atualizarVisualizacaoPecas();
    }

    private void limparDestaques() {
        campoTrianguloMap.values().forEach(t -> {
            t.setStroke(Color.BLACK);
            t.setStrokeWidth(1);
            t.setOnMouseClicked(null);
        });
    }

    @FXML
    private void lancarDados() {
        if (jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()) {
            botaoLancarDados.setDisable(true);
            jogo.lancarDados();
            animarDados();
        }
    }

    private void animarDados() {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(100), e -> {
            Random r = new Random();
            dado1View.setImage(obterImagemDado(r.nextInt(6) + 1));
            dado2View.setImage(obterImagemDado(r.nextInt(6) + 1));
        }));
        timeline.setCycleCount(10);
        timeline.setOnFinished(e -> {
            atualizarDadosVisiveis();
            atualizarVisualizacaoPecas();

            // ⚠️ Verifica se o jogador atual tem jogadas possíveis
            if (jogo.getCamposComPecasMoveis().isEmpty()) {
                System.out.println("Jogador " + jogo.getJogadorAtual().getNome() + " não tem jogadas possíveis. Passa o turno.");
                jogo.getJogadorAtual().getDadosDisponiveis().clear(); // limpa os dados
                alternarJogador();
            }
        });
        timeline.play();
    }

    private Image obterImagemDado(int valor) {
        return new Image(getClass().getResource("/dados/dado" + valor + ".png").toExternalForm());
    }

    private void atualizarDadosVisiveis() {
        List<Integer> dados = jogo.getJogadorAtual().getDadosDisponiveis();

        dado1View.setImage(obterImagemDado(dados.size() > 0 ? dados.get(0) : 0));
        dado2View.setImage(obterImagemDado(dados.size() > 1 ? dados.get(1) : 0));

        if (dado3View != null) {
            if (dados.size() > 2) {
                dado3View.setVisible(true);
                dado3View.setImage(obterImagemDado(dados.get(2)));
            } else {
                dado3View.setVisible(false);
                dado3View.setImage(null);
            }
        }

        if (dado4View != null) {
            if (dados.size() > 3) {
                dado4View.setVisible(true);
                dado4View.setImage(obterImagemDado(dados.get(3)));
            } else {
                dado4View.setVisible(false);
                dado4View.setImage(null);
            }
        }
    }

    private void atualizarLabelTurno() {
        if (jogo != null) {
            if (jogo.getJogadorAtual() == jogo.getJogadorServidor()) {
                labelTurno.setText("SUA VEZ (Brancas)");
                labelTurno.setStyle("-fx-font-size: 30px; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 2, 0.5, 0, 0);");
            } else {
                labelTurno.setText("Vez do Adversário (Vermelhas)");
                labelTurno.setStyle("-fx-font-size: 30px; -fx-text-fill: red; -fx-font-weight: bold;");
            }
            boolean dadosNaoLancados = jogo.getJogadorAtual().getDadosDisponiveis().isEmpty();
            botaoLancarDados.setDisable(!dadosNaoLancados);
        }
    }

    private boolean isMinhaVez() {
        return jogo != null && jogo.getJogadorAtual() == jogo.getJogadorServidor();
    }

    private void alternarJogador() {
        if (jogo != null) {
            jogo.alternarJogador();
            campoSelecionado = null;
            limparDestaques();
            atualizarLabelTurno();
            atualizarVisualizacaoPecas();
            botaoLancarDados.setDisable(false);
        }
    }
}