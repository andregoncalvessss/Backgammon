package backgammon.modelo;

import java.util.ArrayList;
import java.util.List;

public class Campo {
    private final int id;
    private final List<Peca> pecas;

    public Campo(int id) {
        this.id = id;
        this.pecas = new ArrayList<>();
    }

    public int getId() { return id; }

    public List<Peca> getPecas() { return pecas; }

    public void adicionarPeca(Peca peca) {
        pecas.add(peca);
    }

    public void removerPeca(Peca peca) {
        pecas.remove(peca);
    }

    public boolean temPecas() {
        return !pecas.isEmpty();
    }

    public Peca getTopo() {
        if (pecas.isEmpty()) return null;
        return pecas.get(pecas.size() - 1);
    }

    // --- NOVO MÉTODO: Verifica se há pelo menos uma peça deste jogador neste campo ---
    public boolean temPecasDoJogador(Jogador jogador) {
        return pecas.stream().anyMatch(p -> p.getJogador() == jogador);
    }
}