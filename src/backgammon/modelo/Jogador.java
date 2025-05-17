package backgammon.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Jogador {
    private List<Peca> pecas;
    private int dado1;
    private int dado2;

    public Jogador() {
        this.pecas = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            pecas.add(new Peca(this));
        }
    }

    public void rolarDados() {
        Random random = new Random();
        dado1 = random.nextInt(6) + 1;
        dado2 = random.nextInt(6) + 1;
    }

    public int getDado1() {
        return dado1;
    }

    public int getDado2() {
        return dado2;
    }

    public List<Peca> getPecas() {
        return pecas;
    }

    public Peca retiraPeca() {
        if (!pecas.isEmpty()) {
            return pecas.remove(0);
        }
        return null;
    }
}