package backgammon.modelo;

import java.util.ArrayList;
import java.util.List;

public class Tabuleiro {
    private List<Campo> campos;

    public Tabuleiro() {
        campos = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            campos.add(new Campo(i));
        }
    }

    public Campo getCampo(int index) {
        return campos.get(index);
    }

    public List<Campo> getCampos() {
        return campos;
    }
}
