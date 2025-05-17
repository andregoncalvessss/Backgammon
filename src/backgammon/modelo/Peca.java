package backgammon.modelo;

public class Peca {
    private Jogador dono;

    public Peca(Jogador dono) {
        this.dono = dono;
    }

    public Jogador getDono() {
        return dono;
    }
}
