package backgammon.modelo;

import java.util.Stack;

public class Campo {
    private int id;
    private Stack<Peca> pecas;

    public Campo(int id) {
        this.id = id;
        this.pecas = new Stack<>();
    }

    public void adicionarPeca(Peca peca) {
        pecas.push(peca);
    }

    public Peca removerPeca() {
        return pecas.isEmpty() ? null : pecas.pop();
    }

    public Peca topo() {
        return pecas.isEmpty() ? null : pecas.peek();
    }

    public int contarPecas() {
        return pecas.size();
    }

    public boolean estaVazio() {
        return pecas.isEmpty();
    }

    public int getId() {
        return id;
    }

    public Stack<Peca> getPecas() {
        return pecas;
    }
}
