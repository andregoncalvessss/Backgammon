package backgammon.modelo;

public class Peca {
    private final Jogador jogador;

    public Peca(Jogador jogador) {
        this.jogador = jogador;
    }

    public Jogador getJogador() {
        return jogador;
    }
}