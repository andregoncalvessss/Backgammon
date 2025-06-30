package backgammon.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class PecaUI extends StackPane {
    private Circle circle;
    private Text countText;

    public PecaUI(Color color, int count) {
        circle = new Circle(15);
        circle.setFill(color);
        circle.setStroke(Color.BLACK);

        countText = new Text(String.valueOf(count));
        countText.setFill(Color.BLACK);
        countText.setFont(Font.font(14));

        getChildren().addAll(circle, countText);
        setPrefSize(30, 30);
    }

    public void setCount(int count) {
        countText.setText(String.valueOf(count));
    }

    public void setColor(Color color) {
        circle.setFill(color);
    }
}