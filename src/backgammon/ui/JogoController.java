package backgammon.ui;

import backgammon.modelo.JogoBackgammon;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.Random;

public class JogoController {

    @FXML private Pane aspectWrapper;
    @FXML private Pane tabuleiroContainer;
    @FXML private Button botaoLancarDados;
    @FXML private ImageView dado1View;
    @FXML private ImageView dado2View;

    private final GridPane tabuleiro = new GridPane();
    private JogoBackgammon jogo;

    public void setJogo(JogoBackgammon jogo) {
        this.jogo = jogo;
    }

    @FXML
    public void initialize() {
        StackPane centralizador = new StackPane(tabuleiro);
        tabuleiroContainer.getChildren().add(centralizador);

        tabuleiro.maxWidthProperty().bind(Bindings.createDoubleBinding(() ->
                        Math.min(tabuleiroContainer.getWidth(), tabuleiroContainer.getHeight() * 16.0 / 9.0),
                tabuleiroContainer.widthProperty(), tabuleiroContainer.heightProperty()));
        tabuleiro.maxHeightProperty().bind(tabuleiro.maxWidthProperty().multiply(9.0 / 16.0));
        tabuleiro.prefWidthProperty().bind(tabuleiro.maxWidthProperty());
        tabuleiro.prefHeightProperty().bind(tabuleiro.maxHeightProperty());

        tabuleiro.setHgap(6);

        for (int i = 0; i < 16; i++) {
            ColumnConstraints col = new ColumnConstraints();
            if (i == 0) {
                col.setPercentWidth(1.0);
            } else if (i == 15) {
                col.setPercentWidth(7.0);
            } else {
                col.setPercentWidth(92.0 / 14);
            }
            tabuleiro.getColumnConstraints().add(col);
        }

        for (int i = 0; i < 2; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(50);
            tabuleiro.getRowConstraints().add(row);
        }

        Image fundo = new Image(getClass().getResource("/backgammon/ui/tabuleiro.png").toExternalForm());
        BackgroundImage bg = new BackgroundImage(
                fundo, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, false)
        );
        tabuleiro.setBackground(new Background(bg));

        for (int col = 1; col < 15; col++) {
            if (col == 7 || col == 8) continue;
            tabuleiro.add(criarTriangulo(col, true), col, 0);
            tabuleiro.add(criarTriangulo(col, false), col, 1);
        }

        tabuleiroContainer.widthProperty().addListener((obs, oldVal, newVal) -> tabuleiro.requestLayout());
        tabuleiroContainer.heightProperty().addListener((obs, oldVal, newVal) -> tabuleiro.requestLayout());
    }

    private Region criarTriangulo(int coluna, boolean cima) {
        Polygon triangle = new Polygon();
        Pane trianglePane = new Pane(triangle);
        StackPane wrapper = new StackPane(trianglePane);
        wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        if (coluna >= 1 && coluna <= 6) {
            wrapper.setPadding(new Insets(0, -40, 0, 40));
        } else if (coluna >= 9 && coluna <= 14) {
            wrapper.setPadding(new Insets(0, 40, 0, -40));
        }

        wrapper.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                updateTriangleShape(trianglePane, newBounds.getWidth(), newBounds.getHeight(), coluna, cima)
        );

        return wrapper;
    }

    private void updateTriangleShape(Pane pane, double width, double height, int index, boolean cima) {
        double alturaTriangulo = height * 0.768;
        double margemVertical = height * 0.116;

        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(
                0.0, cima ? margemVertical : height - margemVertical,
                width, cima ? margemVertical : height - margemVertical,
                width / 2, cima ? margemVertical + alturaTriangulo : height - margemVertical - alturaTriangulo + 32
        );

        pane.getChildren().setAll(triangle);
        triangle.setFill(index % 2 == 0 ? Color.SADDLEBROWN : Color.BURLYWOOD);
        triangle.setStroke(Color.BLACK);
        triangle.setStrokeWidth(1);
    }

    @FXML
    private void lançarDados() {
        if (jogo != null) {
            jogo.lancarDados();
        }

        Timeline timeline = new Timeline();
        Random random = new Random();

        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(100), e -> {
            int temp1 = random.nextInt(6) + 1;
            int temp2 = random.nextInt(6) + 1;

            dado1View.setImage(new Image(getClass().getResource("/backgammon/ui/dados/dado" + temp1 + ".png").toExternalForm()));
            dado2View.setImage(new Image(getClass().getResource("/backgammon/ui/dados/dado" + temp2 + ".png").toExternalForm()));
        }));

        timeline.setCycleCount(10);
        timeline.setOnFinished(e -> {
            if (jogo != null) {
                int d1 = jogo.getJogadorAtual().getDado1();
                int d2 = jogo.getJogadorAtual().getDado2();
                dado1View.setImage(new Image(getClass().getResource("/backgammon/ui/dados/dado" + d1 + ".png").toExternalForm()));
                dado2View.setImage(new Image(getClass().getResource("/backgammon/ui/dados/dado" + d2 + ".png").toExternalForm()));
            }
        });

        timeline.play();
    }
}